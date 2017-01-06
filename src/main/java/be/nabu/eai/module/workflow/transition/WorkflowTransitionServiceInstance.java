package be.nabu.eai.module.workflow.transition;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.Workflow.TransactionableAction;
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

	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		WorkflowInstance instance;
		List<WorkflowTransitionInstance> history = new ArrayList<WorkflowTransitionInstance>();
		List<WorkflowInstanceProperty> properties = new ArrayList<WorkflowInstanceProperty>();
		
		String connectionId = service.getWorkflow().getConfig().getConnection() == null ? null : service.getWorkflow().getConfig().getConnection().getId();
		if (service.isInitial()) {
			instance = new WorkflowInstance();
			instance.setId(UUID.randomUUID().toString().replace("-", ""));
			if (input != null) {
				instance.setParentId((String) input.get("parentId"));
				instance.setCorrelationId((String) input.get("correlationId"));
				instance.setBatchId((String) input.get("batchId"));
				instance.setGroupId((String) input.get("groupId"));
				instance.setContextId((String) input.get("contextId"));
				instance.setWorkflowType((String) input.get("workflowType"));
				instance.setUri((URI) input.get("uri"));
			}
			instance.setStarted(new Date());
			instance.setDefinitionId(service.getWorkflow().getId());
			instance.setEnvironment(service.getWorkflow().getRepository().getGroup());
			instance.setStateId(service.getFromState().getId());
			instance.setTransitionState(Level.RUNNING);
			
			Workflow.runTransactionally(new TransactionableAction<Void>() {
				@Override
				public Void call(String transactionId) throws Exception {
					service.getWorkflow().getConfig().getProvider().getWorkflowManager().createWorkflow(connectionId, transactionId, instance);
					return null;
				}
			});
		}
		else {
			String workflowId = input == null ? null : (String) input.get("workflowId");
			if (workflowId == null) {
				throw new ServiceException("WORKFLOW-1", "No workflow id given");
			}
			WorkflowManager workflowManager = service.getWorkflow().getConfig().getProvider().getWorkflowManager();
			
			instance = workflowManager.getWorkflow(connectionId, workflowId);
			
			if (!instance.getStateId().equals(service.getFromState().getId())) {
				throw new ServiceException("WORKFLOW-0", "Workflow " + workflowId + " is not in the correct state to trigger this transition");
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
		
		Boolean asynchronous = input == null ? null : (Boolean) input.get("asynchronous");
		
		// TODO: in the future we can have a generic concept of a "thread pool" which you can share between for example workflows to make sure they don't use too many resources
		// can scale this to http servers etc as well
		if (asynchronous != null && asynchronous) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						service.getWorkflow().run(instance, history, properties, service.getTransition(), executionContext.getSecurityContext().getToken(), input);
					}
					catch (ServiceException e) {
						LoggerFactory.getLogger(service.getWorkflow().getId()).error("Transition '" + service.getId() + "' exited with exception", e);
						logger.error("Workflow stopped with exception", e);
					}
				}
			}).start();
		}
		else {
			service.getWorkflow().run(instance, history, properties, service.getTransition(), executionContext.getSecurityContext().getToken(), input);
		}
		
		ComplexContent output = service.getServiceInterface().getOutputDefinition().newInstance();
		if (service.isInitial()) {
			output.set("workflowId", instance.getId());
		}
		return output;
	}
}
