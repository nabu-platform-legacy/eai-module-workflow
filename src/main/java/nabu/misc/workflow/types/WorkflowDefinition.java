package nabu.misc.workflow.types;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.libs.types.api.KeyValuePair;

@XmlRootElement(name = "workflowDefinition")
@XmlType(propOrder = { "definitionId", "connectionId", "providerId", "properties", "states" })
public class WorkflowDefinition {
	private String definitionId, connectionId, providerId;
	private Long version;
	private List<WorkflowState> states;
	private List<KeyValuePair> properties;
	public String getDefinitionId() {
		return definitionId;
	}
	public void setDefinitionId(String definitionId) {
		this.definitionId = definitionId;
	}
	public String getConnectionId() {
		return connectionId;
	}
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}
	public String getProviderId() {
		return providerId;
	}
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	public List<WorkflowState> getStates() {
		return states;
	}
	public void setStates(List<WorkflowState> states) {
		this.states = states;
	}
	public List<KeyValuePair> getProperties() {
		return properties;
	}
	public void setProperties(List<KeyValuePair> properties) {
		this.properties = properties;
	}
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}
}
