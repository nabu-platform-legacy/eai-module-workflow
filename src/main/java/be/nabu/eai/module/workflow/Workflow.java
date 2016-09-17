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
import java.util.UUID;

import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.workflow.provider.WorkflowManager;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.evaluator.PathAnalyzer;
import be.nabu.libs.evaluator.QueryParser;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.evaluator.types.operations.TypesOperationProvider;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;

// expose folders for each state with transition methods (input extends actual transition service input + workflow instance id)
// only expose if state is manual? as in, no transition picker
// need a way to revert a workflow to a previous state
// 		> only in case of error? or also if done
// need recovery protocol, basically if a system comes online, checks any workflows that have open transitions on their name, revert those
// 		> end up in previous state (even if automatic picker) and need to resolve manually? if stateless, can pick it up again
// retry picks closest stateless state that it passed and goes from there
// can also retry from a chosen stateless state _that the workflow passed through_

// TODO: add security checks for transitions
// TODO: can add a service runtime tracker that intercepts steps (and descriptions etc?) and builds a log file for each transition
public class Workflow extends JAXBArtifact<WorkflowConfiguration> implements StartableArtifact {

	private boolean started;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Map<String, TypeOperation> analyzedOperations = new HashMap<String, TypeOperation>();
	private Map<String, WorkflowTransition> transitions = new HashMap<String, WorkflowTransition>();
	private Map<String, WorkflowState> states = new HashMap<String, WorkflowState>();
	// one mapping per transition
	private Map<String, VMService> mappings = new HashMap<String, VMService>();
	
	private Map<String, ComplexType> stateEvaluationStructures = new HashMap<String, ComplexType>();
	
	private Map<String, DefinedStructure> structures = new HashMap<String, DefinedStructure>();

	public Workflow(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow.xml", WorkflowConfiguration.class);
		DefinedStructure properties = new DefinedStructure();
		properties.setName("properties");
		structures.put("properties", properties);
	}

//	public void retry() {
//		// find nearest successful transition that ended in a stateless state, we can go from there
//		WorkflowTransitionInstance statelessParent = null;
//		for (WorkflowTransitionInstance transition : transitions) {
//			if (transition.getTransitionState().equals(Level.SUCCEEDED)) {
//				WorkflowTransition definition = getTransitionById(transition.getDefinitionId());
//				if (definition.getTargetStateId() != null && getStateById(definition.getTargetStateId()).isStateless()) {
//					statelessParent = transition;
//				}
//			}
//		}
//		// if we have a stateless parent, build a new transition from there
//		if (statelessParent != null) {
//			int nextSequence = transitions.get(transitions.size() - 1).getSequence() + 1;
//			// if it is the last transition
//			if (transitions.indexOf(statelessParent) == transitions.size() - 1) {
//				WorkflowTransition transitionDefinition = getTransitionById(statelessParent.getDefinitionId());
//				run(workflow, getStateById(transitionDefinition.getTargetStateId()), SystemPrincipal.ROOT, nextSequence, statelessParent, null);
//			}
//			else {
//				// we first need the next transition attempted, so we know how the workflow was supposed to go
//				WorkflowTransitionInstance nextTransition = getTransitionBySequence(transitions, statelessParent.getSequence() + 1);
//				WorkflowTransition transitionDefinition = getTransitionById(nextTransition.getDefinitionId());
//				run(workflow, transitionDefinition, SystemPrincipal.ROOT, nextSequence, statelessParent, null);
//			}
//		}
//	}
	
	public void recover() {
		if (getConfig().getProvider() != null && getConfig().getProvider().getConfig().getGetWorkflows() != null) {
			WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
			List<WorkflowInstance> runningWorkflows = workflowManager.getWorkflows(getConfig().getConnectionId(), getRepository().getName(), getId(), Level.RUNNING);
			if (runningWorkflows != null) {
				for (WorkflowInstance workflow : runningWorkflows) {
					try {
						// mark workflow as reverted
						workflow.setTransitionState(Level.REVERTED);
						
						List<WorkflowTransitionInstance> transitions = workflowManager.getTransitions(getConfig().getConnectionId(), workflow.getId());
						Collections.sort(transitions);

						// if the last transition was RUNNING, it has to be reverted
						WorkflowTransitionInstance last = transitions.get(transitions.size() - 1);
						if (last.getTransitionState().equals(Level.RUNNING)) {
							// revert the original transition
							last.setTransitionState(Level.REVERTED);
							last.setStopped(new Date());
						}
						
						runTransactionally(new TransactionableAction<Void>() {
							@Override
							public Void call(String transactionId) throws Exception {
								WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
								workflowManager.updateWorkflow(getConfig().getConnectionId(), transactionId, workflow);
								if (last.getTransitionState() == Level.REVERTED) {
									workflowManager.updateTransition(getConfig().getConnectionId(), transactionId, last);
								}
								return null;
							}
						});
					}
					catch (Exception e) {
						logger.error("Could not revert workflow " + workflow.getId(), e);
					}
				}
			}
		}
	}
	
