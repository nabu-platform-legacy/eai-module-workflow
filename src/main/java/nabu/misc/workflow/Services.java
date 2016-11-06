package nabu.misc.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.utils.KeyValuePairImpl;

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
								states.add(state.getId());
								continue states;
							}
						}
					}
					// anonymous access to this transition
					else {
						states.add(state.getId());
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
	public List<WorkflowInstance> getWorkflowsForUser(@NotNull @WebParam(name = "definitionId") String definitionId, @NotNull @WebParam(name = "stateId") String stateId, @WebParam(name = "token") Token token, @WebParam(name = "from") Date from, @WebParam(name = "until") Date until, @WebParam(name = "properties") List<KeyValuePair> properties, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit) {
		return getAnyWorkflows(definitionId, stateId, token, Level.WAITING, from, until, properties, offset, limit);
	}

	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflows(@NotNull @WebParam(name = "definitionId") String definitionId, @WebParam(name = "stateId") String stateId, @WebParam(name = "transactionState") Level level, @WebParam(name = "from") Date from, @WebParam(name = "until") Date until, @WebParam(name = "properties") List<KeyValuePair> properties, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit) {
		return getAnyWorkflows(definitionId, stateId, null, level, from, until, properties, offset, limit);
	}
	
	private List<WorkflowInstance> getAnyWorkflows(@NotNull @WebParam(name = "definitionId") String definitionId, @WebParam(name = "stateId") String stateId, @WebParam(name = "token") Token token, @WebParam(name = "transactionState") Level level, @WebParam(name = "from") Date from, @WebParam(name = "until") Date until, @WebParam(name = "properties") List<KeyValuePair> properties, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		if (token != null && stateId != null && !getActionableStates(definitionId, token).contains(stateId)) {
			throw new SecurityException("The user does not have access to workflows of type '" + definitionId + "' in state '" + stateId + "'");
		}
		WorkflowState workflowState = null;
		if (stateId != null) {
			for (WorkflowState potential : resolve.getConfig().getStates()) {
				if (potential.getName().equals(stateId)) {
					workflowState = potential;
					break;
				}
			}
			if (workflowState == null) {
				throw new IllegalArgumentException("'" + stateId + "' is not a valid state for the workflow '" + definitionId + "'");
			}
		}
		return resolve.getConfig().getProvider().getWorkflowManager().getWorkflows(
			resolve.getConfig().getConnection() == null ? null : resolve.getConfig().getConnection().getId(), 
			definitionId, 
			workflowState == null ? null : workflowState.getId(), 
			level, 
			from,
			until,
			properties,
			offset, 
			limit
		);
	}
	
	@WebResult(name = "propertyDefinitions")
	public List<KeyValuePair> getPropertyDefinitions(@NotNull @WebParam(name = "definitionId") String definitionId) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		List<KeyValuePair> properties = new ArrayList<KeyValuePair>();
		ComplexType propertyDefinition = resolve.getPropertyDefinition();
		for (Element<?> element : TypeUtils.getAllChildren(propertyDefinition)) {
			if (element.getType() instanceof SimpleType) {
				properties.add(new KeyValuePairImpl(element.getName(), ((SimpleType<?>) element.getType()).getInstanceClass().getName()));
			}
		}
		return properties;
	}
	
	@WebResult(name = "definitionIds")
	public List<String> getDefinitionIds() {
		List<Workflow> artifacts = EAIResourceRepository.getInstance().getArtifacts(Workflow.class);
		List<String> definitions = new ArrayList<String>();
		if (artifacts != null) {
			for (Workflow artifact : artifacts) {
				definitions.add(artifact.getId());
			}
		}
		return definitions;
	}
}
