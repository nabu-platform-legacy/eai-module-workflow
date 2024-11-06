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
