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

package nabu.misc.workflow.types;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.api.annotation.Field;

@ComplexTypeDescriptor(collectionName = "WorkflowInstanceProperties",
	propOrder = { "id", "workflowId", "key", "value", "transitionId" })
public class WorkflowInstanceProperty implements KeyValuePair {

	private UUID id, workflowId, transitionId;
	private String key, value;

	@NotNull
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	
	@Field(foreignKey = "nabu.misc.workflow.types.WorkflowInstance:id")
	@NotNull
	public UUID getWorkflowId() {
		return workflowId;
	}
	public void setWorkflowId(UUID workflowId) {
		this.workflowId = workflowId;
	}
	
	@Override
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	@Field(foreignKey = "nabu.misc.workflow.types.workflowTransition:id")
	@NotNull
	public UUID getTransitionId() {
		return transitionId;
	}
	public void setTransitionId(UUID transitionId) {
		this.transitionId = transitionId;
	}
	
}
