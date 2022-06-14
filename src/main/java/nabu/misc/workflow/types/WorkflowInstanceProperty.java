package nabu.misc.workflow.types;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;

@ComplexTypeDescriptor(collectionName = "WorkflowInstanceProperties",
	propOrder = { "id", "workflowId", "key", "value", "transitionId" })
public class WorkflowInstanceProperty implements KeyValuePair {

	private UUID id, workflowId, transitionId;
	private String key, value;

	@NotNull
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	
	@NotNull
	public UUID getWorkflowId() {
		return workflowId;
	}
	public void setWorkflowId(UUID workflowId) {
		this.workflowId = workflowId;
	}
	
	@Override
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	@NotNull
	public UUID getTransitionId() {
		return transitionId;
	}
	public void setTransitionId(UUID transitionId) {
		this.transitionId = transitionId;
	}
	
}
