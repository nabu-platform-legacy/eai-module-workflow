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

import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.api.annotation.Field;
import nabu.misc.workflow.types.WorkflowInstance.Level;

@ComplexTypeDescriptor(collectionName = "WorkflowBatchInstances",
	propOrder = { "id", "workflowId", "transitionId", "systemId", "started", "created", "stopped", "state" })
public class WorkflowBatchInstance {
	private UUID id, workflowId, transitionId;
	// the id of the batch and of the workflow that created the batch as well as the transition that created the batch
	private String systemId;
	// when the batch instance was started, when all batch children were created
	// and when the batch was stopped
	private Date started, created, stopped;
	// the state of the batch
	private Level state;
	
	@NotNull
	@Field(primary = true)
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
	
	@Field(foreignKey = "nabu.misc.workflow.types.workflowTransition:id")
	@NotNull
	public UUID getTransitionId() {
		return transitionId;
	}
	public void setTransitionId(UUID transitionId) {
		this.transitionId = transitionId;
	}
	public Date getStarted() {
		return started;
	}
	public void setStarted(Date started) {
		this.started = started;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getStopped() {
		return stopped;
	}
	public void setStopped(Date stopped) {
		this.stopped = stopped;
	}
	public Level getState() {
		return state;
	}
	public void setState(Level state) {
		this.state = state;
	}
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
}
