package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nabu.misc.workflow.types.WorkflowBatchInstance;
import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.services.jdbc.RepositoryDataSourceResolver;
import be.nabu.eai.module.services.vm.RepositoryExecutorProvider;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.application.api.RESTFragment;
import be.nabu.eai.module.web.application.api.RESTFragmentProvider;
import be.nabu.eai.module.workflow.api.WorkflowListener;
import be.nabu.eai.module.workflow.provider.WorkflowManager;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionService;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.Notification;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.PathAnalyzer;
import be.nabu.libs.evaluator.QueryParser;
import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.impl.VariableOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.evaluator.types.operations.TypesOperationProvider;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.services.vm.api.ExecutorProvider;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.TypeBaseUtils;
import be.nabu.libs.types.mask.MaskedContent;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.cep.api.EventSeverity;
import be.nabu.utils.cep.impl.CEPUtils;
import be.nabu.utils.cep.impl.ComplexEventImpl;

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
// if we find a workflow that is running in an old version, we need to "mount" that version
// so basically get the old definition, add it (like the current workflow) to the repository but in a subfolder like "v3"
// we can then access all the document types & transitions and run it (best effort)
// if there are dependencies missing or modified to work differently, this can not be helped (but the same is true for classic BPMN)
public class Workflow extends JAXBArtifact<WorkflowConfiguration> implements WebFragment, RESTFragmentProvider {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Map<String, TypeOperation> analyzedOperations = new HashMap<String, TypeOperation>();
	// one mapping per transition
	private Map<String, VMService> mappings = new HashMap<String, VMService>();
	
	private Map<String, ComplexType> stateEvaluationStructures = new HashMap<String, ComplexType>();
	
	private Map<String, DefinedStructure> structures = new HashMap<String, DefinedStructure>();
	
	public static WorkflowInstance resolveInstance(String connectionId, String workflowId) {
		WorkflowProvider defaultProvider = (WorkflowProvider) EAIResourceRepository.getInstance().resolve("nabu.misc.workflow.providers.basic.provider");
		return defaultProvider.getWorkflowManager().getWorkflow(connectionId, workflowId);
	}
	
	public static String deduceConnectionId(Workflow workflow) {
		String serviceContext = ServiceUtils.getServiceContext(ServiceRuntime.getRuntime(), false);
		if (serviceContext == null && workflow != null) {
			serviceContext = workflow.getId();
		}
		return new RepositoryDataSourceResolver().getDataSourceId(serviceContext);
	}
	
