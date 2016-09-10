package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import be.nabu.eai.module.services.vm.VMServiceManager;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.container.ContainerArtifactManager.WrapperEntry;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.validator.api.Validation;

public class WorkflowManager extends JAXBArtifactManager<WorkflowConfiguration, Workflow> {

	public WorkflowManager() {
		super(Workflow.class);
	}

	@Override
	protected Workflow newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new Workflow(id, container, repository);
	}

	@Override
	public Workflow load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		Workflow workflow = super.load(entry, messages);
		Resource privateDirectory = entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			for (Resource child : (ResourceContainer<?>) privateDirectory) {
				VMService loaded = new VMServiceManager().load(new WrapperEntry(entry.getRepository(), entry, (ResourceContainer<?>) child, child.getName()), messages);
				workflow.getMappings().put(child.getName(), loaded);
			}
		}
		return workflow;
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, Workflow artifact) throws IOException {
		List<Validation<?>> messages = super.save(entry, artifact);
		// remove all folders in private
		Resource privateDirectory = entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			((ManageableContainer<?>) entry.getContainer()).delete(EAIResourceRepository.PRIVATE);
		}
		privateDirectory = ((ManageableContainer<?>) entry.getContainer()).create(EAIResourceRepository.PRIVATE, Resource.CONTENT_TYPE_DIRECTORY);
		for (String name : artifact.getMappings().keySet()) {
			ManageableContainer<?> create = (ManageableContainer<?>) ((ManageableContainer<?>) privateDirectory).create(name, Resource.CONTENT_TYPE_DIRECTORY);
			messages.addAll(new VMServiceManager().save(new WrapperEntry(entry.getRepository(), entry, create, name), artifact.getMappings().get(name)));
		}
		return messages;
	}
	
}
