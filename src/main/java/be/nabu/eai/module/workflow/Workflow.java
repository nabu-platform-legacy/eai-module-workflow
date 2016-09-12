package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.workflow.provider.WorkflowInstance;
import be.nabu.eai.module.workflow.provider.WorkflowInstance.Level;
import be.nabu.eai.module.workflow.provider.WorkflowManager;
import be.nabu.eai.module.workflow.provider.WorkflowProperty;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.module.workflow.provider.WorkflowTransitionInstance;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.structure.DefinedStructure;

// expose folders for each state with transition methods (input extends actual transition service input + workflow instance id)
// only expose if state is manual? as in, no transition picker
// need a way to revert a workflow to a previous state
// 		> only in case of error? or also if done
// need recovery protocol, basically if a system comes online, checks any workflows that have open transitions on their name, revert those
// 		> end up in previous state (even if automatic picker) and need to resolve manually? if stateless, can pick it up again
// retry picks closest stateless state that it passed and goes from there
// can also retry from a chosen stateless state _that the workflow passed through_

// TODO: add security checks for transitions
public class Workflow extends JAXBArtifact<WorkflowConfiguration> implements StartableArtifact {

	private boolean started;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Map<String, TypeOperation> analyzedOperations = new HashMap<String, TypeOperation>();
	private Map<String, WorkflowTransition> transitions = new HashMap<String, WorkflowTransition>();
	private Map<String, WorkflowState> states = new HashMap<String, WorkflowState>();
	// one mapping per transition
	private Map<String, VMService> mappings = new HashMap<String, VMService>();
	
	private Map<String, DefinedStructure> structures = new HashMap<String, DefinedStructure>();

	public Workflow(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow.xml", WorkflowConfiguration.class);
		DefinedStructure global = new DefinedStructure();
		global.setName("global");
		structures.put("global", global);
	}

	// retry any ongoing flows from this server
	@Override
	public void start() throws IOException {
		if (!started) {
			started = true;
			if (getConfiguration().getProvider() != null && getConfiguration().getProvider().getConfiguration().getGetRunningWorkflows() != null) {
				WorkflowManager workflowManager = getConfiguration().getProvider().getWorkflowManager();
				List<WorkflowInstance> runningWorkflows = workflowManager.getRunningWorkflows(getRepository().getName(), getId());
				for (WorkflowInstance workflow : runningWorkflows) {
					String transactionId = UUID.randomUUID().toString();
					try {
						// mark workflow as reverted
						workflow.setTransitionState(Level.REVERTED);
						workflowManager.updateWorkflow(transactionId, workflow);
						
						List<WorkflowTransitionInstance> transitions = workflowManager.getTransitions(workflow.getId());
						Collections.sort(transitions);
						WorkflowTransitionInstance last = transitions.get(transitions.size() - 1);
						if (last.getTransitionState().equals(Level.RUNNING)) {
							// revert the original transition
							last.setTransitionState(Level.REVERTED);
							last.setStopped(new Date());
							workflowManager.updateTransition(transactionId, last);
						}
						// find nearest successful transition that ended in a stateless state, we can go from there
						WorkflowTransitionInstance statelessParent = null;
						for (WorkflowTransitionInstance transition : transitions) {
							if (transition.getTransitionState().equals(Level.SUCCEEDED)) {
								WorkflowTransition definition = getTransitionById(transition.getDefinitionId());
								if (definition.getTargetStateId() != null && getStateById(definition.getTargetStateId()).isStateless()) {
									statelessParent = transition;
								}
							}
						}
						// if we have a stateless parent, build a new transition from there
						if (statelessParent != null) {
							int nextSequence = transitions.get(transitions.size() - 1).getSequence() + 1;
							// if it is the last transition
							if (transitions.indexOf(statelessParent) == transitions.size() - 1) {
								WorkflowTransition transitionDefinition = getTransitionById(statelessParent.getDefinitionId());
								run(workflow, getStateById(transitionDefinition.getTargetStateId()), SystemPrincipal.ROOT, nextSequence, statelessParent, null);
							}
							else {
								// we first need the next transition attempted, so we know how the workflow was supposed to go
								WorkflowTransitionInstance nextTransition = getTransitionBySequence(transitions, statelessParent.getSequence() + 1);
								WorkflowTransition transitionDefinition = getTransitionById(nextTransition.getDefinitionId());
								run(workflow, transitionDefinition, SystemPrincipal.ROOT, nextSequence, statelessParent, null);
							}
						}
						// commit the transaction
						WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
					}
					catch (Exception e) {
						if (WorkflowProvider.executionContext.get() != null) {
							WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
						}
					}
					finally {
						// unset context
						WorkflowProvider.executionContext.set(null);
					}
				}
			}
		}
	}
	
