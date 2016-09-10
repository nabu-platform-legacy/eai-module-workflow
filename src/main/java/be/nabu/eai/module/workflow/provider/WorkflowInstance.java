package be.nabu.eai.module.workflow.provider;

import java.util.Date;

import javax.validation.constraints.NotNull;

public class WorkflowInstance {
	private String id, parentId, definitionId;
	private Date created;
	private String stateId, batchId, contextId, environment;
	private Level transitionState;
	
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
	
	@NotNull	
	public Level getTransitionState() {
		return transitionState;
	}
	public void setTransitionState(Level transitionState) {
		this.transitionState = transitionState;
	}

	public static enum Level {
		RUNNING,
		SUCCEEDED,
		FAILED,
		REVERTED
	}
}
