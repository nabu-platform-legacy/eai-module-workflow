package be.nabu.eai.module.workflow;

import java.util.Set;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;

public class Workflow extends JAXBArtifact<WorkflowConfiguration> implements DefinedService {

	public Workflow(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "workflow.xml", WorkflowConfiguration.class);
	}

	@Override
	public ServiceInterface getServiceInterface() {
		// the input has a document containing one input document per initial transition possible
		// this starts a new workflow
		// also add a batch id to link multiple workflows started in a batch
		// context id is free to choose, if not filled in, it should be the id of the workflow
		// environment is autofilled
		// TODO Auto-generated method stub
		return null;
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

}
