package ca.uhn.ws;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ca.uhn.model.json.Error;
import ca.uhn.model.json.exception.AuthorizationErrorException;
import ca.uhn.model.json.exception.InternalErrorException;
import ca.uhn.model.json.exception.InvalidParamsException;
import ca.uhn.model.json.exception.LimitReachedException;
import ca.uhn.model.json.exception.MethodNotFoundException;
import ca.uhn.ws.model.ServiceInfo;
import ca.uhn.ws.security.ISecurityManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class JsonService extends HttpServlet {

        private static final String DATE_FORMAT = "MMM dd, yyyy hh:mm:ss a";
	private static final String JSON_SECURITY_MANAGER = "jsonSecurityManager";
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(JsonService.class);

	private static class DateTimeSerializer implements JsonSerializer<Date> {
		public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
			SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
			return new JsonPrimitive(format.format(src));
		}
	}

	private static class DateTimeDeserializer implements JsonDeserializer<Date> {
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				String date = json.getAsJsonPrimitive().getAsString();
				if (null == date || date.length() == 0) return null;
				
				SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
				Date parsedDate = format.parse(date);
				if(typeOfT == null) return parsedDate;
				if(java.sql.Date.class.equals(typeOfT.getClass())){
					return new java.sql.Date(parsedDate.getTime());	
				}else if (Timestamp.class.equals(typeOfT.getClass())){
					return new Timestamp(parsedDate.getTime());
				}else{				
					return parsedDate;	
				}
			} catch (ParseException e) {
				throw new JsonParseException(e.getMessage());
			}
		}
	}

	public static Gson gson = new GsonBuilder()
								.setPrettyPrinting()
								.registerTypeAdapter(Date.class, new DateTimeSerializer())
								.registerTypeAdapter(Date.class, new DateTimeDeserializer())
								.registerTypeAdapter(Timestamp.class, new DateTimeSerializer())
								.registerTypeAdapter(Timestamp.class, new DateTimeDeserializer())
								.registerTypeAdapter(java.sql.Date.class, new DateTimeSerializer())
								.registerTypeAdapter(java.sql.Date.class, new DateTimeDeserializer())
								.create();

	//map of request handler methods keyed by method name
	private static Map<String,Method> methods = new HashMap<String,Method>();
	private static Map<String,Class<?>> methodParams = new HashMap<String,Class<?>>();

	private ISecurityManager securityManager = null;

	/**
	 * looks up all the methods from the classes specified in jsonHandlerPackage environment variable, and puts them in a map,
	 * keyed on the method name.
	 * The handlers for incoming requests will be looked up based on the name.
	 * Method names must be public, static, unique, and must be annotated with JsonOperation.
	 */

	public void init() {
		try {
			Context env = (Context) new InitialContext().lookup("java:comp/env");
			
			List<String> jsonPackages = new ArrayList<String>();
			
			int i = 1;
			String origName = "jsonHandlerPackage";
			String name = origName;
			
			while (true) {
				String jsonHandlerPackage = null;
				try {
					log.debug("Looking up:: "+name);
					jsonHandlerPackage = (String) env.lookup(name);
				} catch (NamingException ne) {
					log.debug(ne);
				}
				if (jsonHandlerPackage != null) {
					log.debug("Found:: "+name);
					jsonPackages.add(jsonHandlerPackage);
					name = origName + i;
					i++;
				} else {
					log.debug("Not Found:: "+name);
					break;
				}
			}
			
			if (jsonPackages.isEmpty()) {
				throw new Exception("No packages defined as 'jsonHandlerPackage' in web.xml");
			}
			
			
			log.info("Instantiating security manager");

			String message = "No security manager specified, to add a security manager set jsonSecurityManager env-entry in web.xml";
			try {

				String securityManagerClassName = (String) env.lookup(JSON_SECURITY_MANAGER);
				if (securityManagerClassName == null) {
					log.warn(message);
				} else {
					try {
						securityManager = (ISecurityManager)Class.forName(securityManagerClassName).newInstance();
					} catch (Exception ex) {
						log.error("Failed to load the security manager: " + securityManagerClassName);
					}
				}
			} catch (NameNotFoundException nnfe) {
				log.warn(message);
			}


			for (String packagePath : jsonPackages) {
				log.info("searching for JSON operation handlers in package: " + packagePath);
				List<Class<?>> handlers = getClasses(packagePath);
				for (Class<?> handler : handlers) {
					log.info("found class: " + handler.getName());
					for (Method m : handler.getDeclaredMethods()){
						//only static methods with JsonOperation annotation are valid request handlers
						if (Modifier.isStatic(m.getModifiers())
							&& Modifier.isPublic(m.getModifiers())
							&& null != m.getAnnotation(JsonOperation.class)) {
								String operationName = m.getName();
								//each operation name must be unique
								if (null != methods.get(operationName)) {
									throw new Exception("Duplicate operation present: " + operationName);
								}
	
								methods.put(m.getName(), m);
								Class<?>[] params = m.getParameterTypes();
	
								if (params.length != 2) {
									throw new Exception("Operation " + m.getName() + " does not have propert arity!");
								}
								methodParams.put(m.getName(), params[1]);
	
								log.debug("found handler: " + m.getName());
						}else {
							if(null != m.getAnnotation(JsonOperation.class))
								log.warn("Found @JsonOperation method "+m.getName() + " but did not load - methods must be public and static");
						}
					} /// for
				} // for
			} // for
		} catch (Exception ex) {
			log.error("An error occurred while loading request handlers!", ex);
		}

	}

	public void destroy() {
		methods.clear();
	}

	public void doPost (HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		res.setContentType("application/json");
		String response = null;
		try {
			String json = readRequest(req.getReader());
			log.debug("got request: " + json);

			response = JsonService.handleRequest(req, securityManager, getServletContext(), json);

		} catch (Throwable ex) {
			response = generateError(ex);
		}

		PrintWriter out = res.getWriter();
		out.write(response);
		log.debug("sent response: " + response);
		out.flush();
	}

	private static String generateError(Throwable ex) {
		Throwable cause = (null == ex.getCause()) ? ex : ex.getCause();
		String error = cause.getMessage();
		error = (null == error) ? "Unknown error" : error;

		if (cause instanceof InvalidParamsException) {
			log.error(cause.getMessage());
			return gson.toJson(new Error(error, Error.INVALID_PARAMS));
		} else if (cause instanceof MethodNotFoundException) {
			log.error(cause.getMessage());
			return gson.toJson(new Error(error, Error.METHOD_NOT_FOUND));
		} else if (cause instanceof InternalErrorException) {
			log.error("Internal Error", cause);
			return gson.toJson(new Error(error, Error.INTERNAL_ERROR));
		} else if (cause instanceof AuthorizationErrorException) {
			log.error("Authorization Error", cause);
			return gson.toJson(new Error(error, Error.AUTHORIZATION_ERROR));
		} else if (cause instanceof LimitReachedException) {
			log.error("Max records limit reached", cause);
			return gson.toJson(new Error(error, Error.LIMIT_REACHED_ERROR));
		}
		else {
			log.error("Unknown error occurred while processing the request", cause);
			return gson.toJson(new Error(error, Error.FATAL));
		}
	}

	private static String handleRequest(HttpServletRequest req, ISecurityManager securityManager, ServletContext context, String jsonRequest) throws Exception {

		JsonElement jsonTree = new JsonParser().parse(jsonRequest);
		JsonObject request = jsonTree.getAsJsonObject();

		String method = gson.fromJson(request.get("method"), String.class);
		JsonObject params = request.getAsJsonObject("params");

		ServiceInfo serviceInfo = new ServiceInfo(req, context, method);

		if (null != securityManager) securityManager.authenticate(serviceInfo, method, params);
		else                         log.info("No security manager present!");

		Method m = methods.get(method);
		Class<?> paramsClass = methodParams.get(method);

		if (null == m) throw new MethodNotFoundException("No handler available for request type " + method);


		return gson.toJson(new ca.uhn.model.json.Response(m.invoke(null, new Object[]{serviceInfo, gson.fromJson(params, paramsClass)})));
	}

	private String readRequest(BufferedReader reader) throws IOException {
		StringBuffer sb = new StringBuffer();

		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}

		return sb.toString();
	}

	private static List<Class<?>> getClasses(String packageName)
    	throws ClassNotFoundException, IOException {

		if (null == packageName) throw new ClassNotFoundException("package name must be specified for JSON operations");

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}

		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return classes;
	}

	/**
	* Recursive method used to find all classes in a given directory and subdirs.
	*
	* @param directory   The base directory
	* @param packageName The package name for classes found inside the base directory
	* @return The classes
	* @throws ClassNotFoundException
	*/
	private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}
		return classes;
	}
}