# nuttercode-www

this package contains a very basic, incomplete implementation of http. use instances of the class "WebRequest" to call an http server. extend the class "WebServer" to run a http server.

## Example 1: WebRequest

```java
WebRequest request = new WebRequest();
WebResponse response = request.sendTo("www.google.de");
System.out.println(response.toString());
```

## Example 2: WebRequest with URI in constructor

```java
WebRequest request = new WebRequest("/tools/code-converter");
WebResponse response = request.sendTo("www.nuttercode.de");
System.out.println(response.toString());
```

## Example 3: WebRequest with URI parameters

The URI of the request will be constructed when sendTo is called and will be "/api&username=admin&token=mycooltoken".

```java
WebRequest request = new WebRequest("/api");
request.setUriParameter("username", "admin");
request.setUriParameter("token", "mycooltoken");
WebResponse response = request.sendTo("SOME API HOST");
System.out.println(response.toString());
```
