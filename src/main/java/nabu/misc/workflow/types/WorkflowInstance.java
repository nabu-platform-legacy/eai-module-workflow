package nabu.misc.workflow.types;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.api.annotation.Field;

@ComplexTypeDescriptor(collectionName = "WorkflowInstances",
	propOrder = { "id", "definitionId", "version", "parentId", "batchId", "contextId", "groupId", "correlationId", "workflowType", "uri", "started", "stopped", "environment", "transitionState", "stateId", "anonymized" })
public class WorkflowInstance {
	private UUID id, parentId, batchId, stateId;
	private String definitionId;
	private Date started, stopped;
	private String contextId, groupId, correlationId, environment, workflowType;
	private Level transitionState;
	private URI uri;
	// the version of the workflow you are running
	private Long version;
	private Boolean anonymized;
	
	@Field(primary = true)
	@NotNull
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	
	@Field(foreignKey = "nabu.misc.workflow.types.WorkflowInstance:id")
	public UUID getParentId() {
		return parentId;
	}
	public void setParentId(UUID parentId) {
		this.parentId = parentId;
	}
	
	@Field(foreignKey = "nabu.misc.workflow.types.workflowDefinition:id")
	@NotNull
	public String getDefinitionId() {
		return definitionId;
	}
	public void setDefinitionId(String definitionId) {
		this.definitionId = definitionId;
	}
	
	@NotNull
	public Date getStarted() {
		return started;
	}
	public void setStarted(Date started) {
		this.started = started;
	}
	
	public Date getStopped() {
		return stopped;
	}
	public void setStopped(Date stopped) {
		this.stopped = stopped;
	}

	@Field(foreignKey = "nabu.misc.workflow.types.workflowState:id")
	@NotNull
	public UUID getStateId() {
		return stateId;
	}
	public void setStateId(UUID stateId) {
		this.stateId = stateId;
	}
	
	public UUID getBatchId() {
		return batchId;
	}
	public void setBatchId(UUID batchId) {
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
	
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getCorrelationId() {
		return correlationId;
	}
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	
	public URI getUri() {
		return uri;
	}
	public void setUri(URI uri) {
		this.uri = uri;
	}

	public String getWorkflowType() {
		return workflowType;
	}
	public void setWorkflowType(String workflowType) {
		this.workflowType = workflowType;
	}

	public static enum Level {
		// ongoing (non-final)
		RUNNING,
		// waiting for external trigger, e.g. human interaction (non-final)
		WAITING,
		// a successful termination in between automation states, this should automatically be updated to either WAITING or RUNNING (non-final)
		STOPPED,
		// a runtime error occurred, this is a temporary state from which you can restart at some point or set to failed
		ERROR,
		// successfully concluded (a final state)
		SUCCEEDED,
		// the admin concluded that it will remain failed, this is a final state
		FAILED,
		// used for reverting on crash (non-final)
		REVERTED,
		// it is no longer relevant, execution has been stopped
		CANCELLED
	}

	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}
	public Boolean getAnonymized() {
		return anonymized;
	}
	public void setAnonymized(Boolean anonymized) {
		this.anonymized = anonymized;
	}
}
