package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jws.WebResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.workflow.provider.TransitionInstance;
import be.nabu.eai.module.workflow.provider.WorkflowInstance;
import be.nabu.eai.module.workflow.provider.WorkflowManager;
import be.nabu.eai.module.workflow.provider.WorkflowInstance.Level;
import be.nabu.eai.module.workflow.provider.WorkflowProperty;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.services.vm.SimpleVMServiceDefinition;
import be.nabu.libs.services.vm.VMContext;
import be.nabu.libs.types.api.ComplexContent;

// expose folders for each state with transition methods (input extends actual transition service input + workflow instance id)
// only expose if state is manual? as in, no transition picker
// need a way to revert a workflow to a previous state
// 		> only in case of error? or also if done
// need recovery protocol, basically if a system comes online, checks any workflows that have open transitions on their name, revert those
// 		> end up in previous state (even if automatic picker) and need to resolve manually? if stateless, can pick it up again
// retry picks closest stateless state that it passed and goes from there
// can also retry from a chosen stateless state _that the workflow passed through_
public class Workflow extends JAXBArtifact<WorkflowConfiguration> implements DefinedService, StartableArtifact {

	private boolean started;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Map<String, WorkflowTransition> transitions = new HashMap<String, WorkflowTransition>();
	private Map<String, WorkflowState> states = new HashMap<String, WorkflowState>();
	private Map<String, SimpleVMServiceDefinition> mappings = new HashMap<String, SimpleVMServiceDefinition>();
	
	public Workflow(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow.xml", WorkflowConfiguration.class);
	}