	public void run(WorkflowInstance workflow, WorkflowState state, Principal principal, int sequence, WorkflowTransitionInstance previousTransition, ComplexContent previousTransitionOutput) throws IOException {
		String transactionId = UUID.randomUUID().toString();
		List<WorkflowProperty> workflowProperties;
		try {
			workflowProperties = getConfiguration().getProvider().getWorkflowManager().getWorkflowProperties(workflow.getId());
			WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
		}
		catch (Exception e) {
			WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
			throw new RuntimeException(e);
		}
		finally {
			WorkflowProvider.executionContext.set(null);
		}
		WorkflowTransition pickTransition = null;
		if (pickTransition != null) {
			run(workflow, pickTransition, principal, sequence, previousTransition, previousTransitionOutput, workflowProperties);
		}
		else {
			transactionId = UUID.randomUUID().toString();
			try {
				workflow.setTransitionState(Level.STOPPED);
				getConfiguration().getProvider().getWorkflowManager().updateWorkflow(transactionId, workflow);
				WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
			}
			catch (Exception e) {
				WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
				throw new RuntimeException(e);
			}
			finally {
				WorkflowProvider.executionContext.set(null);
			}
		}
	}
	
	public void run(WorkflowInstance workflow, WorkflowTransition transition, Principal principal, int sequence, WorkflowTransitionInstance previousTransition, ComplexContent previousTransitionOutput) throws IOException {
		String transactionId = UUID.randomUUID().toString();
		List<WorkflowProperty> workflowProperties;
		try {
			workflowProperties = getConfiguration().getProvider().getWorkflowManager().getWorkflowProperties(workflow.getId());
			WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
		}
		catch (Exception e) {
			WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
			throw new RuntimeException(e);
		}
		finally {
			WorkflowProvider.executionContext.set(null);
		}
		run(workflow, transition, principal, sequence, previousTransition, previousTransitionOutput, workflowProperties);
		
	}
	public void run(WorkflowInstance workflow, WorkflowTransition transition, Principal principal, int sequence, WorkflowTransitionInstance previousTransition, ComplexContent previousTransitionOutput, List<WorkflowProperty> workflowProperties) throws IOException {	
		ComplexContent input, output;
		
		// we create the transition entry
		WorkflowTransitionInstance newInstance = new WorkflowTransitionInstance();
		if (principal != null) {
			newInstance.setActorId(principal.getName());
		}
		newInstance.setId(UUID.randomUUID().toString());
		newInstance.setDefinitionId(transition.getId());
		if (previousTransition != null) {
			newInstance.setParentId(previousTransition.getId());
		}
		newInstance.setSequence(sequence);
		newInstance.setStarted(new Date());
		newInstance.setSystemId(getRepository().getName());
		newInstance.setTransitionState(Level.RUNNING);
		newInstance.setWorkflowId(workflow.getId());
		
		String transactionId = UUID.randomUUID().toString();
		try {
			getConfiguration().getProvider().getWorkflowManager().createTransition(transactionId, newInstance);
			workflow.setTransitionState(Level.RUNNING);
			getConfiguration().getProvider().getWorkflowManager().updateWorkflow(transactionId, workflow);
			WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
		}
		catch (Exception e) {
			WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
			throw new RuntimeException(e);
		}
		finally {
			WorkflowProvider.executionContext.set(null);
		}
		WorkflowState targetState = getStateById(transition.getTargetStateId());
		boolean canContinue = targetState.getTransitions() != null && !targetState.getTransitions().isEmpty();
		try {
			// we execute the mapping service for this entry if any
//			VMService mapping = getMappings().get(transition.getId());
//			if (mapping != null) {
//				ComplexContent mapInput = mapping.getServiceInterface().getInputDefinition().newInstance();
//				mapInput.set("workflow", workflow);
//				mapInput.set("properties", workflowProperties);
//				// we map in the output of the previous transitioner
//				if (previousTransitionOutput != null) {
//					WorkflowTransition transitionById = getTransitionById(previousTransition.getDefinitionId());
//					if (transitionById != null) {
//						mapInput.set(EAIRepositoryUtils.stringToField(transitionById.getName()), previousTransitionOutput);
//					}
//				}
//				ServiceRuntime serviceRuntime = new ServiceRuntime(transition.getService(), getRepository().newExecutionContext(principal));
//				input = serviceRuntime.run(mapInput);
//			}
//			else {
//				input = transition.getService().getServiceInterface().getInputDefinition().newInstance();
//			}
//			// we execute the actual transition service
//			ServiceRuntime runtime = new ServiceRuntime(transition.getService(), getRepository().newExecutionContext(principal));
//			output = runtime.run(input);
//
//			List<WorkflowProperty> propertiesToUpdate = new ArrayList<WorkflowProperty>();
//			List<WorkflowProperty> propertiesToCreate = new ArrayList<WorkflowProperty>();
//			if (transition.getFieldsToStore() != null) {
//				for (String fieldToStore : transition.getFieldsToStore().keySet()) {
//					String query = transition.getFieldsToStore().get(fieldToStore);
//					if (!analyzedOperations.containsKey(query)) {
//						synchronized(analyzedOperations) {
//							if (!analyzedOperations.containsKey(query)) {
//								analyzedOperations.put(query, (TypeOperation) new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse(query)));
//							}
//						}
//					}
//					Object value = analyzedOperations.get(query).evaluate(output);
//					boolean found = false;
//					for (WorkflowProperty property : workflowProperties) {
//						if (property.getKey().equals(fieldToStore)) {
//							property.setValue(value == null || value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class));
//							property.setTransitionId(newInstance.getId());
//							propertiesToUpdate.add(property);
//							found = true;
//							break;
//						}
//					}
//					if (!found) {
//						WorkflowProperty property = new WorkflowProperty();
//						property.setId(UUID.randomUUID().toString());
//						property.setWorkflowId(workflow.getId());
//						property.setTransitionId(newInstance.getId());
//						property.setKey(fieldToStore);
//						property.setValue(value == null || value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class));
//						propertiesToCreate.add(property);
//					}
//					if (!propertiesToCreate.isEmpty()) {
//						getConfiguration().getProvider().getWorkflowManager().createWorkflowProperties(transactionId, propertiesToCreate);
//					}
//					if (!propertiesToUpdate.isEmpty()) {
//						getConfiguration().getProvider().getWorkflowManager().updateWorkflowProperties(transactionId, propertiesToUpdate);
//					}
//				}
//			}
//			
			transactionId = UUID.randomUUID().toString();
			try {
				newInstance.setTransitionState(Level.SUCCEEDED);
				newInstance.setStopped(new Date());
				getConfiguration().getProvider().getWorkflowManager().updateTransition(transactionId, newInstance);
				workflow.setStateId(targetState.getId());
				workflow.setTransitionState(canContinue ? Level.RUNNING : Level.WAITING);
				// if there are no further paths, set to succeeded
				if (targetState.getTransitions() == null || targetState.getTransitions().isEmpty()) {
					workflow.setTransitionState(Level.SUCCEEDED);
				}
				getConfiguration().getProvider().getWorkflowManager().updateWorkflow(transactionId, workflow);
				WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
			}
			catch (Exception e) {
				WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
				logger.error("Could not update transition to succeeded", e);
				throw new RuntimeException(e);
			}
			finally {
				WorkflowProvider.executionContext.set(null);
			}
		}
		catch (Exception e) {
			transactionId = UUID.randomUUID().toString();
			try {
				newInstance.setTransitionState(Level.FAILED);
				newInstance.setStopped(new Date());
				StringWriter writer = new StringWriter();
				PrintWriter printer = new PrintWriter(writer);
				e.printStackTrace(printer);
				printer.flush();
				newInstance.setErrorLog(writer.toString());
				if (e instanceof ServiceException) {
					newInstance.setErrorCode(((ServiceException) e).getCode());
				}
				getConfiguration().getProvider().getWorkflowManager().updateTransition(transactionId, newInstance);
				workflow.setTransitionState(Level.FAILED);
				getConfiguration().getProvider().getWorkflowManager().updateWorkflow(transactionId, workflow);
				WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
			}
			catch (Exception f) {
				WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
				logger.error("Could not update transition to failed", f);
				logger.error("Cause of failure", e);
			}
			finally {
				WorkflowProvider.executionContext.set(null);
			}
			throw new RuntimeException(e);
		}
		
		// continue execution
		if (canContinue) {
//			run(workflow, getAsTransitionPicker(targetState.getTransitionPicker()), principal, sequence + 1, newInstance, output);
		}
	}
	
