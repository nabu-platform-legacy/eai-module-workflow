package be.nabu.eai.module.workflow.api;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.libs.authentication.api.Token;
import nabu.misc.workflow.types.WorkflowInstance;

public interface WorkflowListener {
	public void transition(
		@WebParam(name = "workflow") @NotNull WorkflowInstance workflow, 
		@WebParam(name = "transition") @NotNull WorkflowTransition transition,
		@WebParam(name = "fromState") WorkflowState fromState,
		@WebParam(name = "toState") @NotNull WorkflowState toState,
		@WebParam(name = "token") Token token);
}
