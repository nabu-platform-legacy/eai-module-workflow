package be.nabu.eai.module.workflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationUtils;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionService;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.impl.ResponseMethods;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.api.UnmarshallableBinding;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.form.FormBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiableHeader;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class WorkflowRESTListener implements EventHandler<HTTPRequest, HTTPResponse> {

	private Workflow workflow;
	private Charset charset;
	private WebApplication application;
	private WorkflowTransitionService service;

	public WorkflowRESTListener(WebApplication application, Workflow workflow, Charset charset, WorkflowTransitionService service) {
		this.application = application;
		this.workflow = workflow;
		this.charset = charset;
		this.service = service;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
		try {
			URI uri = HTTPUtils.getURI(request, false);
			String path = URIUtils.normalize(uri.getPath());
			
			// the last part is the transition
			String transition = path.replaceAll("^.*/", "");
			
			WorkflowTransitionService service;
			if (this.service != null) {
				if (this.service.isInitial() && !"POST".equalsIgnoreCase(request.getMethod())) {
					throw new HTTPException(404, "Only post allowed for create");
				}
				else if (!this.service.isInitial() && !"PUT".equalsIgnoreCase(request.getMethod())) {
					throw new HTTPException(404, "Only put allowed for update");
				}
				else {
					service = this.service;
				}
			}
			else if ("POST".equalsIgnoreCase(request.getMethod())) {
				service = (WorkflowTransitionService) workflow.getRepository().resolve(workflow.getId() + ".services.initial." + transition);	
			}
			else if ("PUT".equalsIgnoreCase(request.getMethod())) {
				service = (WorkflowTransitionService) workflow.getRepository().resolve(workflow.getId() + ".services.transition." + transition);
			}
			else {
				throw new HTTPException(404, "Only post and put are supported");
			}
			
			if (service == null) {
				throw new HTTPException(404, "Could not find transition: " + transition);
			}

			return process(application, charset, request, service);
		}
		catch (Exception e) {
			throw new HTTPException(500, e);
		}
		finally {
			ServiceRuntime.setGlobalContext(null);
		}
	}

	public static HTTPResponse process(WebApplication application, Charset charset, HTTPRequest request, WorkflowTransitionService service) throws IOException, ParseException, ServiceException {
		Token token = WebApplicationUtils.getToken(application, request);
		Device device = WebApplicationUtils.getDevice(application, request, token);
		WebApplicationUtils.checkRole(application, token, service.getTransition().getRoles());
		HTTPResponse checkRateLimits = WebApplicationUtils.checkRateLimits(application, token, device, service.getId(), null, request);
		if (checkRateLimits != null) {
			return checkRateLimits;
		}
		
		ComplexContent input = null;
		if (request.getContent() instanceof ContentPart) {
			ReadableContainer<ByteBuffer> readable = ((ContentPart) request.getContent()).getReadable();
			if (readable != null) {
				try {
					String contentType = MimeUtils.getContentType(request.getContent().getHeaders());
					UnmarshallableBinding binding;
					if (contentType == null) {
						throw new HTTPException(415, "Unknown request content type", token);
					}
					else if (contentType.equalsIgnoreCase("application/xml") || contentType.equalsIgnoreCase("text/xml")) {
						binding = new XMLBinding(service.getServiceInterface().getInputDefinition(), charset);
					}
					else if (contentType.equalsIgnoreCase("application/json") || contentType.equalsIgnoreCase("application/javascript")) {
						binding = new JSONBinding(service.getServiceInterface().getInputDefinition(), charset);
					}
					else if (contentType.equalsIgnoreCase("application/x-www-form-urlencoded")) {
						binding = new FormBinding(service.getServiceInterface().getInputDefinition(), charset);
					}
					// no binding provider support for now because it is in the rest library
					else {
						throw new HTTPException(415, "Unsupported content type: " + contentType, token);
					}
					input = binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]);
				}
				finally {
					readable.close();
				}
			}
		}
		
		ServiceRuntime runtime = new ServiceRuntime(service, application.getRepository().newExecutionContext(token));
		runtime.getContext().put("session", WebApplicationUtils.getSession(application, request));
		runtime.getContext().put("device", device);
		ServiceUtils.setServiceContext(runtime, service.getId());

		ComplexContent output = runtime.run(input);

		List<Header> headers = new ArrayList<Header>();
		if (device != null && WebApplicationUtils.isNewDevice(application, request)) {
			ModifiableHeader cookieHeader = HTTPUtils.newSetCookieHeader(
				"Device-" + application.getRealm(), 
				device.getDeviceId(),
				new Date(new Date().getTime() + 1000l*60*60*24*365*100),
				application.getCookiePath(),
				// domain
				null, 
				// secure TODO?
				false,
				// http only
				true
			);
			headers.add(cookieHeader);
		}
		
		if (output == null) {
			return HTTPUtils.newEmptyResponse(request, headers.toArray(new Header[headers.size()]));
		}
		else {
			MarshallableBinding binding;
			List<String> acceptedContentTypes = request.getContent() != null
					? MimeUtils.getAcceptedContentTypes(request.getContent().getHeaders())
					: new ArrayList<String>();
			acceptedContentTypes.retainAll(ResponseMethods.allowedTypes);
			String contentType = acceptedContentTypes.isEmpty() ? "application/json" : acceptedContentTypes.get(0);
			if (contentType.equalsIgnoreCase("application/xml") || contentType.equalsIgnoreCase("text/xml")) {
				binding = new XMLBinding(output.getType(), charset);
			}
			else if (contentType.equalsIgnoreCase("application/json") || contentType.equalsIgnoreCase("application/javascript")) {
				binding = new JSONBinding(output.getType(), charset);
			}
			else if (contentType.equalsIgnoreCase("application/x-www-form-urlencoded")) {
				binding = new FormBinding(output.getType(), charset);
			}
			else {
				throw new HTTPException(400, "Unsupported response content type: " + contentType, token);
			}
			
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			binding.marshal(content, (ComplexContent) output);
			byte[] byteArray = content.toByteArray();
			headers.add(new MimeHeader("Content-Length", "" + byteArray.length));
			headers.add(new MimeHeader("Content-Type", contentType + "; charset=" + charset.name()));
			PlainMimeContentPart part = new PlainMimeContentPart(null,
				IOUtils.wrap(byteArray, true),
				headers.toArray(new Header[headers.size()])
			);
			if (!EAIResourceRepository.isDevelopment()) {
				HTTPUtils.setContentEncoding(part, request.getContent().getHeaders());
			}
			return new DefaultHTTPResponse(request, 200, HTTPCodes.getMessage(200), part);
		}
	}
	

}