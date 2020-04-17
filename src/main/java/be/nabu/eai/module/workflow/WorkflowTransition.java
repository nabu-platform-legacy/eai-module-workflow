package be.nabu.eai.module.workflow;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Hidden;
import be.nabu.libs.services.vm.step.Invoke.KeyValueMapAdapter;

// can not directly refer to target state as this may result in circular references!!
// must refer to the id of the target state, separate resolving
public class WorkflowTransition implements Comparable<WorkflowTransition> {
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
	// the order in which the transition queries are executed, higher means later
	private int queryOrder;
	// whether or not this transition should start a batch
	private boolean startBatch;
	// the connecting circle
	private double x, y;
	// the operation id for this transition
	private String operationId;
	// the permission context to use (if any)
	private String permissionContext, permissionAction;
	
	private String target;
	
	private Map<String, String> targetProperties;
	
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
	public int getQueryOrder() {
		return queryOrder;
	}
	public void setQueryOrder(int queryOrder) {
		this.queryOrder = queryOrder;
	}
	@Override
	public int compareTo(WorkflowTransition o) {
		return queryOrder - o.queryOrder;
	}
	public boolean isStartBatch() {
		return startBatch;
	}
	public void setStartBatch(boolean startBatch) {
		this.startBatch = startBatch;
	}

	// should not be persisted but...in a hurry! @hackathon gsk 2018-01-18
	@Hidden
	@XmlTransient
	public String getOperationId() {
		return operationId;
	}
	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}
	
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getTargetProperties() {
		return targetProperties;
	}
	public void setTargetProperties(Map<String, String> targetProperties) {
		this.targetProperties = targetProperties;
	}
	
	public String getPermissionContext() {
		return permissionContext;
	}
	public void setPermissionContext(String permissionContext) {
		this.permissionContext = permissionContext;
	}
	public String getPermissionAction() {
		return permissionAction;
	}
	public void setPermissionAction(String permissionAction) {
		this.permissionAction = permissionAction;
	}
	
}
