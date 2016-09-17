package nabu.misc.workflow.types;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.libs.types.api.KeyValuePair;

@XmlRootElement
@XmlType(propOrder = { "id", "workflowId", "key", "value", "transitionId" })
public class WorkflowInstanceProperty implements KeyValuePair {

	private String id, workflowId, key, value, transitionId;

	@NotNull
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@NotNull
	public String getWorkflowId() {
		return workflowId;
	}
	public void setWorkflowId(String workflowId) {
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
	public String getTransitionId() {
		return transitionId;
	}
	public void setTransitionId(String transitionId) {
		this.transitionId = transitionId;
	}
	
}
