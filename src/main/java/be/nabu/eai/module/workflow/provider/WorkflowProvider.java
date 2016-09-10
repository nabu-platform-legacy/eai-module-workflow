package be.nabu.eai.module.workflow.provider;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import be.nabu.eai.module.workflow.provider.WorkflowInstance.Level;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.pojo.POJOUtils;

public class WorkflowProvider extends JAXBArtifact<WorkflowProviderConfiguration> implements StartableArtifact {

	private boolean started;
	private WorkflowManager manager;
	
	public WorkflowProvider(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow-provider.xml", WorkflowProviderConfiguration.class);
	}

	@Override
	public void start() throws IOException {
		if (!started) {
			if (getConfiguration().getGetRunningWorkflows() != null) {
				WorkflowManager workflowManager = getWorkflowManager();
				List<WorkflowInstance> runningWorkflows = workflowManager.getRunningWorkflows(getRepository().getName());
				for (WorkflowInstance workflow : runningWorkflows) {
					List<TransitionInstance> transitions = workflowManager.getTransitions(workflow.getId());
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
						workflowManager.updateTransition(running);
					}
					workflow.setTransitionState(Level.REVERTED);
					workflowManager.updateWorkflow(workflow);
				}
			}
			started = true;
		}
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	public WorkflowManager getWorkflowManager() {
		if (manager == null) {
			synchronized(this) {
				if (manager == null) {
					try {
						manager = POJOUtils.newProxy(WorkflowManager.class, getRepository(), SystemPrincipal.ROOT, 
							getConfiguration().getCreateWorkflow(),
							getConfiguration().getUpdateWorkflow(),
							getConfiguration().getGetWorkflow(),
							getConfiguration().getGetRunningWorkflows(),
							getConfiguration().getCreateWorkflowProperty(),
							getConfiguration().getUpdateWorkflowProperty(),
							getConfiguration().getGetWorkflowProperties(),
							getConfiguration().getCreateTransition(),
							getConfiguration().getUpdateTransition(),
							getConfiguration().getGetTransitions()
						);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return manager;
	}
}
