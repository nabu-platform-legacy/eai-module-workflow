package be.nabu.eai.module.workflow.transition;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import nabu.misc.workflow.Services;
import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.Workflow.TransactionableAction;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.provider.WorkflowManager;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.types.api.ComplexContent;

public class WorkflowTransitionServiceInstance implements ServiceInstance {
	
	private WorkflowTransitionService service;
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	public WorkflowTransitionServiceInstance(WorkflowTransitionService service) {
		this.service = service;
	}

	@Override
	public Service getDefinition() {
		return service;
	}

	private String getConnectionId(ComplexContent input) {
		// you can give it in the input
		String connectionId = input == null ? null : (String) input.get("connectionId");
		// or configure it
		if (connectionId == null) {
			connectionId = service.getWorkflow().getConfig().getConnection() == null ? null : service.getWorkflow().getConfig().getConnection().getId();
		}
		// or we try to dynamically figure it out
		if (connectionId == null) {
			connectionId = Workflow.deduceConnectionId(service.getWorkflow());
		}
		return connectionId;
	}
	
	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		WorkflowInstance instance;
		List<WorkflowTransitionInstance> history = new ArrayList<WorkflowTransitionInstance>();
		List<WorkflowInstanceProperty> properties = new ArrayList<WorkflowInstanceProperty>();
		
		// transactions are a tricky bit, a workflow generally performs actions, some of which can not be reversed
		// perhaps it is better to always locally transact and simply go back to a previous state or offer compensation mechanisms
		// until this is cleared up, transactions are always managed by the workflow
		final String connectionId = getConnectionId(input);
		if (service.isInitial()) {
			instance = new WorkflowInstance();
			instance.setVersion(service.getWorkflow().getVersion());
			if (input != null) {
				instance.setId((UUID) input.get("workflowId"));
				instance.setParentId((UUID) input.get("parentId"));
				instance.setCorrelationId((String) input.get("correlationId"));
				instance.setBatchId((UUID) input.get("batchId"));
				instance.setGroupId((String) input.get("groupId"));
				instance.setContextId((String) input.get("contextId"));
				instance.setWorkflowType((String) input.get("workflowType"));
				instance.setUri((URI) input.get("uri"));
			}
			// generate an id if none was passed in
			if (instance.getId() == null) {
				instance.setId(UUID.randomUUID());
			}
			instance.setStarted(new Date());
			instance.setDefinitionId(service.getWorkflow().getId());
			instance.setEnvironment(service.getWorkflow().getRepository().getGroup());
			instance.setStateId(service.getFromState().getId());
			instance.setTransitionState(Level.RUNNING);
			
			// when creating a workflow and we are interested in versioning, make sure the version is persisted somewhere
			if (service.getWorkflow().getConfig().isVersion() && service.getWorkflow().getConfig().getProvider().getConfig().getMergeDefinition() != null) {
				service.getWorkflow().getConfig().getProvider().getWorkflowManager()
					.mergeDefinition(Services.buildDefinition(service.getWorkflow()));
			}
			
			Workflow.runTransactionally(new TransactionableAction<Void>() {
				@Override
				public Void call(String transactionId) throws Exception {
					service.getWorkflow().getConfig().getProvider().getWorkflowManager()
						.createWorkflow(connectionId, transactionId, instance);
					return null;
				}
			});
		}
		else {
			UUID workflowId = input == null ? null : (UUID) input.get("workflowId");
			if (workflowId == null) {
				throw new ServiceException("WORKFLOW-1", "No workflow id given (" + service.getWorkflow().getId() + ")");
			}
			WorkflowManager workflowManager = service.getWorkflow().getConfig().getProvider().getWorkflowManager();
			
			instance = workflowManager.getWorkflow(connectionId, workflowId);
			
			if (instance == null) {
				throw new ServiceException("WORKFLOW-3", "The workflow id '" + workflowId + "' is not a workflow instance (" + service.getWorkflow().getId() + ")");
			}
			
			if (!service.getWorkflow().getId().equals(instance.getDefinitionId())) {
				throw new ServiceException("WORKFLOW-6", "The workflow is not of the correct type, expecting a '" + service.getWorkflow().getId() + "' but received an instance of '" + instance.getDefinitionId() + "'");
			}
			
			// we can not operate on an anonymized instance by default
			// workflows are sequences of steps that build on state, if we throw away part of that state, it is likely not going to function well anymore
			// you can't bypass this with force either atm
			// cause we _could_ allow that, but than every transition afterwards would need to be forced, as the anonymized boolean stays on
			// so we would have to think on unsetting the boolean? we're gonna wait for an actual usecase
			if (instance.getAnonymized() != null && instance.getAnonymized()) {
				throw new ServiceException("WORKFLOW-ANONYMIZED", "Workflow " + workflowId + " (" + service.getWorkflow().getId() + ") is anonymized, you can no longer perform transitions on it");
			}
			
			// a global state can be triggered from anywhere
			if (!instance.getStateId().equals(service.getFromState().getId()) && !service.getFromState().isGlobalState()) {
				// check if the "from state" is actually an extension
				WorkflowState state = service.getWorkflow().getStateById(instance.getStateId());
				
				// check if the state is the same as the source state of this service or an extension
				boolean isExtension = isExtension(state, new HashSet<WorkflowState>());
				
				if (!isExtension) {
					Boolean force = (Boolean) input.get("force");
					if (force == null || !force) {
						Boolean bestEffort = (Boolean) input.get("bestEffort");
						if (bestEffort != null && bestEffort) {
							logger.warn("Skipped best effort transition: " + service.getId());
							return null;
						}
						throw new ServiceException("WORKFLOW-0", "Workflow " + workflowId + " (" + service.getWorkflow().getId() + ") is not in the correct state to trigger this transition");
					}
				}
			}
			
			List<WorkflowTransitionInstance> transitions = workflowManager.getTransitions(connectionId, workflowId);
			if (transitions != null) {
				history.addAll(transitions);
			}
			List<WorkflowInstanceProperty> workflowProperties = workflowManager.getWorkflowProperties(connectionId, workflowId);
			if (workflowProperties != null) {
				properties.addAll(workflowProperties);
			}
		}
		
