package nabu.misc.workflow;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.misc.workflow.types.WorkflowDefinition;
import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;
import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionService;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.utils.KeyValuePairImpl;

@WebService
public class Services {
	
	private ExecutionContext executionContext;
	
	@WebResult(name = "workflowId")
	public String start(
			@NotNull @WebParam(name = "definitionId") String definitionId, 
			@WebParam(name = "transitionId") String transitionId, 
			@WebParam(name = "parentId") String parentId, 
			@WebParam(name = "batchId") String batchId, 
			@WebParam(name = "correlationId") String correlationId, 
			@WebParam(name = "contextId") String contextId, 
			@WebParam(name = "groupId") String groupId, 
			@WebParam(name = "workflowType") String workflowType, 
			@WebParam(name = "uri") URI uri, 
			@WebParam(name = "asynchronous") Boolean asynchronous) throws ServiceException {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		WorkflowTransition transition = null;
		WorkflowState state = null;
		for (WorkflowState initialState : resolve.getInitialStates()) {
			for (WorkflowTransition possible : initialState.getTransitions()) {
				if (transitionId == null) {
					if (transition == null) {
						transition = possible;
						state = initialState;
					}
					else {
						throw new IllegalArgumentException("Multiple initial transitions, you must choose one");
					}
				}
				else if (possible.getId().equals(transitionId) || possible.getName().equals(transitionId)) {
					transition = possible;
					state = initialState;
					break;
				}
			}
		}
		if (transition == null) {
			throw new IllegalArgumentException("No initial transition found that matches: " + transitionId);
		}
		
		WorkflowTransitionService service = new WorkflowTransitionService(resolve, state, transition, true);
		ServiceRuntime runtime = new ServiceRuntime(service, executionContext);
		
		ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
		input.set("parentId", parentId);
		input.set("batchId", batchId);
		input.set("correlationId", correlationId);
		input.set("contextId", contextId);
		input.set("groupId", groupId);
		input.set("workflowType", workflowType);
		input.set("uri", uri);
		input.set("asynchronous", asynchronous);
		ComplexContent run = runtime.run(input);
		return run == null ? null : (String) run.get("workflowId");
	}
	
	public void run(
			@NotNull @WebParam(name = "definitionId") String definitionId, 
			@NotNull @WebParam(name = "transitionId") String transitionId, 
			@WebParam(name = "workflowId") String workflowId, 
			@WebParam(name = "asynchronous") Boolean asynchronous) throws ServiceException {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		WorkflowTransition transition = null;
		WorkflowState state = null;
		for (WorkflowState possibleState : resolve.getConfig().getStates()) {
			for (WorkflowTransition possibleTransition : possibleState.getTransitions()) {
				if (possibleTransition.getId().equals(transitionId) || possibleTransition.getName().equals(transitionId)) {
					transition = possibleTransition;
					state = possibleState;
					break;
				}
			}
		}
		if (transition == null) {
			throw new IllegalArgumentException("No transition found that matches: " + transitionId);
		}
		
		WorkflowTransitionService service = new WorkflowTransitionService(resolve, state, transition, true);
		ServiceRuntime runtime = new ServiceRuntime(service, executionContext);
		
		ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
		input.set("workflowId", workflowId);
		input.set("asynchronous", asynchronous);
		
		runtime.run(input);
	}
	