	public boolean isStateless(String stateId) {
		DefinedStructure definedStructure = getStructures().get(stateId);
		return definedStructure == null || !TypeUtils.getAllChildren(definedStructure).iterator().hasNext();
	}
	
	// retry any ongoing flows from this server
	@Override
	public void start() throws IOException {
		if (!started) {
			started = true;
			recover();
		}
	}
	
	public static <T> T runTransactionally(TransactionableAction<T> callable) {
		String transactionId = UUID.randomUUID().toString();
		try {
			T result = callable.call(transactionId);
			WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
			return result;
		}
		catch (Exception e) {
			WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
			throw new RuntimeException(e);
		}
		finally {
			WorkflowProvider.executionContext.set(null);
		}
	}
	
	public static interface TransactionableAction<T> {
		public T call(String transactionId) throws Exception;
	}
	
	public ComplexContent propertiesToObject(List<WorkflowInstanceProperty> properties) {
		DefinedStructure definedStructure = getStructures().get("properties");
		ComplexContent content = null;
		if (definedStructure != null) {
			content = definedStructure.newInstance();
			for (WorkflowInstanceProperty property : properties) {
				if (definedStructure.get(property.getKey()) != null) {
					content.set(property.getKey(), property.getValue());
				}
				else {
					logger.warn("Could not map property '" + property.getKey() + "' to the properties definition of workflow: " + getId());
				}
			}
		}
		return content;
	}
	
	public ComplexType getStateEvaluationType(String stateId) {
		if (!stateEvaluationStructures.containsKey(stateId)) {
			synchronized(stateEvaluationStructures) {
				if (!stateEvaluationStructures.containsKey(stateId)) {
					Structure structure = new Structure();
					structure.add(new ComplexElementImpl("properties", getStructures().get("properties"), structure));
					structure.add(new ComplexElementImpl("state", getStructures().get(stateId), structure));
					stateEvaluationStructures.put(stateId, structure);
				}
			}
		}
		return stateEvaluationStructures.get(stateId);
	}
	
