package be.nabu.eai.module.workflow.provider;

import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.libs.types.api.KeyValuePair;
import nabu.misc.workflow.types.WorkflowBatchInstance;
import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;

public interface WorkflowManager {
	public void createWorkflow(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowInstance instance);
	public void updateWorkflow(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowInstance instance);
	@WebResult(name = "workflow")
	public WorkflowInstance getWorkflow(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "workflowId") @NotNull String workflowId);
	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflows(
		@WebParam(name = "connectionId") String connectionId, 
		@NotNull @WebParam(name = "definitionId") String definitionId, 
		@WebParam(name = "stateId") String stateId, 
		@WebParam(name = "transitionState") Level state, 
		@WebParam(name = "from") Date from, 
		@WebParam(name = "until") Date until, 
		@WebParam(name = "environment") String environment, 
		@WebParam(name = "parentId") String parentId, 
		@WebParam(name = "batchId") String batchId, 
		@WebParam(name = "correlationId") String correlationId,
		@WebParam(name = "contextId") String contextId,
		@WebParam(name = "groupId") String groupId,
		@WebParam(name = "workflowType") String workflowType,
		@WebParam(name = "properties") List<KeyValuePair> properties, 
		@WebParam(name = "offset") Integer offset, 
		@WebParam(name = "limit") Integer limit,
		// whether or not to limit yourself to "running" workflows (in non-final states)
		@WebParam(name = "running") Boolean running);
	
	public void createWorkflowProperties(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "properties") List<WorkflowInstanceProperty> properties);
	public void updateWorkflowProperties(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "properties") List<WorkflowInstanceProperty> properties);
	@WebResult(name = "properties")
	public List<WorkflowInstanceProperty> getWorkflowProperties(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "workflowId") @NotNull String workflowId);
	
	public void createTransition(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowTransitionInstance instance);
	public void updateTransition(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowTransitionInstance instance);
	@WebResult(name = "transitions")
	public List<WorkflowTransitionInstance> getTransitions(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "workflowId") @NotNull String workflowId);

	public void createBatch(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowBatchInstance instance);
	// update the batch but do so in a way that it can not fail in a concurrent environment
	// for example only select for update or only update if the state etc is still the same
	@WebResult(name = "succeeded")
	public boolean updateBatch(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowBatchInstance instance);
	// get the current batch state, this should be:
	// RUNNING: until the "created" is filled in and all child workflows are created
	// WAITING: when all child workflows are created and we are waiting for them to be completed
	// STOPPED: when all child workflows are completed but the batch is not yet progressing (note that this is a computed state only, the batch itself will not appear in this state in the database)
	// ERROR/FAILED: at least one of the child workflows is in error (we don't continue with batch)
	// SUCCEEDED: the batch is done and all child workflows are correct
	public Level calculateBatchState(@WebParam(name = "connectionId") String connectionId, @NotNull @WebParam(name = "batchId") String batchId);
	@WebResult(name = "batch")
	public WorkflowBatchInstance getBatch(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "batchId") @NotNull String batchId);
	@WebResult(name = "batches")
	public List<WorkflowBatchInstance> getBatches(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "state") Level state, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit);
}
