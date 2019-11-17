# nuttercode-www

This package contains a very basic, incomplete implementation of HTTP. Use instances of the class "WebRequest" to call n HTTP server. Extend the class "WebServer" to run a HTTP server.

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
WebResponse response = request.sendTo("SOME API HOST");
System.out.println(response.toString());
```

## Example 4: WebRequest with POST request method and custom body

```java
WebRequest request = new WebRequest("/api");
request.setBody("{\"username\": \"admin\", \"content\": \"admins are cool\"}");
request.setMethod(RequestMethod.POST);
WebResponse response = request.sendTo("www.nuttercode.de");
System.out.println(response.toString());
```

## Example 5: Run a WebServer instance

Let "CoolServer" be a non-abstract class which extends "WebServer".

```java
CoolServer coolServer = new CoolServer();
WebServer.run(coolServer);
```
