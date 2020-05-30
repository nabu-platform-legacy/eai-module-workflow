package be.nabu.eai.module.workflow.transition;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.application.api.RESTFragment;
import be.nabu.eai.module.workflow.Workflow;
import be.nabu.eai.module.workflow.WorkflowRESTListener;
import be.nabu.eai.module.workflow.WorkflowState;
import be.nabu.eai.module.workflow.WorkflowTransition;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.RestrictProperty;
import be.nabu.libs.types.structure.Structure;

public class WorkflowTransitionService implements DefinedService, WebFragment, RESTFragment {

	private String id;

	private String name;
	private WorkflowTransitionServiceInterface serviceInterface;
	
	public WorkflowTransitionService(WorkflowTransitionServiceInterface iface) {
		this.serviceInterface = iface;
		name = EAIRepositoryUtils.stringToField(iface.getTransition().getName());
		this.id = iface.getWorkflow().getId() + ".services." + (iface.isInitial() ? "initial" : "transition") + "." + name;
	}
	
	@Override
	public ServiceInterface getServiceInterface() {
		return serviceInterface;
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

	public boolean isInitial() {
		return serviceInterface.isInitial();
	}

	public WorkflowTransition getTransition() {
		return serviceInterface.getTransition();
	}

	public Workflow getWorkflow() {
		return serviceInterface.getWorkflow();
	}

	public WorkflowState getFromState() {
		return serviceInterface.getFromState();
	}


	private Map<String, EventSubscription<?, ?>> subscriptions = new HashMap<String, EventSubscription<?, ?>>();

	private String getKey(WebApplication artifact, String path) {
		return artifact.getId() + ":" + path;
	}
	
	@Override
	public void start(WebApplication artifact, String path) throws IOException {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			stop(artifact, path);
		}
		String restPath = artifact.getServerPath();
		if (path != null && !path.isEmpty() && !path.equals("/")) {
			if (!restPath.endsWith("/")) {
				restPath += "/";
			}
			restPath += path.replaceFirst("^[/]+", "");
		}
		synchronized(subscriptions) {
			WorkflowRESTListener listener = new WorkflowRESTListener(
				artifact,
				serviceInterface.getWorkflow(), 
				Charset.defaultCharset(),
				this,
				restPath
			);
			EventSubscription<HTTPRequest, HTTPResponse> subscription = artifact.getDispatcher().subscribe(HTTPRequest.class, listener);
			subscription.filter(HTTPServerUtils.limitToPath(restPath));
			subscriptions.put(key, subscription);
		}
	}

	@Override
	public void stop(WebApplication artifact, String path) {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			synchronized(subscriptions) {
				if (subscriptions.containsKey(key)) {
					subscriptions.get(key).unsubscribe();
					subscriptions.remove(key);
				}
			}
		}
	}

	@Override
	public List<Permission> getPermissions(WebApplication artifact, String path) {
		return new ArrayList<Permission>();
	}

	@Override
	public boolean isStarted(WebApplication artifact, String path) {
		return subscriptions.containsKey(getKey(artifact, path));
	}

	@Override
	public String getPath() {
		String cleanName = EAIRepositoryUtils.stringToField(getName());
		String basePath = getWorkflow().getConfig().getBasePath();
		if (basePath == null) {
			basePath = getWorkflow().getId();
		}
		else if (basePath.equals("/")) {
			basePath = null;
		}
		return ("/" + (basePath == null ? "" : basePath + "/") + cleanName).replaceAll("[/]{2,}", "/");
	}

	@Override
	public String getMethod() {
		return isInitial() ? "POST" : "PUT";
	}

	@Override
	public List<String> getConsumes() {
		return Arrays.asList("application/json", "application/xml");
	}

	@Override
	public List<String> getProduces() {
		return Arrays.asList("application/json", "application/xml");
	}

	@Override
	public Type getInput() {
		Structure structure = new Structure();
		structure.setName("input");
		structure.setSuperType(getServiceInterface().getInputDefinition());
		structure.setProperty(new ValueImpl<String>(RestrictProperty.getInstance(), "connectionId,force,bestEffort"));
		return structure;
	}

	@Override
	public Type getOutput() {
		return getServiceInterface().getOutputDefinition();
	}

	@Override
	public List<Element<?>> getQueryParameters() {
		return new ArrayList<Element<?>>();
	}

	@Override
	public List<Element<?>> getHeaderParameters() {
		return new ArrayList<Element<?>>();
	}

	@Override
	public List<Element<?>> getPathParameters() {
		return new ArrayList<Element<?>>();
	}
	
}
