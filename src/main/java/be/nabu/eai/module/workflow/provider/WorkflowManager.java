package be.nabu.eai.module.workflow.provider;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;

public interface WorkflowManager {
	public void createWorkflow(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowInstance instance);
	public void updateWorkflow(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowInstance instance);
	@WebResult(name = "workflow")
	public WorkflowInstance getWorkflow(@WebParam(name = "workflowId") @NotNull String workflowId);
	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflows(@WebParam(name = "systemId") @NotNull String systemId, @WebParam(name = "definitionId") String definitionId, Level state);
	
	public void createWorkflowProperties(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "properties") List<WorkflowInstanceProperty> properties);
	public void updateWorkflowProperties(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "properties") List<WorkflowInstanceProperty> properties);
	@WebResult(name = "properties")
	public List<WorkflowInstanceProperty> getWorkflowProperties(@WebParam(name = "workflowId") @NotNull String workflowId);
	
	public void createTransition(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowTransitionInstance instance);
	public void updateTransition(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowTransitionInstance instance);
	@WebResult(name = "transitions")
	public List<WorkflowTransitionInstance> getTransitions(@WebParam(name = "workflowId") @NotNull String workflowId);
}
