package be.nabu.eai.module.workflow.provider;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class WorkflowProviderManager extends JAXBArtifactManager<WorkflowProviderConfiguration, WorkflowProvider> {

	public WorkflowProviderManager() {
		super(WorkflowProvider.class);
	}

	@Override
	protected WorkflowProvider newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WorkflowProvider(id, container, repository);
	}

}