	public void recover(@NotNull @WebParam(name = "definitionId") String definitionId) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		resolve.recover();
	}
	
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
	protected List<WorkflowState> getStates(@NotNull @WebParam(name = "definitionId") String definitionId) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		return resolve.getConfig().getStates();
	}
	
	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflowsForUser(
			@NotNull @WebParam(name = "definitionId") String definitionId, 
			@NotNull @WebParam(name = "stateId") String stateId, 
			@WebParam(name = "token") Token token, 
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
			@WebParam(name = "limit") Integer limit) {
		return getAnyWorkflows(definitionId, stateId, token, Level.WAITING, from, until, environment, parentId, batchId, correlationId, contextId, groupId, workflowType, properties, offset, limit);
	}
	
	@WebResult(name = "workflow")
	public WorkflowInstance getWorkflow(@NotNull @WebParam(name = "definitionId") String definitionId, @NotNull @WebParam(name = "workflowId") String workflowId) {
		Workflow resolve = (Workflow) ArtifactResolverFactory.getInstance().getResolver().resolve(definitionId);
		if (resolve == null) {
			throw new IllegalArgumentException("Could not find a workflow with id: " + definitionId);
		}
		return resolve.getConfig().getProvider().getWorkflowManager().getWorkflow(resolve.getConfig().getConnection() == null ? null : resolve.getConfig().getConnection().getId(), workflowId);
	}

	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflows(
			@NotNull @WebParam(name = "definitionId") String definitionId, 
			@WebParam(name = "stateId") String stateId, 
			@WebParam(name = "transactionState") Level level, 
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
			@WebParam(name = "limit") Integer limit) {
		return getAnyWorkflows(definitionId, stateId, null, level, from, until, environment, parentId, batchId, correlationId, contextId, groupId, workflowType, properties, offset, limit);
	}
	
	private List<WorkflowInstance> getAnyWorkflows(
			@NotNull @WebParam(name = "definitionId") String definitionId, 
			@WebParam(name = "stateId") String stateId, 
			@WebParam(name = "token") Token token, 
			@WebParam(name = "transactionState") Level level, 
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
			@WebParam(name = "limit") Integer limit) {
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
				if (potential.getName().equals(stateId) || potential.getId().equals(stateId)) {
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
			environment, 
			parentId, 
			batchId, 
			correlationId, 
			contextId, 
			groupId, 
			workflowType,
			properties,
			offset, 
			limit
		);
	}
	
	@WebResult(name = "propertyDefinitions")
	protected List<KeyValuePair> getPropertyDefinitions(@NotNull @WebParam(name = "definitionId") String definitionId) {
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
	protected List<String> getDefinitionIds() {
		List<Workflow> artifacts = EAIResourceRepository.getInstance().getArtifacts(Workflow.class);
		List<String> definitions = new ArrayList<String>();
		if (artifacts != null) {
			for (Workflow artifact : artifacts) {
				definitions.add(artifact.getId());
			}
		}
		Collections.sort(definitions);
		return definitions;
	}
	
	@WebResult(name = "definitions")
	public List<WorkflowDefinition> getDefinitions() {
		List<Workflow> artifacts = EAIResourceRepository.getInstance().getArtifacts(Workflow.class);
		List<WorkflowDefinition> definitions = new ArrayList<WorkflowDefinition>();
		if (artifacts != null) {
			for (Workflow artifact : artifacts) {
				WorkflowDefinition definition = new WorkflowDefinition();
				definition.setDefinitionId(artifact.getId());
				definition.setConnectionId(artifact.getConfig().getConnection() == null ? null : artifact.getConfig().getConnection().getId());
				definition.setProviderId(artifact.getConfig().getProvider() == null ? null : artifact.getConfig().getProvider().getId());
				
				List<KeyValuePair> properties = new ArrayList<KeyValuePair>();
				ComplexType propertyDefinition = artifact.getPropertyDefinition();
				for (Element<?> element : TypeUtils.getAllChildren(propertyDefinition)) {
					if (element.getType() instanceof SimpleType) {
						properties.add(new KeyValuePairImpl(element.getName(), ((SimpleType<?>) element.getType()).getInstanceClass().getName()));
					}
				}
				definition.setProperties(properties);
				definition.setStates(artifact.getConfig().getStates());
				definitions.add(definition);
			}
		}
		Collections.sort(definitions, new Comparator<WorkflowDefinition>() {
			@Override
			public int compare(WorkflowDefinition o1, WorkflowDefinition o2) {
				return o1.getDefinitionId().compareTo(o2.getDefinitionId());
			}
		});
		return definitions;
	}
}
