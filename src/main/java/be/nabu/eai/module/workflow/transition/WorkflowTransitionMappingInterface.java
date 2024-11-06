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

package be.nabu.eai.module.workflow.transition;

import java.net.URI;

import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowTransitionInstance;
import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class WorkflowTransitionMappingInterface implements DefinedServiceInterface {

	private Structure input, output;
	private Workflow workflow;
	private WorkflowState fromState, toState;
	private WorkflowTransition transition;
	private boolean isInitial;

	public WorkflowTransitionMappingInterface(Workflow workflow, WorkflowState fromState, WorkflowTransition transition) {
		this.workflow = workflow;
		this.fromState = fromState;
		this.transition = transition;
		this.isInitial = workflow.getInitialStates().contains(fromState);
		this.toState = transition.getTargetStateId() == null ? null : workflow.getStateById(transition.getTargetStateId());
	}
	
	@Override
	public ComplexType getInputDefinition() {
		if (input == null) {
			synchronized(this) {
				if (input == null) {
					Structure input = new Structure();
					input.setName("input");
					input.add(new SimpleElementImpl<String>("connectionId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					input.add(new ComplexElementImpl("workflow", (ComplexType) BeanResolver.getInstance().resolve(WorkflowInstance.class), input));
					input.add(new ComplexElementImpl("properties", workflow.getStructures().get("properties"), input));
					input.add(new ComplexElementImpl("state", workflow.getStructures().get(Workflow.stringify(fromState.getId())), input));
					input.add(new ComplexElementImpl("transition", workflow.getStructures().get(Workflow.stringify(transition.getId())), input));
					input.add(new ComplexElementImpl("history", (ComplexType) BeanResolver.getInstance().resolve(WorkflowTransitionInstance.class), input, new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0), new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					this.input = input;
				}
			}
		}
		return input;
	}

	@Override
	public ComplexType getOutputDefinition() {
		if (output == null) {
			synchronized(this) {
				if (output == null) {
					Structure output = new Structure();
					output.setName("output");
					output.add(new ComplexElementImpl("properties", workflow.getStructures().get("properties"), output));
					output.add(new ComplexElementImpl("state", workflow.getStructures().get(Workflow.stringify(toState.getId())), output));
					output.add(new SimpleElementImpl<String>("correlationId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					output.add(new SimpleElementImpl<String>("contextId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					output.add(new SimpleElementImpl<String>("groupId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					output.add(new SimpleElementImpl<String>("workflowType", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					output.add(new SimpleElementImpl<String>("log", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					output.add(new SimpleElementImpl<String>("code", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					output.add(new SimpleElementImpl<URI>("uri", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(URI.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					this.output = output;
				}
			}
		}
		return output;
	}

	@Override
	public ServiceInterface getParent() {
		return null;
	}

	@Override
	public String getId() {
		return workflow.getId() + ".interfaces.mappings.transition_" + Workflow.stringify(transition.getId());
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public WorkflowState getFromState() {
		return fromState;
	}

	public WorkflowState getToState() {
		return toState;
	}

	public WorkflowTransition getTransition() {
		return transition;
	}

	public boolean isInitial() {
		return isInitial;
	}

}
