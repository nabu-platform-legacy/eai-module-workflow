package be.nabu.eai.module.workflow;

import java.util.List;

// can not directly refer to target state as this may result in circular references!!
// must refer to the id of the target state, separate resolving
public class WorkflowTransition {
	// a generated if for this state
	private String id;
	// the name of the state
	private String name, description;
	// the state we move to after the transition is done
	private String targetStateId;
	// the roles that are allowed to run this transition
	private List<String> roles;
	// query used to trigger this transition
	private String query;
	// the connecting circle
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

	public String getTargetStateId() {
		return targetStateId;
	}
	public void setTargetStateId(String targetStateId) {
		this.targetStateId = targetStateId;
	}
	
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
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
