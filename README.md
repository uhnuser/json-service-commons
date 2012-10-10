# json-service-commons

a library for creating JSON RPC services in Java

## Installation

The library can be installed using maven:
```bash
mvn install
```

## Usage

To create a JSON RPC service simply include the library in your project. Then set the servlet to the following in `web.xml`:
```xml
<servlet>
	<servlet-name>json-service</servlet-name>
	<servlet-class>ca.uhn.ws.JsonService</servlet-class>
	<load-on-startup>1</load-on-startup>
</servlet>
```

Next, you must add the packages
from which the service will try to load operations:
```xml
<env-entry>
  	<env-entry-name>jsonHandlerPackage</env-entry-name>
		<env-entry-type>java.lang.String</env-entry-type>
		<env-entry-value>services.json</env-entry-value>
</env-entry>
```
Optionally, you can also specify a security manager, any request will be passed to it before being handled:
```xml
<env-entry>
  	<env-entry-name>jsonSecurityManager</env-entry-name>
		<env-entry-type>java.lang.String</env-entry-type>
		<env-entry-value>security.SecurityManager</env-entry-value>
	</env-entry>  
```
The security manager must implement the `ca.uhn.ws.security.ISecurityManager` interface, which has the following signature:
```java
public void authenticate(ServiceInfo serviceInfo, String method, JsonObject params) throws Exception;
```
If authentication fails then `authenticate` should throw an exception and the service will return the error to the client.

Now we'll create our bean and parameters classes
```java
import ca.uhn.model.json.BaseRequestParams;

public class RequestParams extends BaseRequestParams {
  public String id;  
}

import java.util.Date;

public class Person {  
  public String name;
  public Integer height;
  public Date age;
}
```

**note** if you are writing a cusomt client please ensure that the dates are in the following format "MMM dd, yyyy hh:mm:ss a".

Then you simply create a class with the handler operations in the package specified above:
```java
package services.json;
import ca.uhn.ws.JsonOperation;

public class MyService {

  @JsonOperation
  public static Person getPerson(ServiceInfo serviceInfo, RequestParams params) throws Exception {
      if (null == params.id) throw new Exception("id is a required parameter");
      Person person = new Person();
      person.name = "John Doe";
      person.height = 180;
      person.age = new java.util.Date();
      return person;
  }
}
```
All JSON operations must use the `@JsonOperation` annotation and must be static. The operation must accept `ServiceInfo` 
and a class which extends `BaseRequestParams` as parameters. ServiceInfo provides access to the following methods:
```java
HttpServletRequest getRequest()
ServletContext getContext()
String getOperationName()
String getHostname()
```
