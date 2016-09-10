package be.nabu.eai.module.workflow.provider;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class WorkflowProviderGUIManager extends BaseJAXBGUIManager<WorkflowProviderConfiguration, WorkflowProvider> {

	public WorkflowProviderGUIManager() {
		super("Workflow Provider", WorkflowProvider.class, new WorkflowProviderManager(), WorkflowProviderConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected WorkflowProvider newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new WorkflowProvider(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Workflow";
	}
}
