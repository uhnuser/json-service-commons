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

<servlet-mapping>
	<servlet-name>json-service</servlet-name>
	<url-pattern>/json-service</url-pattern>
</servlet-mapping>
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

You can test that the service is working properly by using CURL:

```bash
curl -v -H "Content-Type: application/json" -X POST -d "{\"jsonrpc\": \"2.0\", \"method\": \"getPerson\", \"params\": {\"id\": \"123\"}}" http://localhost:8080/json-service

0/json-service
* About to connect() to localhost port 8080 (#0)
*   Trying ::1... connected
* Connected to localhost (::1) port 8080 (#0)
> POST /test-service/json-service HTTP/1.1
> User-Agent: curl/7.21.4 (universal-apple-darwin11.0) libcurl/7.21.4 OpenSSL/0.9.8r zlib/1.2.5
> Host: localhost:8080
> Accept: */*
> Content-Type: application/json
> Content-Length: 66
> 
< HTTP/1.1 200 OK
< X-Powered-By: Servlet/3.0 JSP/2.2 (GlassFish Server Open Source Edition 3.1.2 Java/Oracle Corporation/1.7)
< Server: GlassFish Server Open Source Edition 3.1.2
< Content-Type: application/json;charset=ISO-8859-1
< Transfer-Encoding: chunked
< Date: Thu, 11 Oct 2012 15:11:01 GMT
< 
{
  "jsonrpc": "2.0",
  "result": {
    "name": "John",
    "height": 180,
    "age": "Oct 11, 2012 11:11:01 AM"
  }
* Connection #0 to host localhost left intact
* Closing connection #0
}
```