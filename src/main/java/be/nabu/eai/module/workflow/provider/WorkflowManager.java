package be.nabu.eai.module.workflow.provider;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

public interface WorkflowManager {
	public void createWorkflow(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowInstance instance);
	public void updateWorkflow(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowInstance instance);
	@WebResult(name = "workflow")
	public WorkflowInstance getWorkflow(@WebParam(name = "workflowId") @NotNull String workflowId);
	@WebResult(name = "workflows")
	public List<WorkflowInstance> getRunningWorkflows(@WebParam(name = "systemId") @NotNull String systemId, @WebParam(name = "definitionId") String definitionId);
	
	public void createWorkflowProperty(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowProperty instance);
	public void updateWorkflowProperty(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowProperty instance);
	@WebResult(name = "properties")
	public List<WorkflowProperty> getWorkflowProperties(@WebParam(name = "workflowId") @NotNull String workflowId);
	
	public void createTransition(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") TransitionInstance instance);
	public void updateTransition(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") TransitionInstance instance);
	@WebResult(name = "transitions")
	public List<TransitionInstance> getTransitions(@WebParam(name = "workflowId") @NotNull String workflowId);
}