	@Override
	public ServiceInterface getServiceInterface() {
		// the input has a document containing one input document per initial transition possible
		// this starts a new workflow
		// also add a batch id to link multiple workflows started in a batch
		// context id is free to choose, if not filled in, it should be the id of the workflow
		// environment is autofilled
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceInstance newInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getReferences() {
		return null;
	}

	@Override
	public void start() throws IOException {
		if (!started) {
			if (getConfiguration().getProvider().getConfiguration().getGetRunningWorkflows() != null) {
				WorkflowManager workflowManager = getConfiguration().getProvider().getWorkflowManager();
				List<WorkflowInstance> runningWorkflows = workflowManager.getRunningWorkflows(getRepository().getName(), getId());
				for (WorkflowInstance workflow : runningWorkflows) {
					String transactionId = UUID.randomUUID().toString();
					try {
						// mark workflow as reverted
						workflow.setTransitionState(Level.REVERTED);
						workflowManager.updateWorkflow(transactionId, workflow);
						
						List<TransitionInstance> transitions = workflowManager.getTransitions(workflow.getId());
						Collections.sort(transitions);
						TransitionInstance last = transitions.get(transitions.size() - 1);
						if (last.getTransitionState().equals(Level.RUNNING)) {
							// revert the original transition
							last.setTransitionState(Level.REVERTED);
							last.setStopped(new Date());
							workflowManager.updateTransition(transactionId, last);
						}
						// find nearest successful transition that ended in a stateless state, we can go from there
						TransitionInstance statelessParent = null;
						for (TransitionInstance transition : transitions) {
							if (transition.getTransitionState().equals(Level.SUCCEEDED)) {
								WorkflowTransition definition = getTransitionById(transition.getDefinitionId());
								if (definition.getTargetState() != null && definition.getTargetState().isStateless()) {
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
								// and it's an automatic, run it
								if (transitionDefinition.getTargetState().getTransitionPicker() != null) {
									run(workflow, getAsTransitionPicker(transitionDefinition.getTargetState().getTransitionPicker()), SystemPrincipal.ROOT, nextSequence, statelessParent, null);
								}
							}
							else {
								// we first need the next transition attempted, so we know how the workflow was supposed to go
								TransitionInstance nextTransition = getTransitionBySequence(transitions, statelessParent.getSequence() + 1);
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
			started = true;
		}
	}
	
	public TransitionPicker getAsTransitionPicker(DefinedService service) {
		return service == null ? null : POJOUtils.newProxy(TransitionPicker.class, getRepository(), SystemPrincipal.ROOT, service);
	}
	
	public void run(WorkflowInstance workflow, TransitionPicker picker, Principal principal, int sequence, TransitionInstance previousTransition, ComplexContent previousTransitionOutput) {
		
	}
	
	public void run(WorkflowInstance workflow, WorkflowTransition transition, Principal principal, int sequence, TransitionInstance previousTransition, ComplexContent previousTransitionOutput) throws IOException {
		ComplexContent input, output;
		
		// we create the transition entry
		TransitionInstance newInstance = new TransitionInstance();
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
			WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
		}
		catch (Exception e) {
			WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
		}
		finally {
			WorkflowProvider.executionContext.set(null);
		}
		
		try {
			// we execute the mapping service for this entry if any
			SimpleVMServiceDefinition mapping = getMappings().get(transition.getId());
			if (mapping != null) {
				List<WorkflowProperty> workflowProperties = getConfiguration().getProvider().getWorkflowManager().getWorkflowProperties(workflow.getId());
				ComplexContent mapInput = mapping.getServiceInterface().getInputDefinition().newInstance();
				mapInput.set("workflow", workflow);
				mapInput.set("properties", workflowProperties);
				// we map in the output of the previous transitioner
				if (previousTransitionOutput != null) {
					WorkflowTransition transitionById = getTransitionById(previousTransition.getDefinitionId());
					if (transitionById != null) {
						mapInput.set(EAIRepositoryUtils.stringToField(transitionById.getName()), previousTransitionOutput);
					}
				}
				ServiceRuntime serviceRuntime = new ServiceRuntime(transition.getService(), getRepository().newExecutionContext(principal));
				input = serviceRuntime.run(mapInput);
			}
			else {
				input = transition.getService().getServiceInterface().getInputDefinition().newInstance();
			}
			// we execute the actual transition service
			ServiceRuntime runtime = new ServiceRuntime(transition.getService(), getRepository().newExecutionContext(principal));
			output = runtime.run(input);

			List<WorkflowProperty> propertiesToUpdate = new ArrayList<WorkflowProperty>();
			List<WorkflowProperty> propertiesToCreate = new ArrayList<WorkflowProperty>();
			if (transition.getFieldsToStore() != null) {
				
				for (String fieldToStore : transition.getFieldsToStore()) {
					// TODO: perform stores where necessary
					// merge intelligently with properties
				}
			}
			
			transactionId = UUID.randomUUID().toString();
			try {
				newInstance.setTransitionState(Level.SUCCEEDED);
				newInstance.setStopped(new Date());
				getConfiguration().getProvider().getWorkflowManager().updateTransition(transactionId, newInstance);
				// if there are no further paths, update workflow as well
				if (transition.getTargetState().getTransitions() == null || transition.getTargetState().getTransitions().isEmpty()) {
					workflow.setTransitionState(Level.SUCCEEDED);
					getConfiguration().getProvider().getWorkflowManager().updateWorkflow(transactionId, workflow);
				}
				// TODO: create/update the workflow properties!
				
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
		if (transition.getTargetState().getTransitions() != null && !transition.getTargetState().getTransitions().isEmpty() && transition.getTargetState().getTransitionPicker() != null) {
			run(workflow, getAsTransitionPicker(transition.getTargetState().getTransitionPicker()), principal, sequence + 1, newInstance, output);
		}
	}
	
	public static TransitionInstance getTransitionBySequence(List<TransitionInstance> instances, int sequence) {
		for (TransitionInstance instance : instances) {
			if (instance.getSequence() == sequence) {
				return instance;
			}
		}
		return null;
	}
	
	public WorkflowTransition getTransitionById(String id) {
		if (!transitions.containsKey(id)) {
			try {
				WorkflowState fictiveState = new WorkflowState();
				fictiveState.setTransitions(getConfiguration().getInitialTransitions());
				transitions.put(id, getTransitionById(id, fictiveState));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return transitions.get(id);
	}
	
	public WorkflowState getStateById(String id) {
		if (!states.containsKey(id)) {
			try {
				WorkflowState fictiveState = new WorkflowState();
				fictiveState.setTransitions(getConfiguration().getInitialTransitions());
				states.put(id, getStateById(id, fictiveState));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return states.get(id);
	}
	
	private WorkflowTransition getTransitionById(String id, WorkflowState state) {
		if (state.getTransitions() != null) {
			for (WorkflowTransition transition : state.getTransitions()) {
				if (id.equals(transition.getId())) {
					return transition;
				}
				else if (transition.getTargetState() != null) {
					WorkflowTransition potential = getTransitionById(id, transition.getTargetState());
					if (potential != null) {
						return potential;
					}
				}
			}
		}
		return null;
	}
	
	private WorkflowState getStateById(String id, WorkflowState state) {
		if (state.getTransitions() != null) {
			for (WorkflowTransition transition : state.getTransitions()) {
				if (transition.getTargetState() != null) {
					if (id.equals(transition.getTargetState().getId())) {
						return transition.getTargetState();
					}
					else {
						WorkflowState potential = getStateById(id, transition.getTargetState());
						if (potential != null) {
							return potential;
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	public Map<String, SimpleVMServiceDefinition> getMappings() {
		return mappings;
	}
	
}
