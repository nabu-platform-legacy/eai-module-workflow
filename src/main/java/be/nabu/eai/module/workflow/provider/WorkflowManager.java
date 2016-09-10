package be.nabu.eai.module.workflow.provider;

import java.util.List;

public interface WorkflowManager {
	public void createWorkflow(WorkflowInstance instance);
	public void updateWorkflow(WorkflowInstance instance);
	public WorkflowInstance getWorkflow(String id);
	
	public void createWorkflowProperty(WorkflowProperty instance);
	public void updateWorkflowProperty(WorkflowProperty instance);
	public List<WorkflowProperty> getWorkflowProperties(String id);
	
	public void createTransition(TransitionInstance instance);
	public void updateTransition(TransitionInstance instance);
	public TransitionInstance getTransition(String id);
}
