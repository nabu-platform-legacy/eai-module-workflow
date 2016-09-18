package nabu.misc.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;

@WebService
public class Services {
	
	@WebResult(name = "states")
	public List<String> getActionableStates(@NotNull @WebParam(name = "definitionId") String definitionId, @WebParam(name = "token") Token token) {
		List<String> states = new ArrayList<String>();
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		TokenValidator tokenValidator = resolve.getTokenValidator();
		if (tokenValidator != null && token != null && !tokenValidator.isValid(token)) {
			token = null;
		}
		RoleHandler roleHandler = resolve.getRoleHandler();
		states: for (WorkflowState state : resolve.getConfig().getStates()) {
			if (state.getTransitions() != null) {
				for (WorkflowTransition transition : state.getTransitions()) {
					if (transition.getRoles() != null && !transition.getRoles().isEmpty()) {
						if (roleHandler == null) {
							throw new IllegalStateException("The workflow '" + definitionId + "' does have roles but no role handler");
						}
						for (String role : transition.getRoles()) {
							if (roleHandler.hasRole(token, role)) {
								states.add(state.getName());
								continue states;
							}
						}
					}
					// anonymous access to this transition
					else {
						states.add(state.getName());
						continue states;
					}
				}
			}
		}
		return states;
	}
	
	@WebResult(name = "properties")
	public List<WorkflowInstanceProperty> getProperties(@NotNull @WebParam(name = "definitionId") String definitionId, @NotNull @WebParam(name = "workflowId") String workflowId) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		return resolve.getConfig().getProvider().getWorkflowManager().getWorkflowProperties(
			resolve.getConfig().getConnection() == null ? null : resolve.getConfig().getConnection().getId(), 
			workflowId
		);
	}
	
	@WebResult(name = "transitions")
	public List<WorkflowTransitionInstance> getHistory(@NotNull @WebParam(name = "definitionId") String definitionId, @NotNull @WebParam(name = "workflowId") String workflowId) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		List<WorkflowTransitionInstance> transitions = resolve.getConfig().getProvider().getWorkflowManager().getTransitions(
			resolve.getConfig().getConnection() == null ? null : resolve.getConfig().getConnection().getId(), 
			workflowId
		);
		if (transitions != null) {
			Collections.sort(transitions);
		}
		return transitions;
	}
	
	@WebResult(name = "states")
	public List<WorkflowState> getStates(@NotNull @WebParam(name = "definitionId") String definitionId) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		return resolve.getConfig().getStates();
	}
	
	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflowsForUser(@NotNull @WebParam(name = "definitionId") String definitionId, @NotNull @WebParam(name = "state") String state, @WebParam(name = "token") Token token, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit) {
		return getAnyWorkflows(definitionId, state, token, Level.WAITING, offset, limit);
	}

	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflows(@NotNull @WebParam(name = "definitionId") String definitionId, @WebParam(name = "state") String state, @WebParam(name = "transactionState") Level level, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit) {
		return getAnyWorkflows(definitionId, state, null, level, offset, limit);
	}
	
	private List<WorkflowInstance> getAnyWorkflows(@NotNull @WebParam(name = "definitionId") String definitionId, @WebParam(name = "state") String state, @WebParam(name = "token") Token token, @WebParam(name = "transactionState") Level level, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		if (token != null && state != null && !getActionableStates(definitionId, token).contains(state)) {
			throw new SecurityException("The user does not have access to workflows of type '" + definitionId + "' in state '" + state + "'");
		}
		WorkflowState workflowState = null;
		if (state != null) {
			for (WorkflowState potential : resolve.getConfig().getStates()) {
				if (potential.getName().equals(state)) {
					workflowState = potential;
					break;
				}
			}
			if (workflowState == null) {
				throw new IllegalArgumentException("'" + state + "' is not a valid state for the workflow '" + definitionId + "'");
			}
		}
		return resolve.getConfig().getProvider().getWorkflowManager().getWorkflows(
			resolve.getConfig().getConnection() == null ? null : resolve.getConfig().getConnection().getId(), 
			definitionId, 
			workflowState == null ? null : workflowState.getId(), 
			level, 
			offset, 
			limit
		);
	}
}
