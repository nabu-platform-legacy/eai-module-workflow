/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.workflow.collection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.developer.CollectionActionImpl;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.CollectionAction;
import be.nabu.eai.developer.api.CollectionManager;
import be.nabu.eai.developer.api.CollectionManagerFactory;
import be.nabu.eai.developer.api.EntryAcceptor;
import be.nabu.eai.developer.collection.EAICollectionUtils;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowManager;
import be.nabu.eai.repository.CollectionImpl;
import be.nabu.eai.repository.api.Collection;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class WorkflowCollectionFactory implements CollectionManagerFactory {

	@Override
	public CollectionManager getCollectionManager(Entry entry) {
		if (entry.isNode() && Workflow.class.equals(entry.getNode().getArtifactClass())) {
			return new WorkflowCollection(entry);
		}
		return null;
	}

	@Override
	public List<CollectionAction> getActionsFor(Entry entry) {
		List<CollectionAction> actions = new ArrayList<CollectionAction>();
		if (EAICollectionUtils.isProject(entry)) {
			actions.add(new CollectionActionImpl(EAICollectionUtils.newActionTile("workflow-large.png", "Add Workflow", "Model your business or integration process."), new javafx.event.EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					WorkflowCreate workflowCreate = new WorkflowCreate();
					EAIDeveloperUtils.buildPopup("Create Workflow", "Workflow", workflowCreate, new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							String name = workflowCreate.getName();
							// only proceed if we actually have a URI
							if (name != null && !name.trim().isEmpty()) {
								try {
									RepositoryEntry connectorEntry = getWorkflowEntry((RepositoryEntry) entry, workflowCreate.getName());
									Workflow workflow = new Workflow(connectorEntry.getId(), connectorEntry.getContainer(), connectorEntry.getRepository());
									new WorkflowManager().save(connectorEntry, workflow);
									EAIDeveloperUtils.created(workflow.getId());
									MainController.getInstance().open(workflow.getId());
								}
								catch (Exception e) {
									MainController.getInstance().notify(e);
								}
							}
						}
					}, false).show();
				}
			}, new EntryAcceptor() {
				@Override
				public boolean accept(Entry entry) {
					Collection collection = entry.getCollection();
					return collection != null && "folder".equals(collection.getType()) && "workflows".equals(collection.getSubType());
				}
			}));
		}
		return actions;
	}

	public static Entry getWorkflowsEntry(RepositoryEntry project) throws IOException {
		Entry child = EAIDeveloperUtils.mkdir(project, "workflows");
		if (!child.isCollection()) {
			CollectionImpl collection = new CollectionImpl();
			collection.setType("folder");
			collection.setSubType("workflows");
			collection.setName("Workflows");
			collection.setSmallIcon("workflow.png");
			collection.setMediumIcon("workflow-medium.png");
			collection.setLargeIcon("workflow-large.png");
			((RepositoryEntry) child).setCollection(collection);
			((RepositoryEntry) child).saveCollection();
			EAIDeveloperUtils.reload(child.getId());
		}
		return child;
	}
	
	public static RepositoryEntry getWorkflowEntry(RepositoryEntry project, String name) throws IOException {
		String normalize = EAICollectionUtils.normalize(name);
		RepositoryEntry createNode = ((RepositoryEntry) getWorkflowsEntry(project)).createNode(normalize, new WorkflowManager(), true);
		if (!normalize.equals(name)) {
			createNode.getNode().setName(name);
			createNode.saveNode();
		}
		return createNode;
	}
}