	public static Workflow resolveDefinition(String definitionId) {
		return (Workflow) EAIResourceRepository.getInstance().resolve(definitionId);
	}
	
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
			String connectionId = getConfig().getConnection() == null ? null : getConfig().getConnection().getId();
			List<WorkflowInstance> runningWorkflows = workflowManager.getWorkflows(connectionId, getId(), null, Level.RUNNING, null, null, null, null, null, null, null, null, null, null, null, null, null);
			if (runningWorkflows != null) {
				for (WorkflowInstance workflow : runningWorkflows) {
					try {
						List<WorkflowTransitionInstance> transitions = workflowManager.getTransitions(connectionId, workflow.getId());
						Collections.sort(transitions);
						
						// if the last transition was RUNNING, it has to be reverted
						WorkflowTransitionInstance last = transitions.get(transitions.size() - 1);
						if (last.getTransitionState().equals(Level.RUNNING)) {
							// if it is not running on this system, we are not interested in it
							if (!last.getSystemId().equals(getRepository().getName())) {
								continue;
							}
							// revert the original transition
							last.setTransitionState(Level.REVERTED);
							last.setStopped(new Date());
						}
						// if the last transition was not in state RUNNING, we don't care atm
						else {
							continue;
						}

						// mark workflow as reverted
						workflow.setTransitionState(Level.REVERTED);
						
						runTransactionally(new TransactionableAction<Void>() {
							@Override
							public Void call(String transactionId) throws Exception {
								WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
								workflowManager.updateWorkflow(connectionId, transactionId, workflow);
								if (last.getTransitionState() == Level.REVERTED) {
									workflowManager.updateTransition(connectionId, transactionId, last);
								}
								return null;
							}
						});
					}
					catch (Exception e) {
						logger.error("Could not revert workflow " + workflow.getId(), e);
						fire("revert", 0, workflow.getId(), "Could not revert running workflow", Notification.format(e), Severity.WARNING, null);
					}
				}
			}
			// revert batches if possible/necessary
			if (getConfig().getProvider().getConfig().getGetBatches() != null) {
				// if the batch is set to stopped, we figured it out was done but weren't able to move it to the next level yet (if it is our system id)
				// continue the shizzle
				List<WorkflowBatchInstance> batches = workflowManager.getBatches(connectionId, Level.WAITING, null, null);
				if (batches != null) {
					// if we were the one who set it to stopped
					for (WorkflowBatchInstance batch : batches) {
						if (batch.getSystemId().equals(getRepository().getName())) {
							try {
								runBatchConclusion(connectionId, SystemPrincipal.ROOT, workflowManager, batch);
//								batch.setState(Level.REVERTED);
//								runTransactionally(new TransactionableAction<Void>() {
//									@Override
//									public Void call(String transactionId) throws Exception {
//										workflowManager.updateBatch(connectionId, transactionId, batch);
//										return null;
//									}
//								});
							}
							catch (Exception e) {
								logger.error("Could not revert workflow batch " + batch.getId(), e);
								fire("batchRevert", 1, batch.getId(), "Could not revert running workflow batch", Notification.format(e), Severity.WARNING, null);
							}
						}
					}
				}
			}
		}
	}
	
	private void fire(String type, int code, String id, String message, String description, Severity severity, Token token) {
		try {
			Notification notification = new Notification();
			if (token != null) {
				notification.setAlias(token.getName());
				notification.setRealm(token.getRealm());
			}
			notification.setContext(Arrays.asList(id, getId()));
			notification.setCode("WORKFLOW-0");
			notification.setType("nabu.misc.workflow." + type);
			notification.setMessage(message);
			notification.setDescription(description);
			notification.setSeverity(severity);
			getRepository().getEventDispatcher().fire(notification, this);
		}
		catch (Exception e) {
			logger.error("Could not send notification", e);
		}
	}
	
	public boolean isStateless(String stateId) {
		DefinedStructure definedStructure = getStructures().get(stateId);
		return definedStructure == null || !TypeUtils.getAllChildren(definedStructure).iterator().hasNext();
	}
	
	public static <T> T runTransactionally(TransactionableAction<T> callable) {
		String transactionId = UUID.randomUUID().toString();
		try {
			T result = callable.call(transactionId);
			WorkflowProvider.commit(transactionId);
			return result;
		}
		// this "should" differentiate for service exceptions
		catch (Exception e) {
			WorkflowProvider.rollback(transactionId);
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
	
	public ComplexType getPropertyDefinition() {
		return getStructures().get("properties");
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
	
	private WorkflowTransitionInstance getTransitionInstance(List<WorkflowTransitionInstance> history, String id) {
		for (WorkflowTransitionInstance instance : history) {
			if (instance.getId().equals(id)) {
				return instance;
			}
		}
		return null;
	}
	
	// an autoretry finds the last transition that can be rerun without providing additional input and runs it
	public void autoRetry(String connectionId, WorkflowInstance workflow, Token token) throws ServiceException {
		List<WorkflowTransitionInstance> history = new ArrayList<WorkflowTransitionInstance>();
		
		List<WorkflowTransitionInstance> transitions = getConfig().getProvider().getWorkflowManager().getTransitions(connectionId, workflow.getId());
		if (transitions != null) {
			history.addAll(transitions);
		}
		
		if (history.isEmpty()) {
			throw new IllegalStateException("No transitions found to retry for: " + workflow.getId());
		}
		
		Collections.sort(history);
		// most recent to oldest
		Collections.reverse(history);
		
		for (WorkflowTransitionInstance instance : history) {
			// we want the service, the interface has all the information we need and if applicable, we need to run it
			// note that we can not auto retry initial transitions, create a new workflow for that
			WorkflowTransition transition = getTransitionById(instance.getDefinitionId());
			// only relevant if the transition still exists
			if (transition != null) {
				String serviceId = getId() + ".services.transition" + "." + EAIRepositoryUtils.stringToField(transition.getName());
				DefinedService transitionService = (DefinedService) getRepository().resolve(serviceId);
				if (transitionService != null) {
					ComplexType inputDefinition = transitionService.getServiceInterface().getInputDefinition();
					// if there are no input parameters for this service, we can run it
					if (inputDefinition.get("state") == null && inputDefinition.get("transition") == null) {
						ComplexContent input = inputDefinition.newInstance();
						input.set("connectionId", connectionId);
						input.set("workflowId", workflow.getId());
						input.set("force", true);
						ServiceRuntime runtime = new ServiceRuntime(transitionService, getRepository().newExecutionContext(token));
						runtime.run(input);
						break;
					}
					else {
						logger.info("[Retry] Skipping transition because of required input: " + serviceId);
					}
				}
				else {
					logger.warn("[Retry] Could not find transition service: " + serviceId);
				}
			}
		}
	}
	
	public long getVersion() {
		Node node = getRepository().getNode(getId());
		if (node == null) {
			throw new IllegalStateException("Could not find node");
		}
		return node.getVersion();
	}
	
	public void run(String connectionId, WorkflowInstance workflow, List<WorkflowTransitionInstance> history, List<WorkflowInstanceProperty> properties, WorkflowTransition transition, Token token, ComplexContent input) throws ServiceException {
		ComplexEventImpl event = null;
		if (getRepository().getComplexEventDispatcher() != null) {
			event = new ComplexEventImpl();
			event.setEventName("workflow-transition");
			event.setAction(transition.getName());
			event.setCorrelationId(workflow.getId());
			event.setArtifactId(getId());
			event.setCreated(new Date());
			event.setStarted(event.getCreated());
			event.setSeverity(EventSeverity.DEBUG);
			if (token != null) {
				event.setAlias(token.getName());
				event.setRealm(token.getRealm());
			}
		}
		
		try {
			// check if the current user is allowed to run it
			TokenValidator tokenValidator = getTokenValidator();
			if (tokenValidator != null && token != null && !tokenValidator.isValid(token)) {
				token = null;
			}
			if (transition.getRoles() != null && !transition.getRoles().isEmpty()) {
				RoleHandler roleHandler = getRoleHandler();
				if (roleHandler != null) {
					boolean allowed = false;
					for (String role : transition.getRoles()) {
						if (roleHandler.hasRole(token, role)) {
							allowed = true;
							break;
						}
					}
					if (!allowed) {
						throw new ServiceException("WORKFLOW-4", "The user does not have the correct role to run this transition");
					}
				}
			}
			
			WorkflowState sourceState = workflow.getStateId() != null ? getStateById(workflow.getStateId()) : null;
			
			PermissionHandler permissionHandler = getPermissionHandler();
			if (permissionHandler != null && !permissionHandler.hasPermission(token, workflow.getId(), transition.getName())) {
				throw new ServiceException("WORKFLOW-5", "The user does not have permission to run this transition");
			}
			
			VMService transitionService = getMappings().get(transition.getId());
			
			if (transitionService == null) {
				ServiceException serviceException = new ServiceException("WORKFLOW-2", "No transition service found");
				serviceException.setDescription("No transition service found for transition: " + transition.getName() + " (" + transition.getId() + ")");
				serviceException.setToken(token);
				throw serviceException;
			}
	
			WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
			
			Collections.sort(history);
			
			// we sort the properties based on their transition history
			// this allows us to set the value in the correct order to preserve overwrites
			properties.sort(new Comparator<WorkflowInstanceProperty>() {
				@Override
				public int compare(WorkflowInstanceProperty o1, WorkflowInstanceProperty o2) {
					WorkflowTransitionInstance transitionInstance1 = getTransitionInstance(history, o1.getTransitionId());
					WorkflowTransitionInstance transitionInstance2 = getTransitionInstance(history, o2.getTransitionId());
					return transitionInstance1.getSequence() - transitionInstance2.getSequence();
				}
			});
			
			// we create the transition entry
			WorkflowTransitionInstance newInstance = new WorkflowTransitionInstance();
			newInstance.setId(UUID.randomUUID().toString().replace("-", ""));
			newInstance.setDefinitionId(transition.getId());
			newInstance.setFromStateId(workflow.getStateId());
			
			// if the target state is the same as the source state, we are not updating the state, use the workflow state id
			// this is useful for state extensions where the target state is a to-be-extended state and not an actual state
			// it is currently unclear what to do if you target another to-be-extended state, this should probably not be allowed
			WorkflowState transitionFromState = getTransitionFromState(transition.getId());
			if (transitionFromState.getId().equals(transition.getTargetStateId())) {
				newInstance.setToStateId(workflow.getStateId());
			}
			else {
				newInstance.setToStateId(transition.getTargetStateId());
			}
	
			if (token != null) {
				newInstance.setActorId(token.getName());
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
			
			WorkflowBatchInstance batch;
			// if we have a batchId in the input, create a batch
			if (transitionService.getServiceInterface().getInputDefinition().get("batchId") != null) {
				batch = new WorkflowBatchInstance();
				batch.setStarted(new Date());
				batch.setCreated(new Date());
				batch.setId(UUID.randomUUID().toString().replace("-", ""));
				batch.setState(Level.RUNNING);
				batch.setSystemId(getRepository().getName());
				batch.setTransitionId(newInstance.getId());
				batch.setWorkflowId(workflow.getId());
				newInstance.setBatchId(batch.getId());
			}
			else {
				batch = null;
			}
			
			// persist the transition and optionally update the workflow instance
			runTransactionally(new TransactionableAction<Void>() {
				@Override
				public Void call(String transactionId) throws Exception {
					workflowManager.createTransition(connectionId, transactionId, newInstance);
					if (workflow.getTransitionState() != Level.RUNNING) {
						workflow.setTransitionState(Level.RUNNING);
						workflowManager.updateWorkflow(connectionId, transactionId, workflow);
					}
					if (batch != null) {
						workflowManager.createBatch(connectionId, transactionId, batch);
					}
					return null;
				}
			});
	
			// now we run the transition service
			ComplexContent mapInput = transitionService.getServiceInterface().getInputDefinition().newInstance();
			// old workflows don't have a connection id in their input
			if (mapInput.getType().get("connectionId") != null) {
				mapInput.set("connectionId", connectionId);
			}
			mapInput.set("workflow", workflow);
			mapInput.set("properties", propertiesToObject(properties));
			mapInput.set("history", history);
			
			if (input != null) {
				mapInput.set("state", input.get("state"));
				mapInput.set("transition", input.get("transition"));
			}
			
			if (batch != null) {
				mapInput.set("batchId", batch.getId());
			}
			
			WorkflowState targetState = getStateById(transition.getTargetStateId());
			boolean isFinalState = isFinalState(targetState);
			ComplexContent output;
			ServiceRuntime serviceRuntime = new ServiceRuntime(transitionService, getRepository().newExecutionContext(token));
			boolean contextSet = false;
			if (ServiceUtils.getServiceContext(serviceRuntime, false) == null) {
				ServiceUtils.setServiceContext(serviceRuntime, workflow.getDefinitionId());
				contextSet = true;
			}
			try {
				output = serviceRuntime.run(mapInput);
				
				List<WorkflowInstanceProperty> propertiesToUpdate = new ArrayList<WorkflowInstanceProperty>();
				List<WorkflowInstanceProperty> propertiesToCreate = new ArrayList<WorkflowInstanceProperty>();
	
				ComplexContent object = output == null ? null : (ComplexContent) output.get("properties");
				if (object != null) {
					Map<String, String> stringMap = TypeBaseUtils.toStringMap(object);
					// TODO: need a good way to "clean up" arrays:
					// first time you add an array of 3 items, next time an array of 2
					// the first two items will be cleanly overwritten but the third will persist
					for (String key : stringMap.keySet()) {
						String value = stringMap.get(key);
						if (value != null) {
							boolean found = false;
							
							// we no longer update properties based on key
							// instead we set the key/value for this given transition instance
							// that gives us a history of the property values over time in the workflow
//							for (WorkflowInstanceProperty current : properties) {
//								if (current.getKey().equals(key)) {
//									current.setValue(value);
//									current.setTransitionId(newInstance.getId());
//									propertiesToUpdate.add(current);
//									found = true;
//									break;
//								}
//							}
							
							if (!found) {
								WorkflowInstanceProperty property = new WorkflowInstanceProperty();
								property.setId(UUID.randomUUID().toString().replace("-", ""));
								property.setWorkflowId(workflow.getId());
								property.setTransitionId(newInstance.getId());
								property.setKey(key);
								property.setValue(value);
								properties.add(property);
								propertiesToCreate.add(property);
							}
						}
					}
				}
				String groupId = output == null ? null : (String) output.get("groupId");
				String contextId = output == null ? null : (String) output.get("contextId");
				String workflowType = output == null ? null : (String) output.get("workflowType");
				String correlationId = output == null ? null : (String) output.get("correlationId");
				
				if (groupId != null) {
					workflow.setGroupId(groupId);
				}
				if (contextId != null) {
					workflow.setContextId(contextId);
				}
				if (workflowType != null) {
					workflow.setWorkflowType(workflowType);
				}
				if (correlationId != null) {
					workflow.setCorrelationId(correlationId);
				}
				
				newInstance.setLog(output == null ? null : (String) output.get("log"));
				newInstance.setCode(output == null ? null : (String) output.get("code"));
				newInstance.setUri(output == null ? null : (URI) output.get("uri"));
				
				newInstance.setStopped(new Date());
				newInstance.setTransitionState(batch != null ? Level.WAITING : Level.SUCCEEDED);
				
				// use the possibly computed to state id from the transition
				workflow.setStateId(newInstance.getToStateId());
				
				if (isFinalState) {
					workflow.setTransitionState(batch == null ? Level.SUCCEEDED : Level.WAITING);
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
						workflowManager.updateTransition(connectionId, transactionId, newInstance);
						workflowManager.updateWorkflow(connectionId, transactionId, workflow);
						if (!propertiesToCreate.isEmpty()) {
							workflowManager.createWorkflowProperties(connectionId, transactionId, propertiesToCreate);
						}
						if (!propertiesToUpdate.isEmpty()) {
							workflowManager.updateWorkflowProperties(connectionId, transactionId, propertiesToUpdate);
						}
						// other transitions may still be running
//						if (batch != null) {
//							batch.setCreated(new Date());
//							batch.setState(Level.WAITING);
//							workflowManager.updateBatch(connectionId, transactionId, batch);
//						}
						return null;
					}
				});
				
				callListeners(connectionId, workflow, transition, sourceState, targetState, token);
				
				// check if the batch is already done, if so we can continue
				if (batch != null) {
					if (!continueBatch(batch)) {
						return;
					}
					else {
						runTransactionally(new TransactionableAction<Void>() {
							@Override
							public Void call(String transactionId) throws Exception {
								newInstance.setTransitionState(Level.SUCCEEDED);
								workflowManager.updateTransition(connectionId, transactionId, newInstance);
								batch.setState(Level.SUCCEEDED);
								workflowManager.updateBatch(connectionId, transactionId, batch);
								workflow.setTransitionState(Level.SUCCEEDED);
								workflowManager.updateWorkflow(connectionId, transactionId, workflow);
								return null;
							}
						});
					}
				}
			}
			catch (Exception e) {
				if (event != null) {
					CEPUtils.enrich(event, e);
				}
				newInstance.setTransitionState(Level.ERROR);
				newInstance.setStopped(new Date());
				StringWriter writer = new StringWriter();
				PrintWriter printer = new PrintWriter(writer);
				e.printStackTrace(printer);
				printer.flush();
				newInstance.setErrorLog(writer.toString());
				// try to find the actual structured cause
				Throwable current = e;
				while (current != null) {
					if (current instanceof ServiceException) {
						newInstance.setErrorCode(((ServiceException) current).getCode());
						if (event != null) {
							event.setCode(newInstance.getErrorCode());
							event.setReason(((ServiceException) current).getDescription());
						}
						break;
					}
					current = current.getCause();
				}
				workflow.setTransitionState(Level.ERROR);
				runTransactionally(new TransactionableAction<Void>() {
					@Override
					public Void call(String transactionId) throws Exception {
						workflowManager.updateTransition(connectionId, transactionId, newInstance);
						workflowManager.updateWorkflow(connectionId, transactionId, workflow);
						return null;
					}
				});
				throw e instanceof ServiceException ? (ServiceException) e : new ServiceException("WORKFLOW-7", "Unexpected exception occured while executing transition", e);
			}
			finally {
				if (contextSet) {
					ServiceUtils.setServiceContext(serviceRuntime, null);
				}
				if (event != null) {
					event.setStopped(new Date());
					getRepository().getComplexEventDispatcher().fire(event, this);
				}
			}
			
			// continue execution
			if (!isFinalState) {
				// make sure the current transition is reflected in the history
				history.add(newInstance);
				continueWorkflow(connectionId, workflow, history, properties, token, targetState, output);
			}
			// if this workflow was part of a batch and it's done, let's check that batch
			else if (workflow.getBatchId() != null) {
				continueBatchIfNecessary(connectionId, workflow, token, workflowManager);
			}
		}
		catch (Exception e) {
			// we could not properly finish the event itself
			if (event != null && event.getStopped() == null) {
				event.setStopped(new Date());
				CEPUtils.enrich(event, e);
				getRepository().getComplexEventDispatcher().fire(event, this);
			}
			fire("run", 2, workflow.getId(), "Failed while running transition '" + transition.getName() + "' (or automatic transitions after it)", Notification.format(e), Severity.ERROR, token);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			else if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			else {
				throw new ServiceException("WORKFLOW-8", "Unexpected exception occured while continuing workflow", e);
			}
		}
	}

	private void continueBatchIfNecessary(String connectionId, WorkflowInstance workflow, Token token, WorkflowManager workflowManager) {
		Level level = workflowManager.calculateBatchState(connectionId, workflow.getBatchId());
		if (level == Level.STOPPED) {
			WorkflowBatchInstance parentBatch = workflowManager.getBatch(connectionId, workflow.getBatchId());
			if (continueBatch(parentBatch)) {
				runBatchConclusion(connectionId, token, workflowManager, parentBatch);
			}
		}
	}

	private void runBatchConclusion(String connectionId, Token token, WorkflowManager workflowManager, WorkflowBatchInstance parentBatch) {
		parentBatch.setState(Level.SUCCEEDED);
		// need to get the state of the parent workflow, update the final transition and continue
		WorkflowInstance parentFlow = workflowManager.getWorkflow(connectionId, parentBatch.getWorkflowId());
		Workflow parentWorkflow = (Workflow) getRepository().resolve(parentFlow.getDefinitionId());
		if (parentWorkflow == null) {
			throw new IllegalArgumentException("Can not find definition of parent workflow: " + parentFlow.getDefinitionId());
		}
		List<WorkflowTransitionInstance> parentHistory = workflowManager.getTransitions(connectionId, parentFlow.getId());
		List<WorkflowInstanceProperty> parentProperties = workflowManager.getWorkflowProperties(connectionId, parentFlow.getId());
		Collections.sort(parentHistory);
		WorkflowState targetParentState = null;
		boolean hasTransitionInCorrectState = false;
		for (WorkflowTransitionInstance transitionInstance : parentHistory) {
			if (transitionInstance.getTransitionState() == Level.WAITING && parentBatch.getId().equals(transitionInstance.getBatchId())) {
				transitionInstance.setTransitionState(Level.SUCCEEDED);
				hasTransitionInCorrectState = true;
				Workflow parentFlowDefinition = (Workflow) getRepository().resolve(parentFlow.getDefinitionId());
				targetParentState = parentFlowDefinition.getStateById(transitionInstance.getToStateId());
				boolean isFinal = targetParentState.getTransitions() == null || targetParentState.getTransitions().isEmpty();
				runTransactionally(new TransactionableAction<Void>() {
					@Override
					public Void call(String transactionId) throws Exception {
						workflowManager.updateTransition(connectionId, transactionId, transitionInstance);
						workflowManager.updateBatch(connectionId, transactionId, parentBatch);
						if (isFinal) {
							parentFlow.setTransitionState(Level.SUCCEEDED);
							workflowManager.updateWorkflow(connectionId, transactionId, parentFlow);
						}
						return null;
					}
				});
				break;
			}
		}
		if (hasTransitionInCorrectState) {
			parentWorkflow.continueWorkflow(connectionId, parentFlow, parentHistory, parentProperties == null ? new ArrayList<WorkflowInstanceProperty>() : parentProperties, token, targetParentState, null);
		}
	}

	public void callListeners(String connectionId, WorkflowInstance workflow, WorkflowTransition transition, WorkflowState fromState, WorkflowState toState, Token token) {
		if (getConfig().getTransitionListeners() != null) {
			for (DefinedService service : getConfig().getTransitionListeners()) {
				WorkflowListener listener = POJOUtils.newProxy(WorkflowListener.class, service, getRepository(), SystemPrincipal.ROOT);
				try {
					listener.transition(connectionId, workflow, transition, fromState, toState, token);
				}
				catch (Exception e) {
					fire("listener", 2, workflow.getId(), "Could not execute workflow listener", Notification.format(e), Severity.ERROR, token);
				}
			}
		}
	}
	
	private Object getVariable(ComplexContent pipeline, String query) throws ServiceException {
		VariableOperation.registerRoot();
		try {
			Operation<ComplexContent> analyze = new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse(query));
			return analyze.evaluate(pipeline);
		}
		catch (EvaluationException e) {
			throw new ServiceException("WORKFLOW-9", "Could not evaluate the variable", e);
		}
		catch (ParseException e) {
			throw new ServiceException("WORKFLOW-10", "Could not parse the variable query", e);
		}
		finally {
			VariableOperation.unregisterRoot();
		}
	}

	private void continueWorkflow(String connectionId, WorkflowInstance workflow, List<WorkflowTransitionInstance> history, List<WorkflowInstanceProperty> properties, Token token, WorkflowState targetState, ComplexContent output) {
		ComplexContent content = getStateEvaluationType(targetState.getId()).newInstance();
		content.set("properties", propertiesToObject(properties));
		content.set("state", output == null ? null : output.get("state"));
		List<WorkflowTransition> possibleTransitions = new ArrayList<WorkflowTransition>(targetState.getTransitions());
		// we add the transitions of extended states, they can also autotrigger
		if (targetState.getExtensions() != null) {
			for (String name : targetState.getExtensions()) {
				WorkflowState extended = getStateById(name);
				if (extended.getTransitions() != null) {
					for (WorkflowTransition transition : extended.getTransitions()) {
						if (!possibleTransitions.contains(transition)) {
							possibleTransitions.add(transition);
						}
					}
				}
			}
		}
		Collections.sort(possibleTransitions);
		boolean foundNext = false;
		int queryOrderMatch = -1;
		for (WorkflowTransition possibleTransition : possibleTransitions) {
			// if we already have a matching query order and the current order is higher, we will not be running any more transitions
			if (queryOrderMatch >= 0 && possibleTransition.getQueryOrder() > queryOrderMatch) {
				break;
			}
			if (canAutomaticallyTransition(possibleTransition)) {
				String query = possibleTransition.getQuery();
				Boolean value = null;
				try {
					if (!analyzedOperations.containsKey(query)) {
						synchronized(analyzedOperations) {
							if (!analyzedOperations.containsKey(query)) {
								analyzedOperations.put(query, (TypeOperation) new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse(query)));
							}
						}
					}
					value = (Boolean) analyzedOperations.get(query).evaluate(content);
				}
				catch (Exception e) {
					logger.error("There is an invalid query '" + query + "' for workflow: " + getId(), e);
				}
				try {
					if (value != null && value) {
						// we only ever execute automatic self transitions once, otherwise we can end up in an unending loop or force the user to always validate this themselves
						if (isSelfTransition(possibleTransition) && hasOccurred(possibleTransition, history)) {
							continue;
						}
						queryOrderMatch = possibleTransition.getQueryOrder();
						foundNext = true;
						// this allows us to easily build in asynchronous and/or timed executions
						if (possibleTransition.getTarget() != null) {
							String cleanName = EAIRepositoryUtils.stringToField(possibleTransition.getName());
							String serviceId = getId() + ".services.transition." + cleanName;
							DefinedService transitionService = (DefinedService) getRepository().resolve(serviceId);
							if (transitionService == null) {
								throw new IllegalStateException("Could not find transition service: " + serviceId);
							}
							MaskedContent masked = new MaskedContent(content, transitionService.getServiceInterface().getInputDefinition());
							masked.set("bestEffort", "true");
							masked.set("workflowId", workflow.getId());
							masked.set("connectionId", connectionId);
							
							ExecutorProvider executor = new RepositoryExecutorProvider(getRepository());
							Map<String, String> targetProperties = possibleTransition.getTargetProperties();
							Map<String, Object> parameters = new HashMap<String, Object>();
							if (targetProperties != null) {
								for (Map.Entry<String, String> entry : targetProperties.entrySet()) {
									if (entry.getValue() != null && entry.getValue().startsWith("=")) {
										parameters.put(entry.getKey(), getVariable(masked, entry.getValue().substring(1)));
									}
									else {
										parameters.put(entry.getKey(), entry.getValue());
									}
								}
							}
							ServiceRunner runner = executor.getRunner(possibleTransition.getTarget(), parameters);
							
							runner.run(transitionService, ServiceRuntime.getRuntime().getExecutionContext(), masked);
						}
						// this is more performant because we don't have to retrieve the history & properties again
						else {
							run(connectionId, workflow, history, properties, possibleTransition, token, content);
						}
						// we don't allow multiple transitions to be executed anymore, it is impossible in a FSM to have multiple states (if parallel transitions were to go to a different state)
						// but even if they go to the same state, suppose 1 succeeds and the other fails, did it arrive in that state? do we automatically continue?
						break;
					}
				}
				catch (Exception e) {
					logger.error("Could not automatically transition to " + possibleTransition.getName(), e);
				}
			}
		}
		// update the workflow to WAITING
		if (!foundNext) {
			WorkflowState currentState = getStateById(workflow.getStateId());
			boolean isFinalState = isFinalState(currentState);
			
			workflow.setTransitionState(isFinalState ? Level.SUCCEEDED : Level.WAITING);
			runTransactionally(new TransactionableAction<Void>() {
				@Override
				public Void call(String transactionId) throws Exception {
					WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
					workflowManager.updateWorkflow(connectionId, transactionId, workflow);
					return null;
				}
			});
			// if we are a final state and in a batch, continue the batch
			if (isFinalState && workflow.getBatchId() != null) {
				continueBatchIfNecessary(connectionId, workflow, token, getConfig().getProvider().getWorkflowManager());
			}
		}
	}
	
	private boolean isSelfTransition(WorkflowTransition transition) {
		for (WorkflowState state : getConfig().getStates()) {
			// if it starts from this state and goes back to this state, it is a self transition
			if (state.getTransitions() != null && state.getTransitions().contains(transition)) {
				return transition.getTargetStateId().contentEquals(state.getId());
			}
		}
		return false;
	}
	
	private boolean hasOccurred(WorkflowTransition transition, List<WorkflowTransitionInstance> history) {
		if (history != null) {
			for (WorkflowTransitionInstance instance : history) {
				if (instance.getDefinitionId().equals(transition.getId())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean continueBatch(WorkflowBatchInstance batch) {
		String connectionId = getConfig().getConnection() == null ? null : getConfig().getConnection().getId();
		WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
		Level batchState = workflowManager.calculateBatchState(connectionId, batch.getId());
		if (batchState == Level.STOPPED) {
			// we update it to stopped to signal that we want to continue with the next part
			batch.setState(Level.WAITING);
			// make sure we take control of the batch
			batch.setSystemId(getRepository().getName());
			batch.setStopped(new Date());
			return runTransactionally(new TransactionableAction<Boolean>() {
				@Override
				public Boolean call(String transactionId) throws Exception {
					WorkflowManager workflowManager = getConfig().getProvider().getWorkflowManager();
					// if we were able to start the batch
					if (workflowManager.updateBatch(connectionId, transactionId, batch)) {
						return true;
					}
					return false;
				}
			});
		}
		return false;
	}
	
	private boolean canAutomaticallyTransition(WorkflowTransition transition) {
		// if we have no query, we can't do it automatically
		if (transition.getQuery() == null) {
			return false;
		}
		// if we have a required input, we can't automatically transition
		DefinedStructure definedStructure = getStructures().get(transition.getId());
		for (Element<?> child : TypeUtils.getAllChildren(definedStructure)) {
			Value<Integer> minOccurs = child.getProperty(MinOccursProperty.getInstance());
			if (minOccurs == null || minOccurs.getValue() != 0) {
				return false;
			}
		}
		return true;
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
		for (WorkflowState state : getConfig().getStates()) {
			if (state.getId().equals(id) || state.getName().equals(id)) {
				return state;
			}
		}
		return null;
	}
	
	public WorkflowState getTransitionFromState(String transitionId) {
		for (WorkflowState state : getConfig().getStates()) {
			if (state.getTransitions() != null) {
				for (WorkflowTransition transition : state.getTransitions()) {
					if (transition.getId().equals(transitionId) || transition.getName().equals(transitionId)) {
						return state;
					}
				}
			}
		}
		return null;
	}
	
	public WorkflowTransition getTransitionById(String id) {
		for (WorkflowState state : getConfig().getStates()) {
			WorkflowTransition potential = getTransitionById(id, state);
			if (potential != null) {
				return potential;
			}
		}
		return null;
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

	public boolean isExtensionState(String id) {
		if (getConfig().getStates() != null) {
			for (WorkflowState state : getConfig().getStates()) {
				if (state.getExtensions() != null && state.getExtensions().contains(id)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public Collection<WorkflowState> getInitialStates() {
		Map<String, WorkflowState> initialStates = new HashMap<String, WorkflowState>();
		List<String> targetedStates = new ArrayList<String>();
		for (WorkflowState state : getConfig().getStates()) {
			if (!state.isGlobalState()) {
				initialStates.put(state.getId(), state);
			}
			for (WorkflowTransition transition : state.getTransitions()) {
				targetedStates.add(transition.getTargetStateId());
			}
		}
		for (String targetedState : targetedStates) {
			initialStates.remove(targetedState);
		}
		return initialStates.values();
	}

	public boolean isFinalState(WorkflowState state) {
		if (state.getFinalState() != null) {
			return state.getFinalState();
		}
		// if it has transitions, it is not final (by default)
		if (state.getTransitions() != null && !state.getTransitions().isEmpty()) {
			return false;
		}
		else if (state.getExtensions() != null) {
			for (String extensionId : state.getExtensions()) {
				WorkflowState extended = getStateById(extensionId);
				if (!isFinalState(extended)) {
					return false;
				}
			}
		}
		return true;
	}
	
	public Collection<WorkflowState> getFinalStates() {
		Map<String, WorkflowState> finalStates = new HashMap<String, WorkflowState>();
		for (WorkflowState state : getConfig().getStates()) {
			if (isFinalState(state)) {
				finalStates.put(state.getId(), state);
			}
		}
		return finalStates.values();
	}
	
	public Map<String, VMService> getMappings() {
		return mappings;
	}

	public Map<String, DefinedStructure> getStructures() {
		return structures;
	}
	
	public RoleHandler getRoleHandler() {
		if (getConfig().getRoleService() != null) {
			return POJOUtils.newProxy(RoleHandler.class, getConfig().getRoleService(), getRepository(), SystemPrincipal.ROOT);
		}
		return null;
	}
	public PermissionHandler getPermissionHandler() {
		if (getConfig().getPermissionService() != null) {
			return POJOUtils.newProxy(PermissionHandler.class, getConfig().getPermissionService(), getRepository(), SystemPrincipal.ROOT);
		}
		return null;
	}
	public TokenValidator getTokenValidator() {
		if (getConfig().getTokenValidatorService() != null) {
			return POJOUtils.newProxy(TokenValidator.class, getConfig().getTokenValidatorService(), getRepository(), SystemPrincipal.ROOT);
		}
		return null;
	}

	
	/******************************* WEB FRAGMENT *******************************/
	
	private Map<String, List<EventSubscription<?, ?>>> subscriptions = new HashMap<String, List<EventSubscription<?, ?>>>();
	
	private String getKey(WebApplication artifact, String path) {
		return artifact.getId() + ":" + path;
	}
	
	@Override
	public void start(WebApplication artifact, String path) throws IOException {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			stop(artifact, path);
		}
		
		String fullPath = artifact.getServerPath();
		if (path != null && !path.isEmpty() && !path.equals("/")) {
			if (!fullPath.endsWith("/")) {
				fullPath += "/";
			}
			fullPath += path.replaceFirst("^[/]+", "");
		}
		if (!fullPath.endsWith("/")) {
			fullPath += "/";
		}
		EventDispatcher dispatcher = artifact.getConfiguration().getVirtualHost().getDispatcher();
		// we want to send the full path without base path here as the getPath() in the workflow transition service includes the base path
		EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, new WorkflowRESTListener(artifact, this, Charset.defaultCharset(), null, fullPath));
		// to filter, we want to add the base path as well
		if (getConfig().getBasePath() != null) {
			fullPath += getConfig().getBasePath();
			fullPath = fullPath.replaceAll("[/]{2,}", "/");
		}
		subscription.filter(HTTPServerUtils.limitToPath(fullPath));
		
		List<EventSubscription<?, ?>> list = subscriptions.get(key);
		if (list == null) {
			list = new ArrayList<EventSubscription<?, ?>>();
			synchronized(subscriptions) {
				subscriptions.put(key, list);
			}
		}
		list.add(subscription);
	}

	@Override
	public void stop(WebApplication artifact, String path) {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			synchronized(subscriptions) {
				if (subscriptions.containsKey(key)) {
					for (EventSubscription<?, ?> subscription : subscriptions.get(key)) {
						subscription.unsubscribe();
					}
					subscriptions.remove(key);
				}
			}
		}
	}

	@Override
	public List<Permission> getPermissions(WebApplication artifact, String path) {
		return new ArrayList<Permission>();
	}

	@Override
	public boolean isStarted(WebApplication artifact, String path) {
		return subscriptions.containsKey(getKey(artifact, path));
	}

	@Override
	public List<RESTFragment> getFragments(boolean limitToUser, Token token) {
		List<RESTFragment> fragments = new ArrayList<RESTFragment>();
		if (getConfig().getStates() != null) {
			Collection<WorkflowState> initialStates = getInitialStates();
			for (WorkflowState state : getConfig().getStates()) {
				boolean isInitial = initialStates.contains(state);
				for (WorkflowTransition transition : state.getTransitions()) {
					String cleanName = EAIRepositoryUtils.stringToField(transition.getName());
					String serviceId = getId() + ".services." + (isInitial ? "initial" : "transition") + "." + cleanName;
					WorkflowTransitionService service = (WorkflowTransitionService) getRepository().resolve(serviceId);
					if (service == null) {
						throw new IllegalStateException("Can not find the service: "  + serviceId);
					}
					fragments.add(new RESTFragment() {
						@Override
						public String getId() {
							return service.getId();
						}
						@Override
						public String getPath() {
							return Workflow.this.getId() + "/" + cleanName;
						}
						@Override
						public String getMethod() {
							return isInitial ? "POST" : "PUT";
						}
						@Override
						public List<String> getConsumes() {
							return Arrays.asList("application/json", "application/xml");
						}
						@Override
						public List<String> getProduces() {
							return Arrays.asList("application/json", "application/xml");
						}
						@Override
						public Type getInput() {
							return service.getServiceInterface().getInputDefinition();
						}
						@Override
						public Type getOutput() {
							return service.getServiceInterface().getOutputDefinition();
						}
						@Override
						public List<Element<?>> getQueryParameters() {
							return new ArrayList<Element<?>>();
						}
						@Override
						public List<Element<?>> getHeaderParameters() {
							return new ArrayList<Element<?>>();
						}
						@Override
						public List<Element<?>> getPathParameters() {
							return new ArrayList<Element<?>>();
						}
					});
				}
			}
		}
		return fragments;
	}
}