	public void run(WorkflowInstance workflow, List<WorkflowTransitionInstance> history, List<WorkflowInstanceProperty> properties, WorkflowTransition transition, Principal principal, ComplexContent input) throws ServiceException {
		VMService transitionService = getMappings().get(transition.getId());
		
		if (transitionService == null) {
			throw new ServiceException("WORKFLOW-2", "No transition service found for transition: " + transition.getName() + " (" + transition.getId() + ")");
		}

		Collections.sort(history);
		
		// we create the transition entry
		WorkflowTransitionInstance newInstance = new WorkflowTransitionInstance();
		newInstance.setId(UUID.randomUUID().toString());
		newInstance.setDefinitionId(transition.getId());

		if (principal != null) {
			newInstance.setActorId(principal.getName());
		}

		int sequence = 0;
		if (history.size() > 0) {
			sequence = history.get(history.size() - 1).getSequence() + 1;
			newInstance.setParentId(history.get(history.size() - 1).getId());
		}
	
		newInstance.setSequence(sequence);
		newInstance.setStarted(new Date());
		newInstance.setSystemId(getRepository().getName());
		newInstance.setTransitionState(Level.RUNNING);
		newInstance.setWorkflowId(workflow.getId());
		
		// persist the transition and optionally update the workflow instance
		runTransactionally(new TransactionableAction<Void>() {
			@Override
			public Void call(String transactionId) throws Exception {
				getConfiguration().getProvider().getWorkflowManager().createTransition(getConfig().getConnectionId(), transactionId, newInstance);
				if (workflow.getTransitionState() != Level.RUNNING) {
					workflow.setTransitionState(Level.RUNNING);
					getConfiguration().getProvider().getWorkflowManager().updateWorkflow(getConfig().getConnectionId(), transactionId, workflow);
				}
				return null;
			}
		});

		// now we run the transition service
		ComplexContent mapInput = transitionService.getServiceInterface().getInputDefinition().newInstance();
		mapInput.set("workflow", workflow);
		mapInput.set("properties", propertiesToObject(properties));
		
		if (input != null) {
			mapInput.set("state", input.get("state"));
			mapInput.set("transition", input.get("transition"));
		}
		
		WorkflowState targetState = getStateById(transition.getTargetStateId());
		boolean isFinalState = targetState.getTransitions() == null || targetState.getTransitions().isEmpty();
		ComplexContent output;
		try {
			ServiceRuntime serviceRuntime = new ServiceRuntime(transitionService, getRepository().newExecutionContext(principal));
			output = serviceRuntime.run(mapInput);
			
			List<WorkflowInstanceProperty> propertiesToUpdate = new ArrayList<WorkflowInstanceProperty>();
			List<WorkflowInstanceProperty> propertiesToCreate = new ArrayList<WorkflowInstanceProperty>();

			ComplexContent object = (ComplexContent) output.get("properties");
			if (object != null) {
				for (Element<?> element : TypeUtils.getAllChildren(object.getType())) {
					Object value = object.get(element.getName());
					if (value != null) {
						for (WorkflowInstanceProperty current : properties) {
							if (current.getKey().equals(element.getName())) {
								current.setValue(ConverterFactory.getInstance().getConverter().convert(value, String.class));
								current.setTransitionId(newInstance.getId());
								propertiesToUpdate.add(current);
							}
							else {
								WorkflowInstanceProperty property = new WorkflowInstanceProperty();
								property.setId(UUID.randomUUID().toString());
								property.setWorkflowId(workflow.getId());
								property.setTransitionId(newInstance.getId());
								property.setKey(element.getName());
								property.setValue(ConverterFactory.getInstance().getConverter().convert(value, String.class));
								properties.add(property);
								propertiesToCreate.add(property);
							}
						}
					}
				}
			}
			
			newInstance.setStopped(new Date());
			newInstance.setTransitionState(Level.SUCCEEDED);
			
			workflow.setStateId(targetState.getId());
			
			if (isFinalState) {
				workflow.setTransitionState(Level.SUCCEEDED);
				workflow.setStopped(new Date());
			}
			else {
				// we could "optimize" and leave it in "running" mode if we first calculate the next automatic transition (if any)
				// but this would mean we can only commit the transition, properties etc until after that calculation
				// any error here (e.g. a typo in a rule) would revert too much)
				workflow.setTransitionState(Level.STOPPED);
			}
			
			runTransactionally(new TransactionableAction<Void>() {
				@Override
				public Void call(String transactionId) throws Exception {
					WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
					workflowManager.updateTransition(getConfig().getConnectionId(), transactionId, newInstance);
					workflowManager.updateWorkflow(getConfig().getConnectionId(), transactionId, workflow);
					if (!propertiesToCreate.isEmpty()) {
						workflowManager.createWorkflowProperties(getConfig().getConnectionId(), transactionId, propertiesToCreate);
					}
					if (!propertiesToUpdate.isEmpty()) {
						workflowManager.updateWorkflowProperties(getConfig().getConnectionId(), transactionId, propertiesToUpdate);
					}
					return null;
				}
			});
		}
		catch (Exception e) {
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
			workflow.setTransitionState(Level.FAILED);

			runTransactionally(new TransactionableAction<Void>() {
				@Override
				public Void call(String transactionId) throws Exception {
					WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
					workflowManager.updateTransition(getConfig().getConnectionId(), transactionId, newInstance);
					workflowManager.updateWorkflow(getConfig().getConnectionId(), transactionId, workflow);
					return null;
				}
			});
			throw new ServiceException(e);
		}
		
		// continue execution
		if (!isFinalState) {
			// make sure the current transition is reflected in the history
			history.add(newInstance);
			ComplexContent content = getStateEvaluationType(targetState.getId()).newInstance();
			content.set("properties", propertiesToObject(properties));
			content.set("state", output.get("state"));
			List<WorkflowTransition> possibleTransitions = new ArrayList<WorkflowTransition>(targetState.getTransitions());
			Collections.sort(possibleTransitions);
			boolean foundNext = false;
			for (WorkflowTransition possibleTransition : possibleTransitions) {
				String query = possibleTransition.getQuery();
				if (query != null) {
					try {
						if (!analyzedOperations.containsKey(query)) {
							synchronized(analyzedOperations) {
								if (!analyzedOperations.containsKey(query)) {
									analyzedOperations.put(query, (TypeOperation) new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse(query)));
								}
							}
						}
						Boolean value = (Boolean) analyzedOperations.get(query).evaluate(content);
						if (value != null && value) {
							foundNext = true;
							run(workflow, history, properties, possibleTransition, principal, content);
							break;
						}
					}
					catch (Exception e) {
						logger.error("There is an invalid query '" + query + "' for workflow: " + getId(), e);
					}
				}
			}
			// update the workflow to WAITING
			if (!foundNext) {
				workflow.setTransitionState(Level.WAITING);
				runTransactionally(new TransactionableAction<Void>() {
					@Override
					public Void call(String transactionId) throws Exception {
						WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
						workflowManager.updateWorkflow(getConfig().getConnectionId(), transactionId, workflow);
						return null;
					}
				});
			}
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
