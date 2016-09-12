package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import be.nabu.eai.module.services.vm.VMServiceManager;
import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionService;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.container.ContainerArtifactManager.WrapperEntry;
import be.nabu.eai.repository.artifacts.container.ContainerArtifactManager.ContainerRepository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Workflow load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		Workflow workflow = super.load(entry, messages);
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			ResourceContainer<?> structures = (ResourceContainer<?>) privateDirectory.getChild("structures");
			if (structures != null) {
				for (Resource child : (ResourceContainer<?>) structures) {
					DefinedStructure loaded = new StructureManager().load(new WrapperEntry(entry.getRepository(), entry, (ResourceContainer<?>) child, child.getName()), messages);
					String group;
					String name;
					if (child.getName().equals("global")) {
						group = "types";
						name = "global";
					}
					else {
						WorkflowState stateById = workflow.getStateById(child.getName());
						if (stateById != null) {
							group = "types.states";
							name = EAIRepositoryUtils.stringToField(stateById.getName());
						}
						else {
							WorkflowTransition transitionById = workflow.getTransitionById(child.getName());
							if (transitionById == null) {
								continue;
							}
							group = "types.transitions";
							name = EAIRepositoryUtils.stringToField(transitionById.getName());
						}
					}
					loaded.setId(entry.getId() + "." + group + "." + name);
					workflow.getStructures().put(child.getName(), loaded);
				}
			}
			// Tricky stuff: the services that do the mapping are based on the documents we dynamically add to state
			ContainerRepository containerRepository = new ContainerRepository(entry.getId(), (RepositoryEntry) entry, (Collection<Artifact>) (Collection) workflow.getStructures().values());
			containerRepository.setExactAliases(true);
			for (DefinedStructure structure : workflow.getStructures().values()) {
				containerRepository.alias(entry.getId() + ":" + structure.getId(), structure.getId());
			}
			// so we can only load them after the documents are added to the tree
			ResourceContainer<?> services = (ResourceContainer<?>) privateDirectory.getChild("services");
			if (services != null) {
				for (Resource child : (ResourceContainer<?>) services) {
					VMService loaded;
					try {
						loaded = new VMServiceManager().load(new WrapperEntry(containerRepository, entry, (ResourceContainer<?>) child, child.getName()), messages);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					workflow.getMappings().put(child.getName(), loaded);
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
	
	public void refreshChildren(ModifiableEntry parent, Workflow artifact) {
		removeChildren((ModifiableEntry) parent, artifact);
		addChildren((ModifiableEntry) parent, artifact);
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry parent, Workflow artifact) {
		List<Entry> entries = new ArrayList<Entry>();
		// add the state structures
		ModifiableEntry types = EAIRepositoryUtils.getParent(parent, "types", true);
		for (String id : artifact.getStructures().keySet()) {
			String group = null;
			String name;
			if (id.equals("global")) {
				name = "global";
			}
			else {
				WorkflowState stateById = artifact.getStateById(id);
				if (stateById != null) {
					name = EAIRepositoryUtils.stringToField(stateById.getName());
					group = "states";
				}
				else {
					WorkflowTransition transitionById = artifact.getTransitionById(id);
					if (transitionById != null) {
						name = EAIRepositoryUtils.stringToField(transitionById.getName());
						group = "transitions";
					}
					else {
						throw new RuntimeException("Could not find " + id);
					}
				}
			}
			ModifiableEntry directParent = group == null ? types : EAIRepositoryUtils.getParent(types, group, true);
			EAINode node = new EAINode();
			node.setArtifactClass(DefinedStructure.class);
			node.setArtifact(artifact.getStructures().get(id));
			node.setLeaf(true);
			Entry childEntry = new MemoryEntry(parent.getRepository(), directParent, node, directParent.getId() + "." + name, name);
			// need to explicitly set id (it was loaded from file)
			artifact.getStructures().get(id).setId(childEntry.getId());
			node.setEntry(childEntry);
			directParent.addChildren(childEntry);
			entries.add(childEntry);
		}
		
		// we need a service for each transition
		ModifiableEntry services = EAIRepositoryUtils.getParent(parent, "services", true);
		
		Map<String, WorkflowState> initialStates = new HashMap<String, WorkflowState>();
		List<String> targetedStates = new ArrayList<String>();
		for (WorkflowState state : artifact.getConfig().getStates()) {
			initialStates.put(state.getId(), state);
			for (WorkflowTransition transition : state.getTransitions()) {
				targetedStates.add(transition.getTargetStateId());
			}
		}
		for (String targetedState : targetedStates) {
			initialStates.remove(targetedState);
		}
		
		// initial transitions that can create the workflow
		ModifiableEntry initial = EAIRepositoryUtils.getParent(services, "initial", true);
		for (WorkflowState state : initialStates.values()) {
			for (WorkflowTransition transition : state.getTransitions()) {
				EAINode node = new EAINode();
				node.setArtifactClass(DefinedService.class);
				WorkflowTransitionService service = new WorkflowTransitionService(artifact, state, transition, true);
				node.setArtifact(service);
				node.setLeaf(true);
				Entry childEntry = new MemoryEntry(parent.getRepository(), initial, node, service.getId(), service.getName());
				node.setEntry(childEntry);
				initial.addChildren(childEntry);
				entries.add(childEntry);
			}
		}
		
		// other transitions that can be triggered on the workflow
		ModifiableEntry transitions = EAIRepositoryUtils.getParent(services, "transition", true);
		for (WorkflowState state : artifact.getConfig().getStates()) {
			if (!initialStates.containsValue(state)) {
				for (WorkflowTransition transition : state.getTransitions()) {
					EAINode node = new EAINode();
					node.setArtifactClass(DefinedService.class);
					WorkflowTransitionService service = new WorkflowTransitionService(artifact, state, transition, false);
					node.setArtifact(service);
					node.setLeaf(true);
					Entry childEntry = new MemoryEntry(parent.getRepository(), transitions, node, service.getId(), service.getName());
					node.setEntry(childEntry);
					transitions.addChildren(childEntry);
					entries.add(childEntry);		
				}
			}
		}
		
		return entries;
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry parent, Workflow artifact) {
		List<Entry> entries = new ArrayList<Entry>();
		ModifiableEntry structures = EAIRepositoryUtils.getParent(parent, "types", true);
		removeRecursively(structures, entries);
		ModifiableEntry services = EAIRepositoryUtils.getParent(parent, "services", true);
		removeRecursively(services, entries);
		entries.add(services);
		return entries;
	}
	
	public static void removeRecursively(ModifiableEntry parent, List<Entry> entries) {
		List<String> toRemove = new ArrayList<String>();
		for (Entry child : parent) {
			entries.add(child);
			toRemove.add(child.getName());
			if (child instanceof ModifiableEntry) {
				removeRecursively((ModifiableEntry) child, entries);
			}
		}
		parent.removeChildren(toRemove.toArray(new String[toRemove.size()]));
	}
}
