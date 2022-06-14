package be.nabu.eai.module.workflow.transition;

import java.net.URI;

import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.Scope;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.CommentProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.ScopeProperty;
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
			this.isInitial = workflow.getInitialStates().contains(fromState) && !workflow.isExtensionState(fromState.getId());
		}
		
		private Structure input, output;
		@Override
		public ComplexType getInputDefinition() {
			if (input == null) {
				synchronized(this) {
					if (input == null) {
						Structure input = new Structure();
						input.setName("input");
						input.add(new SimpleElementImpl<String>("connectionId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0),
							new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
						if (!isInitial) {
							input.add(new SimpleElementImpl<String>("workflowId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input));
							// a global state can be called from anywhere, force and besteffort have no power here
							if (!fromState.isGlobalState()) {
								input.add(new SimpleElementImpl<Boolean>("force", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Boolean.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "If the workflow is not in the correct state, do you still want to trigger this transition? This is especially interesting for forcing a retry at a specific state."),
									new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
								input.add(new SimpleElementImpl<Boolean>("bestEffort", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Boolean.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "If the workflow is not in the correct state, do you want an exception or just leave it? This is especially interesting for timed transitions that are used as a fallback."),
									new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
							}
						}
						else {
							input.add(new SimpleElementImpl<String>("workflowId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "A pregenerated id for the workflow"),
								new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
							input.add(new SimpleElementImpl<String>("parentId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The id of the parent workflow"),
								new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
							input.add(new SimpleElementImpl<String>("batchId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The id of the batch it belongs to"),
								new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
							input.add(new SimpleElementImpl<String>("correlationId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "A free to choose correlation id that can link this workflow to something else")));
							input.add(new SimpleElementImpl<String>("contextId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "A contextually relevant id for this instance of the workflow, for example an invoice number")));
							input.add(new SimpleElementImpl<String>("groupId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The contextually relevant group id for this instance of the workflow, for example a customer id")));
							input.add(new SimpleElementImpl<String>("workflowType", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "The contextually relevant type of this workflow instance, for example a message type")));
							input.add(new SimpleElementImpl<URI>("uri", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(URI.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<String>(CommentProperty.getInstance(), "A lot of workflows revolve around data, if this is the case, log the uri for the relevant data here"),
								new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
						}
						ComplexType stateType = (ComplexType) getRepository().resolve(workflow.getId() + ".types.states." + EAIRepositoryUtils.stringToField(fromState.getName()));
						if (stateType != null && !TypeUtils.getAllChildren(stateType).isEmpty()) {
							input.add(new ComplexElementImpl("state", (ComplexType) stateType, input));
						}
						ComplexType transitionType = (ComplexType) getRepository().resolve(workflow.getId() + ".types.transitions." + EAIRepositoryUtils.stringToField(transition.getName()));
						if (transitionType != null && !TypeUtils.getAllChildren(transitionType).isEmpty()) {
							ComplexElementImpl element = new ComplexElementImpl("transition", transitionType, input);
							input.add(element);
							// if the transition has no mandatory fields, set it to optional
							boolean optional = true;
							for (Element<?> child : TypeUtils.getAllChildren(element.getType())) {
								Integer minOccurs = ValueUtils.getValue(MinOccursProperty.getInstance(), child.getProperties());
								if (minOccurs == null || minOccurs != 0) {
									optional = false;
									break;
								}
							}
							if (optional) {
								element.setProperty(new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0));
							}
						}
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