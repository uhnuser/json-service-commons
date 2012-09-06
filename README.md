# json-service-commons

a library for creating JSON RPC services in Java

## Installation

The library can be installed using maven:
```bash
mvn install
```

## Usage

To create a JSON RPC service simply include the library in your project. Next, you must add the packages
from which the service will try to load operations in your `web.xml`:
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
Then you simply create a class with the handler operations in the package specified above:
```java
package services.json;
import ca.uhn.ws.JsonOperation;

public class MyService {

  @JsonOperation
  public static Person getPerson(ServiceInfo serviceInfo, RequestParams params) throws Exception {
      if (null == params.id) throw new Exception("id is a required parameter");
      return new Person();
  }
}
```