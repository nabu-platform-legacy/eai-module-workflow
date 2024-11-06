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

package be.nabu.eai.module.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class WorkflowState {
	// a generated if for this state
	private UUID id;
	// the name of the state
	private String name, description;
	// the possible transitions out of this state
	private List<WorkflowTransition> transitions;
	// you can extend this state with other states
	// this allows for reusable transitions
	private List<UUID> extensions;
	// visual positioning
	private double x, y;
	// a global state is inherently extended by all other states, this means it has no extensions, nothing going to it but it is _not_ an initial state
	// nor will it require a force to run it, no matter in which state the workflow is
	// this is mostly interesting for generic stuff like cancel or for example adding a comment, attachment... something that is always allowed
	private boolean globalState;
	// final states are normally calculated
	// however, you can explicitly choose to mark a state as final (or not final), use with caution
	private Boolean finalState;
	
	@XmlJavaTypeAdapter(value = UuidXmlAdapter.class)
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<WorkflowTransition> getTransitions() {
		if (transitions == null) {
			transitions = new ArrayList<WorkflowTransition>();
		}
		return transitions;
	}
	public void setTransitions(List<WorkflowTransition> transitions) {
		this.transitions = transitions;
	}
	
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	
	@XmlJavaTypeAdapter(value = UuidXmlAdapter.class)
	public List<UUID> getExtensions() {
		return extensions;
	}
	public void setExtensions(List<UUID> extensions) {
		this.extensions = extensions;
	}
	public boolean isGlobalState() {
		return globalState;
	}
	public void setGlobalState(boolean globalState) {
		this.globalState = globalState;
	}
	public Boolean getFinalState() {
		return finalState;
	}
	public void setFinalState(Boolean finalState) {
		this.finalState = finalState;
	}
}
