package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import be.nabu.eai.module.services.vm.VMServiceManager;
import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.container.ContainerArtifactManager.WrapperEntry;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.Validation;

public class WorkflowManager extends JAXBArtifactManager<WorkflowConfiguration, Workflow> implements ArtifactRepositoryManager<Workflow> {

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
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			ResourceContainer<?> services = (ResourceContainer<?>) privateDirectory.getChild("services");
			if (services != null) {
				for (Resource child : (ResourceContainer<?>) services) {
					VMService loaded = new VMServiceManager().load(new WrapperEntry(entry.getRepository(), entry, (ResourceContainer<?>) child, child.getName()), messages);
					workflow.getMappings().put(child.getName(), loaded);
				}
			}
			ResourceContainer<?> structures = (ResourceContainer<?>) privateDirectory.getChild("structures");
			if (structures != null) {
				for (Resource child : (ResourceContainer<?>) structures) {
					DefinedStructure loaded = new StructureManager().load(new WrapperEntry(entry.getRepository(), entry, (ResourceContainer<?>) child, child.getName()), messages);
					workflow.getStructures().put(child.getName(), loaded);
				}
			}
		}
		return workflow;
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, Workflow artifact) throws IOException {
		List<Validation<?>> messages = super.save(entry, artifact);
		if (messages == null) {
			messages = new ArrayList<Validation<?>>();
		}
		// remove all folders in private
		Resource privateDirectory = entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			((ManageableContainer<?>) entry.getContainer()).delete(EAIResourceRepository.PRIVATE);
		}
		privateDirectory = ((ManageableContainer<?>) entry.getContainer()).create(EAIResourceRepository.PRIVATE, Resource.CONTENT_TYPE_DIRECTORY);
		
		ManageableContainer<?> services = (ManageableContainer<?>) ((ManageableContainer<?>) privateDirectory).create("services", Resource.CONTENT_TYPE_DIRECTORY);
		ManageableContainer<?> structures = (ManageableContainer<?>) ((ManageableContainer<?>) privateDirectory).create("structures", Resource.CONTENT_TYPE_DIRECTORY);
		
		// save services
		for (String name : artifact.getMappings().keySet()) {
			ManageableContainer<?> create = (ManageableContainer<?>) services.create(name, Resource.CONTENT_TYPE_DIRECTORY);
			messages.addAll(new VMServiceManager().save(new WrapperEntry(entry.getRepository(), entry, create, name), artifact.getMappings().get(name)));
		}
		
		// save structures
		for (String name : artifact.getStructures().keySet()) {
			ManageableContainer<?> create = (ManageableContainer<?>) structures.create(name, Resource.CONTENT_TYPE_DIRECTORY);
			messages.addAll(new StructureManager().save(new WrapperEntry(entry.getRepository(), entry, create, name), artifact.getStructures().get(name)));
		}
		return messages;
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry parent, Workflow artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		// add the state structures
		ModifiableEntry structures = EAIRepositoryUtils.getParent(parent, "state", true);
		for (String id : artifact.getStructures().keySet()) {
			String name;
			if (id.equals("global")) {
				name = "global";
			}
			else {
				WorkflowState stateById = artifact.getStateById(id);
				if (stateById == null) {
					throw new RuntimeException("Could not find state with id: " + id);
				}
				name = EAIRepositoryUtils.stringToField(stateById.getName());
			}
			EAINode node = new EAINode();
			node.setArtifactClass(DefinedStructure.class);
			node.setArtifact(artifact.getStructures().get(id));
			node.setLeaf(true);
			Entry childEntry = new MemoryEntry(parent.getRepository(), structures, node, structures.getId() + "." + name, name);
			// need to explicitly set id (it was loaded from file)
			artifact.getStructures().get(id).setId(childEntry.getId());
			node.setEntry(childEntry);
			structures.addChildren(childEntry);
			entries.add(childEntry);
		}
		return entries;
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry parent, Workflow artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		ModifiableEntry structures = EAIRepositoryUtils.getParent(parent, "state", true);
		Iterator<Entry> iterator = structures.iterator();
		while (iterator.hasNext()) {
			entries.add(iterator.next());
			iterator.remove();
		}
		return entries;
	}
	
}
