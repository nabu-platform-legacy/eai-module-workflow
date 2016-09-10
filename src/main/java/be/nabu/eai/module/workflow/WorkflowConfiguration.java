package be.nabu.eai.module.workflow;

import java.util.List;

public class WorkflowConfiguration {
	private List<WorkflowTransition> initialTransitions;

	public List<WorkflowTransition> getInitialTransitions() {
		return initialTransitions;
	}

	public void setInitialTransitions(List<WorkflowTransition> initialTransitions) {
		this.initialTransitions = initialTransitions;
	}
	
}
