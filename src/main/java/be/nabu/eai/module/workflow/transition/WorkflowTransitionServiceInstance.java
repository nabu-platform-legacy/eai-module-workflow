package be.nabu.eai.module.workflow.transition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
		if (service.isInitial()) {
			instance = new WorkflowInstance();
			instance.setId(UUID.randomUUID().toString());
			instance.setParentId((String) input.get("parentId"));
			instance.setContextId((String) input.get("contextId"));
			instance.setBatchId((String) input.get("batchId"));
			instance.setStarted(new Date());
			instance.setDefinitionId(service.getWorkflow().getId());
			// TODO: update to cluster name instead of system name
			instance.setEnvironment(service.getWorkflow().getRepository().getName());
			instance.setStateId(service.getFromState().getId());
			instance.setTransitionState(Level.RUNNING);
			
			Workflow.runTransactionally(new TransactionableAction<Void>() {
				@Override
				public Void call(String transactionId) throws Exception {
					service.getWorkflow().getConfig().getProvider().getWorkflowManager().createWorkflow(transactionId, instance);
					return null;
				}
			});
		}
		else {
			String workflowId = (String) input.get("workflowId");
			if (workflowId == null) {
				throw new ServiceException("WORKFLOW-1", "No workflow id given");
			}
			WorkflowManager workflowManager = service.getWorkflow().getConfig().getProvider().getWorkflowManager();
			instance = workflowManager.getWorkflow(workflowId);
			List<WorkflowTransitionInstance> transitions = workflowManager.getTransitions(workflowId);
			if (transitions != null) {
				history.addAll(transitions);
			}
			List<WorkflowInstanceProperty> workflowProperties = workflowManager.getWorkflowProperties(workflowId);
			if (workflowProperties != null) {
				properties.addAll(workflowProperties);
			}
		}
		
		service.getWorkflow().run(instance, history, properties, service.getTransition(), executionContext.getSecurityContext().getPrincipal(), input);
		
		ComplexContent output = service.getServiceInterface().getOutputDefinition().newInstance();
		if (service.isInitial()) {
			output.set("workflowId", instance.getId());
		}
		return output;
	}
}
