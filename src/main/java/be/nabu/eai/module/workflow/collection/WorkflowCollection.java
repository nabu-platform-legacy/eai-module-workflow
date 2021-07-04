package be.nabu.eai.module.workflow.collection;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.developer.api.CollectionManager;
import be.nabu.eai.developer.collection.EAICollectionUtils;
import be.nabu.eai.repository.api.Entry;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class WorkflowCollection implements CollectionManager {

	private Entry entry;

	public WorkflowCollection(Entry entry) {
		this.entry = entry;
	}

	@Override
	public Entry getEntry() {
		return entry;
	}

	@Override
	public boolean hasSummaryView() {
		return true;
	}

	@Override
	public Node getSummaryView() {
		List<Button> buttons = new ArrayList<Button>();
		buttons.add(EAICollectionUtils.newViewButton(entry));
		buttons.add(EAICollectionUtils.newDeleteButton(entry, null));
		List<Entry> children = new ArrayList<Entry>();
		Entry initial = entry.getRepository().getEntry(entry.getId() + ".services.initial");
		for (Entry child : initial) {
			children.add(child);
		}
		Entry transition = entry.getRepository().getEntry(entry.getId() + ".services.transition");
		for (Entry child : transition) {
			children.add(child);
		}
		return EAICollectionUtils.newSummaryTile(entry, "workflow-large.png", children, buttons);
	}
	
}
