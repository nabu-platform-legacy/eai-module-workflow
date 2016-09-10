package be.nabu.eai.module.workflow;

import java.util.ArrayList;
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
	// visual positioning
	private double x, y;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public DefinedService getTransitionPicker() {
		return transitionPicker;
	}
	public void setTransitionPicker(DefinedService transitionPicker) {
		this.transitionPicker = transitionPicker;
	}
	public boolean isStateless() {
		return stateless;
	}
	public void setStateless(boolean stateless) {
		this.stateless = stateless;
	}
	public List<WorkflowTransition> getTransitions() {
		if (transitions == null) {
			transitions = new ArrayList<WorkflowTransition>();
		}
		return transitions;
	}
	public void setTransitions(List<WorkflowTransition> transitions) {
		this.transitions = transitions;
	}
	
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	
}
