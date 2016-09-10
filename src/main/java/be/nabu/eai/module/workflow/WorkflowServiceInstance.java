package be.nabu.eai.module.workflow;

import java.util.Date;
import java.util.UUID;

import be.nabu.eai.module.workflow.provider.WorkflowInstance;
import be.nabu.eai.module.workflow.provider.WorkflowInstance.Level;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.types.api.ComplexContent;

public class WorkflowServiceInstance implements ServiceInstance {

	private Workflow workflow;

	public WorkflowServiceInstance(Workflow workflow) {
		this.workflow = workflow;
	}
	
	@Override
	public Service getDefinition() {
		return workflow;
	}

	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(UUID.randomUUID().toString());
		instance.setBatchId((String) input.get("batchId"));
		instance.setContextId((String) input.get("contextId"));
		instance.setParentId((String) input.get("parentId"));
		instance.setCreated(new Date());
		instance.setDefinitionId(workflow.getId());
		// TODO: this should ideally be the server group name (not yet implemented), not the server name
		instance.setEnvironment(workflow.getRepository().getName());
		instance.setTransitionState(Level.RUNNING);
		
		String transactionId = UUID.randomUUID().toString();
		try {
			workflow.getConfiguration().getProvider().getWorkflowManager().createWorkflow(transactionId, instance);
			WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
		}
		catch (Exception e) {
			WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
			throw new RuntimeException(e);
		}
		finally {
			WorkflowProvider.executionContext.set(null);
		}
		
		try {
			for (WorkflowTransition transition : workflow.getConfiguration().getInitialTransitions()) {
				Object object = input.get(EAIRepositoryUtils.stringToField(transition.getName()));
				if (object != null) {
					workflow.run(instance, transition, executionContext.getSecurityContext().getPrincipal(), 0, null, null);
					break;
				}
			}
		}
		catch (Exception e) {
			throw new ServiceException(e);
		}
		
		ComplexContent output = workflow.getServiceInterface().getOutputDefinition().newInstance();
		output.set("workflow", instance);
		return output;
	}

}