	public static WorkflowTransitionInstance getTransitionBySequence(List<WorkflowTransitionInstance> instances, int sequence) {
		for (WorkflowTransitionInstance instance : instances) {
			if (instance.getSequence() == sequence) {
				return instance;
			}
		}
		return null;
	}

	public WorkflowState getStateById(String id) {
		if (!states.containsKey(id)) {
			try {
				for (WorkflowState state : getConfiguration().getStates()) {
					if (state.getId().equals(id)) {
						return state;
					}
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return states.get(id);
	}
	
	public WorkflowTransition getTransitionById(String id) {
		if (!transitions.containsKey(id)) {
			try {
				WorkflowTransition transition = null;
				for (WorkflowState state : getConfiguration().getStates()) {
					WorkflowTransition potential = getTransitionById(id, state);
					if (potential != null) {
						transition = potential;
						break;
					}
				}
				transitions.put(id, transition);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return transitions.get(id);
	}
	
	private WorkflowTransition getTransitionById(String id, WorkflowState state) {
		if (state.getTransitions() != null) {
			for (WorkflowTransition transition : state.getTransitions()) {
				if (id.equals(transition.getId())) {
					return transition;
				}
			}
		}
		return null;
	}
	
	@Override
	public boolean isStarted() {
		return started;
	}

	public Map<String, VMService> getMappings() {
		return mappings;
	}

	public Map<String, DefinedStructure> getStructures() {
		return structures;
	}
}
