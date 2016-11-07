package be.nabu.eai.module.workflow.transition;

import java.net.URI;
import java.util.Set;

import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.CommentProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class WorkflowTransitionService implements DefinedService {

	private boolean isInitial;
	private WorkflowTransition transition;
	private Workflow workflow;
	private String id;

	private Structure input, output;
	private WorkflowState fromState;
	private String name;
	
	public WorkflowTransitionService(Workflow workflow, WorkflowState fromState, WorkflowTransition transition, boolean isInitial) {
		this.workflow = workflow;
		this.fromState = fromState;
		this.transition = transition;
		this.isInitial = isInitial;
		name = EAIRepositoryUtils.stringToField(transition.getName());
		this.id = workflow.getId() + ".services." + (isInitial ? "initial" : "transition") + "." + name;
	}
	
	@Override
	public ServiceInterface getServiceInterface() {
		return new WorkflowTransitionServiceInterface();
	}

	@Override
	public ServiceInstance newInstance() {
		return new WorkflowTransitionServiceInstance(this);
	}

	@Override
	public Set<String> getReferences() {
		return null;
	}

	@Override
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public class WorkflowTransitionServiceInterface implements ServiceInterface {
		@Override
		public ComplexType getInputDefinition() {
			if (input == null) {
				synchronized(WorkflowTransitionService.this) {
					if (input == null) {
						Structure input = new Structure();
						input.setName("input");
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
						input.add(new ComplexElementImpl("state", (ComplexType) workflow.getRepository().resolve(workflow.getId() + ".types.states." + EAIRepositoryUtils.stringToField(fromState.getName())), input));
						input.add(new ComplexElementImpl("transition", (ComplexType) workflow.getRepository().resolve(workflow.getId() + ".types.transitions." + EAIRepositoryUtils.stringToField(transition.getName())), input));
						WorkflowTransitionService.this.input = input;
					}
				}
			}
			return input;
		}

		@Override
		public ComplexType getOutputDefinition() {
			if (output == null) {
				synchronized(WorkflowTransitionService.this) {
					if (output == null) {
						Structure output = new Structure();
						output.setName("output");
						if (isInitial) {
							output.add(new SimpleElementImpl<String>("workflowId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output));
						}
						WorkflowTransitionService.this.output = output;
					}
				}
			}
			return output;
		}

		@Override
		public ServiceInterface getParent() {
			return null;
		}
	}

	public boolean isInitial() {
		return isInitial;
	}

	public WorkflowTransition getTransition() {
		return transition;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public WorkflowState getFromState() {
		return fromState;
	}
	
}
