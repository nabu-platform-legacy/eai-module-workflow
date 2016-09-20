package nabu.misc.workflow.types;

import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nabu.misc.workflow.types.WorkflowInstance.Level;

@XmlRootElement
@XmlType(propOrder = { "id", "definitionId", "workflowId", "parentId", "actorId", "systemId", "started", "stopped", "log", "errorLog", "errorCode", "sequence", "transitionState", "fromStateId", "toStateId", "batchId" })
public class WorkflowTransitionInstance implements Comparable<WorkflowTransitionInstance> {
	// the parent id is of the transition that came before
	private String id, workflowId, parentId, definitionId;
	// whoever triggered this transition
	private String actorId;
	// the system that performed the action
	private String systemId;
	// when it started and stopped
	private Date started, stopped;
	// any pertinent logs
	private String log, errorLog, errorCode;
	// the sequence number of this transition, its order in all the transitions
	private int sequence;
	// the state the transition ended in
	private Level transitionState;
	// the state ids involved
	private String fromStateId, toStateId;
	// if this transition started a new batch, what is the id?
	// while the batch is running this transition will be put into "WAITING" mode until the batch is resolved
	// if the batch has no conclusion handler configured, the system will find any transition with that batch id and go from there
	private String batchId;
	
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
	
	public String getActorId() {
		return actorId;
	}
	public void setActorId(String actorId) {
		this.actorId = actorId;
	}
	
	@NotNull
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
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
	
	public String getLog() {
		return log;
	}
	public void setLog(String log) {
		this.log = log;
	}
	
	public String getErrorLog() {
		return errorLog;
	}
	public void setErrorLog(String errorLog) {
		this.errorLog = errorLog;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	
	public int getSequence() {
		return sequence;
	}
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	
	public Level getTransitionState() {
		return transitionState;
	}
	public void setTransitionState(Level transitionState) {
		this.transitionState = transitionState;
	}
	@Override
	public int compareTo(WorkflowTransitionInstance o) {
		return sequence - o.sequence;
	}
	public String getFromStateId() {
		return fromStateId;
	}
	public void setFromStateId(String fromStateId) {
		this.fromStateId = fromStateId;
	}
	public String getToStateId() {
		return toStateId;
	}
	public void setToStateId(String toStateId) {
		this.toStateId = toStateId;
	}
	public String getBatchId() {
		return batchId;
	}
	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}
}
