package be.nabu.eai.module.workflow;

import java.util.List;

import be.nabu.libs.services.api.DefinedService;

public class WorkflowState {
	// a generated if for this state
	private String id;
	// the name of the state
	private String name, description;
	// a service that can pick a certain transition (by name)
	private DefinedService transitionPicker;
	// whether this state is stateless... basically means there is only the workflow state, no additional state-state
	// this means we can not use outputs from previous transitions directly for optimization
	// but it also means we can do stuff like retry, parallel etc when a flow is in such a state
	private boolean stateless;
	// the possible transitions out of this state
	private List<WorkflowTransition> transitions;
}
