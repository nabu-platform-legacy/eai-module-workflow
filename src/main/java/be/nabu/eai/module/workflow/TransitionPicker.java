package be.nabu.eai.module.workflow;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.eai.module.workflow.provider.WorkflowTransitionInstance;
import be.nabu.eai.module.workflow.provider.WorkflowInstance;
import be.nabu.eai.module.workflow.provider.WorkflowProperty;

public interface TransitionPicker {
	@WebResult(name = "transition")
	public WorkflowTransition pickTransition(@WebParam(name = "workflow") WorkflowInstance workflow, @WebParam(name = "previousTransition") WorkflowTransitionInstance previousTransition, @WebParam(name = "properties") List<WorkflowProperty> properties);
}