		// TODO: deprecated!
		Boolean asynchronous = input == null ? null : (Boolean) input.get("asynchronous");
		
		// TODO: in the future we can have a generic concept of a "thread pool" which you can share between for example workflows to make sure they don't use too many resources
		// can scale this to http servers etc as well
		if (asynchronous != null && asynchronous) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						service.getWorkflow().run(connectionId, instance, history, properties, service.getTransition(), executionContext.getSecurityContext().getToken(), input);
					}
					catch (ServiceException e) {
						LoggerFactory.getLogger(service.getWorkflow().getId()).error("Transition '" + service.getId() + "' exited with exception", e);
						logger.error("Workflow stopped with exception", e);
					}
				}
			}).start();
		}
		else {
			service.getWorkflow().run(connectionId, instance, history, properties, service.getTransition(), executionContext.getSecurityContext().getToken(), input);
		}
		
		ComplexContent output = service.getServiceInterface().getOutputDefinition().newInstance();
		if (service.isInitial()) {
			output.set("workflowId", instance.getId());
		}
		return output;
	}
	
	private boolean isExtension(WorkflowState currentState, Set<WorkflowState> checkedStates) {
		// the same state, we good to go
		if (currentState.getId().equals(service.getFromState().getId())) {
			return true;
		}
		// we already checked this state and apparently concluded it was not in there
		else if (checkedStates.contains(currentState)) {
			return false;
		}
		// always add the current state to prevent circular lookups
		checkedStates.add(currentState);
		// the state extends other states, check those as well
		if (currentState.getExtensions() != null) {
			for (UUID stateId : currentState.getExtensions()) {
				WorkflowState stateById = service.getWorkflow().getStateById(stateId);
				if (isExtension(stateById, checkedStates)) {
					return true;
				}
			}
		}
		return false;
	}
}
