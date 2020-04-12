package de.nuttercode.www.server;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class WebAppServer extends WebServer {

	private final Map<String, WebRequestHandler> handlerMap;
	private String apiPackageRoot;

	public WebAppServer(File configurationFile) {
		super(configurationFile);
		handlerMap = new HashMap<>();
		apiPackageRoot = "";
	}

	@Override
	protected void onInit() {
		super.onInit();
		apiPackageRoot = getConfiguration("app_package_root");
		if (apiPackageRoot.isEmpty())
			throw new IllegalArgumentException("app_package_root is not configured in the configuration file");
	}

	@Override
	public WebResponse handleRequest(WebRequest request) {
		WebResponse response = null;
		try {
			String uriClass = request.getReducedUri().replace("/", ".");
			WebRequestHandler handler = handlerMap.get(uriClass);
			Class<?> cl = null;
			if (handler == null) {
				boolean isApiFunction = false;
				boolean isWebRequestHandler = false;
				String classPath = apiPackageRoot + "." + uriClass;
				try {
					cl = ClassLoader.getSystemClassLoader().loadClass(classPath);
					for (Annotation a : cl.getAnnotations()) {
						if (a.annotationType().equals(WebAppComponent.class)) {
							isApiFunction = true;
							break;
						}
					}
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					if (hasLog())
						getLog().logInfo("class " + classPath + " not found");
				}
				if (cl != null) {
					if (!isApiFunction && hasLog())
						getLog().logInfo("class " + classPath + " is not an ApiFunction");
					for (Class<?> intf : cl.getInterfaces()) {
						if (intf.equals(WebRequestHandler.class)) {
							isWebRequestHandler = true;
						}
					}
					if (!isWebRequestHandler && hasLog())
						getLog().logInfo("class " + classPath + " is not a WebRequestHandler");
					handler = (WebRequestHandler) cl.getConstructor().newInstance();
					if (isApiFunction && isWebRequestHandler)
						handlerMap.put(uriClass, handler);
				}
			}
			response = handler != null ? handler.handleRequest(request) : WebResponse.from(ResponseCode.NOT_FOUND);
		} catch (Exception e) {
			if (hasLog())
				getLog().logError(e.getMessage());
		}
		return response != null ? response : WebResponse.from(ResponseCode.INTERNAL_SERVER_ERROR);
	}

}
