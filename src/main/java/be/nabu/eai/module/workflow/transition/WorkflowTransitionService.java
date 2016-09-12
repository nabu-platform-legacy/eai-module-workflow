package be.nabu.eai.module.workflow.transition;

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
		// TODO Auto-generated method stub
		return null;
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
}
