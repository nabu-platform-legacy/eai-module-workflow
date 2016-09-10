package be.nabu.eai.module.workflow.provider;

import java.security.Principal;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ExecutionContextProvider;
import be.nabu.libs.services.pojo.POJOUtils;

public class WorkflowProvider extends JAXBArtifact<WorkflowProviderConfiguration> {

	private WorkflowManager manager;
	
	public static ThreadLocal<ExecutionContext> executionContext = new ThreadLocal<ExecutionContext>();
	
	public WorkflowProvider(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow-provider.xml", WorkflowProviderConfiguration.class);
	}

	public WorkflowManager getWorkflowManager() {
		if (manager == null) {
			synchronized(this) {
				if (manager == null) {
					try {
						manager = POJOUtils.newProxy(WorkflowManager.class, new ExecutionContextProvider() {
								@Override
								public ExecutionContext newExecutionContext(Principal principal) {
									if (executionContext.get() == null) {
										executionContext.set(getRepository().newExecutionContext(principal));
									}
									return executionContext.get();
								}
							},
							SystemPrincipal.ROOT, 
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
