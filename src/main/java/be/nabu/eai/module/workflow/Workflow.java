package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import be.nabu.eai.module.workflow.provider.TransitionInstance;
import be.nabu.eai.module.workflow.provider.WorkflowInstance;
import be.nabu.eai.module.workflow.provider.WorkflowManager;
import be.nabu.eai.module.workflow.provider.WorkflowInstance.Level;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;

// expose folders for each state with transition methods (input extends actual transition service input + workflow instance id)
// only expose if state is manual? as in, no transition picker
// need a way to revert a workflow to a previous state
// 		> only in case of error? or also if done
// need recovery protocol, basically if a system comes online, checks any workflows that have open transitions on their name, revert those
// 		> end up in previous state (even if automatic picker) and need to resolve manually? if stateless, can pick it up again
// retry picks closest stateless state that it passed and goes from there
// can also retry from a chosen stateless state _that the workflow passed through_
public class Workflow extends JAXBArtifact<WorkflowConfiguration> implements DefinedService, StartableArtifact {

	private boolean started;
	
	private Map<String, WorkflowTransition> transitions = new HashMap<String, WorkflowTransition>();
	private Map<String, WorkflowState> states = new HashMap<String, WorkflowState>();
	
	public Workflow(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow.xml", WorkflowConfiguration.class);
	}

	@Override
	public ServiceInterface getServiceInterface() {
		// the input has a document containing one input document per initial transition possible
		// this starts a new workflow
		// also add a batch id to link multiple workflows started in a batch
		// context id is free to choose, if not filled in, it should be the id of the workflow
		// environment is autofilled
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceInstance newInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getReferences() {
		return null;
	}

	@Override
	public void start() throws IOException {
		if (!started) {
			if (getConfiguration().getProvider().getConfiguration().getGetRunningWorkflows() != null) {
				WorkflowManager workflowManager = getConfiguration().getProvider().getWorkflowManager();
				List<WorkflowInstance> runningWorkflows = workflowManager.getRunningWorkflows(getRepository().getName(), getId());
				for (WorkflowInstance workflow : runningWorkflows) {
					String transactionId = UUID.randomUUID().toString();
					try {
						// mark workflow as reverted
						workflow.setTransitionState(Level.REVERTED);
						workflowManager.updateWorkflow(transactionId, workflow);
						
						List<TransitionInstance> transitions = workflowManager.getTransitions(workflow.getId());
						Collections.sort(transitions);
						TransitionInstance running = null;
						for (TransitionInstance transition : transitions) {
							if (transition.getTransitionState().equals(Level.RUNNING)) {
								running = transition;
								break;
							}
						}
						if (running != null) {
							// revert the original transition
							running.setTransitionState(Level.REVERTED);
							running.setStopped(new Date());
							workflowManager.updateTransition(transactionId, running);
							
							// find nearest successful transition that ended in a stateless state, we can go from there
							TransitionInstance statelessParent = null;
							for (TransitionInstance transition : transitions) {
								if (transition.getTransitionState().equals(Level.SUCCEEDED)) {
									WorkflowTransition definition = getTransitionById(transition.getDefinitionId());
									if (definition.getTargetState() != null && definition.getTargetState().isStateless()) {
										statelessParent = transition;
									}
								}
							}
							// if we have a stateless parent, build a new transition from there
							if (statelessParent != null) {
								// we first need the next transition attempted, so we know how the workflow was supposed to go
								TransitionInstance nextTransition = getTransitionBySequence(transitions, statelessParent.getSequence() + 1);
								WorkflowTransition transitionDefinition = getTransitionById(nextTransition.getDefinitionId());
								run(workflow, transitionDefinition);
							}
						}
						// commit the transaction
						WorkflowProvider.executionContext.get().getTransactionContext().commit(transactionId);
					}
					catch (Exception e) {
						if (WorkflowProvider.executionContext.get() != null) {
							WorkflowProvider.executionContext.get().getTransactionContext().rollback(transactionId);
						}
					}
					finally {
						// unset context
						WorkflowProvider.executionContext.set(null);
					}
				}
			}
			started = true;
		}
	}
	
	public static void run(WorkflowInstance workflow, WorkflowTransition transition) {
		// TODO: execute the transition on the workflow
	}
	
	public static TransitionInstance getTransitionBySequence(List<TransitionInstance> instances, int sequence) {
		for (TransitionInstance instance : instances) {
			if (instance.getSequence() == sequence) {
				return instance;
			}
		}
		return null;
	}
	
	public WorkflowTransition getTransitionById(String id) {
		if (!transitions.containsKey(id)) {
			try {
				WorkflowState fictiveState = new WorkflowState();
				fictiveState.setTransitions(getConfiguration().getInitialTransitions());
				transitions.put(id, getTransitionById(id, fictiveState));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return transitions.get(id);
	}
	
	public WorkflowState getStateById(String id) {
		if (!states.containsKey(id)) {
			try {
				WorkflowState fictiveState = new WorkflowState();
				fictiveState.setTransitions(getConfiguration().getInitialTransitions());
				states.put(id, getStateById(id, fictiveState));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return states.get(id);
	}
	
	private WorkflowTransition getTransitionById(String id, WorkflowState state) {
		if (state.getTransitions() != null) {
			for (WorkflowTransition transition : state.getTransitions()) {
				if (id.equals(transition.getId())) {
					return transition;
				}
				else if (transition.getTargetState() != null) {
					WorkflowTransition potential = getTransitionById(id, transition.getTargetState());
					if (potential != null) {
						return potential;
					}
				}
			}
		}
		return null;
	}
	
	private WorkflowState getStateById(String id, WorkflowState state) {
		if (state.getTransitions() != null) {
			for (WorkflowTransition transition : state.getTransitions()) {
				if (transition.getTargetState() != null) {
					if (id.equals(transition.getTargetState().getId())) {
						return transition.getTargetState();
					}
					else {
						WorkflowState potential = getStateById(id, transition.getTargetState());
						if (potential != null) {
							return potential;
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean isStarted() {
		return started;
	}
}
