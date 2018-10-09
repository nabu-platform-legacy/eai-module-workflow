package be.nabu.eai.module.workflow.provider;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ExecutionContextProvider;
import be.nabu.libs.services.pojo.POJOUtils;

public class WorkflowProvider extends JAXBArtifact<WorkflowProviderConfiguration> {

	private WorkflowManager manager;
	
	public static ThreadLocal<ExecutionContext> executionContext = new ThreadLocal<ExecutionContext>();
	
	public WorkflowProvider(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow-provider.xml", WorkflowProviderConfiguration.class);
	}
	
	public static void commit(String transactionId) {
		if (executionContext.get() != null) {
			executionContext.get().getTransactionContext().commit(transactionId);
		}
		else if (ServiceRuntime.getRuntime() != null) {
			ServiceRuntime.getRuntime().getExecutionContext().getTransactionContext().commit(transactionId);
		}
		else {
			throw new RuntimeException("Could not commit execution context");
		}
	}
	
	public static void rollback(String transactionId) {
		if (executionContext.get() != null) {
			executionContext.get().getTransactionContext().rollback(transactionId);
		}
		else if (ServiceRuntime.getRuntime() != null) {
			ServiceRuntime.getRuntime().getExecutionContext().getTransactionContext().rollback(transactionId);
		}
		else {
			throw new RuntimeException("Could not rollback execution context");
		}
	}

	public WorkflowManager getWorkflowManager() {
		if (manager == null) {
			synchronized(this) {
				if (manager == null) {
					try {
						manager = POJOUtils.newProxy(WorkflowManager.class, new ExecutionContextProvider() {
								@Override
								public ExecutionContext newExecutionContext(Token primary, Token...alternatives) {
									if (executionContext.get() == null) {
										executionContext.set(getRepository().newExecutionContext(primary, alternatives));
									}
									return executionContext.get();
								}
							},
							SystemPrincipal.ROOT, 
							getConfiguration().getCreateWorkflow(),
							getConfiguration().getUpdateWorkflow(),
							getConfiguration().getGetWorkflow(),
							getConfiguration().getGetWorkflows(),
							getConfiguration().getCreateWorkflowProperties(),
							getConfiguration().getUpdateWorkflowProperties(),
							getConfiguration().getGetWorkflowProperties(),
							getConfiguration().getCreateTransition(),
							getConfiguration().getUpdateTransition(),
							getConfiguration().getGetTransitions(),
							getConfiguration().getGetAmountOfWorkflows(),
							getConfiguration().getMergeDefinition(),
							getConfiguration().getGetDefinition()
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
