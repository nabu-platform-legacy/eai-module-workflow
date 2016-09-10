package be.nabu.eai.module.workflow;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.eai.module.workflow.provider.WorkflowProperty;

public interface TransitionPicker {
	@WebResult(name = "transition")
	public String pickTransition(@WebParam(name = "workflowId") String workflowId, @WebParam(name = "state") String stateName, @WebParam(name = "properties") List<WorkflowProperty> properties);
}
