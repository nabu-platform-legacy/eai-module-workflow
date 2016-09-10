package be.nabu.eai.module.workflow.provider;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import be.nabu.libs.types.api.KeyValuePair;

public class WorkflowInstance {
	private String id, parentId, definitionId;
	private Date created;
	private String stateId, batchId, contextId, environment;
	private List<KeyValuePair> properties;
	
	@NotNull
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	
	@NotNull
	public String getDefinitionId() {
		return definitionId;
	}
	public void setDefinitionId(String definitionId) {
		this.definitionId = definitionId;
	}
	
	@NotNull
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	
	public String getStateId() {
		return stateId;
	}
	public void setStateId(String stateId) {
		this.stateId = stateId;
	}
	
	public String getBatchId() {
		return batchId;
	}
	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}
	
	public String getContextId() {
		return contextId;
	}
	public void setContextId(String contextId) {
		this.contextId = contextId;
	}
	
	@NotNull
	public String getEnvironment() {
		return environment;
	}
	public void setEnvironment(String environment) {
		this.environment = environment;
	}
	
	public List<KeyValuePair> getProperties() {
		return properties;
	}
	public void setProperties(List<KeyValuePair> properties) {
		this.properties = properties;
	}
	
}
