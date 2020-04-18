package nabu.misc.workflow.types;

import java.util.Date;

import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.api.annotation.Field;
import nabu.misc.workflow.types.WorkflowInstance.Level;

@ComplexTypeDescriptor(collectionName = "WorkflowBatchInstances",
	propOrder = { "id", "workflowId", "transitionId", "systemId", "started", "created", "stopped", "state" })
public class WorkflowBatchInstance {
	// the id of the batch and of the workflow that created the batch as well as the transition that created the batch
	private String id, workflowId, transitionId, systemId;
	// when the batch instance was started, when all batch children were created
	// and when the batch was stopped
	private Date started, created, stopped;
	// the state of the batch
	private Level state;
	
	@Field(primary = true)
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getWorkflowId() {
		return workflowId;
	}
	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}
	public String getTransitionId() {
		return transitionId;
	}
	public void setTransitionId(String transitionId) {
		this.transitionId = transitionId;
	}
	public Date getStarted() {
		return started;
	}
	public void setStarted(Date started) {
		this.started = started;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getStopped() {
		return stopped;
	}
	public void setStopped(Date stopped) {
		this.stopped = stopped;
	}
	public Level getState() {
		return state;
	}
	public void setState(Level state) {
		this.state = state;
	}
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
}
