package be.nabu.eai.module.workflow.transition;

import java.net.URI;

import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.CommentProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class WorkflowTransitionServiceInterface implements DefinedServiceInterface {
		
		private boolean isInitial;
		private Workflow workflow;
		private WorkflowState fromState;
		private WorkflowTransition transition;
		private Repository repository;
		
		public WorkflowTransitionServiceInterface(Workflow workflow, WorkflowState fromState, WorkflowTransition transition) {
			this.workflow = workflow;
			this.fromState = fromState;
			this.transition = transition;
			this.isInitial = workflow.getInitialStates().contains(fromState);
		}
		
		private Structure input, output;
		@Override
		public ComplexType getInputDefinition() {
			if (input == null) {
				synchronized(this) {
					if (input == null) {
						Structure input = new Structure();
						input.setName("input");
						input.add(new SimpleElementImpl<String>("connectionId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
						if (!isInitial) {
							input.add(new SimpleElementImpl<String>("workflowId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input));
						}
						else {
							input.add(new SimpleElementImpl<String>("parentId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The id of the parent workflow")));
							input.add(new SimpleElementImpl<String>("batchId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The id of the batch it belongs to")));
							input.add(new SimpleElementImpl<String>("correlationId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "A free to choose correlation id that can link this workflow to something else")));
							input.add(new SimpleElementImpl<String>("contextId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "A contextually relevant id for this instance of the workflow, for example an invoice number")));
							input.add(new SimpleElementImpl<String>("groupId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The contextually relevant group id for this instance of the workflow, for example a customer id")));
							input.add(new SimpleElementImpl<String>("workflowType", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The contextually relevant type of this workflow instance, for example a message type")));
							input.add(new SimpleElementImpl<URI>("uri", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(URI.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "A lot of workflows revolve around data, if this is the case, log the uri for the relevant data here")));
						}
						input.add(new ComplexElementImpl("state", (ComplexType) getRepository().resolve(workflow.getId() + ".types.states." + EAIRepositoryUtils.stringToField(fromState.getName())), input));
						input.add(new ComplexElementImpl("transition", (ComplexType) getRepository().resolve(workflow.getId() + ".types.transitions." + EAIRepositoryUtils.stringToField(transition.getName())), input));
//						input.add(new SimpleElementImpl<Boolean>("asynchronous", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Boolean.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "Whether or not the execution should be done asynchronously")));
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
						if (isInitial) {
							output.add(new SimpleElementImpl<String>("workflowId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output));
						}
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

		public boolean isInitial() {
			return isInitial;
		}

		public Workflow getWorkflow() {
			return workflow;
		}

		public WorkflowState getFromState() {
			return fromState;
		}

		public WorkflowTransition getTransition() {
			return transition;
		}

		@Override
		public String getId() {
			return workflow.getId() + ".interfaces." + (isInitial ? "initial" : "transition") + "." + EAIRepositoryUtils.stringToField(transition.getName());
		}

		public Repository getRepository() {
			return repository == null ? workflow.getRepository() : repository;
		}

		public void setRepository(Repository repository) {
			this.repository = repository;
		}
	}