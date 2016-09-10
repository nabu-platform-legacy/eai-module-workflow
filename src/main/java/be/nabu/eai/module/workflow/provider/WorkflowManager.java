package be.nabu.eai.module.workflow.provider;

import java.util.List;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

public interface WorkflowManager {
	public void createWorkflow(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowInstance instance);
	public void updateWorkflow(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowInstance instance);
	public WorkflowInstance getWorkflow(@WebParam(name = "workflowId") @NotNull String workflowId);
	public List<WorkflowInstance> getRunningWorkflows(@WebParam(name = "systemId") @NotNull String systemId);
	
	public void createWorkflowProperty(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowProperty instance);
	public void updateWorkflowProperty(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowProperty instance);
	public List<WorkflowProperty> getWorkflowProperties(@WebParam(name = "workflowId") @NotNull String workflowId);
	
	public void createTransition(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") TransitionInstance instance);
	public void updateTransition(@WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") TransitionInstance instance);
	public List<TransitionInstance> getTransitions(@WebParam(name = "workflowId") @NotNull String workflowId);
}
