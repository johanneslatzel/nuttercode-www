# nuttercode-www

This package contains a very basic, incomplete implementation of HTTP. Use instances of the class "WebRequest" to call n HTTP server. Extend the class "WebServer" to run a HTTP server.

## Example 1: WebRequest

```java
WebRequest request = new WebRequest();
WebResponse response = request.sendTo("www.google.de");
System.out.println(response.toString());
```
Output:

```bash
HTTP/1.1 200 OK
X-Frame-Options: SAMEORIGIN
Accept-Ranges: none
Transfer-Encoding: chunked
Cache-Control: private, max-age=0
Server: gws
Set-Cookie: NID=191=oTLmkVuki_dfCKva3aJHZUsRcDzBgTQaMN9VLVNzfAcGX0UiTYSdH5bNItkVRFz7hHCm_xU27FWxNvyAshsW2LvNTLQygYo40D6mB-nrrmSJLn8h8cKEnUKrgBPNtHWIF_SZcFzewYBY5dlkJbhNTl56jZ8JJXnL_jIIMozmK14; expires=Mon, 18-May-2020 15:21:18 GMT; path=/; domain=.google.de; HttpOnly
Vary: Accept-Encoding
Expires: -1
P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info."
X-XSS-Protection: 0
Date: Sun, 17 Nov 2019 15:21:18 GMT
Content-Type: text/html; charset=ISO-8859-1
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
