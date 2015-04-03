## Purpose ##

This library lets you:
  * Serialize and deserialize JSON-RPC 2.0 (http://json-rpc.org) requests and responses and
  * Invoke them on arbitrary methods of a Java object

## How to use ##
```
Gson gson = new GsonBuilder().registerTypeAdapter(...).create();
RpcRequest request = gson.fromJson(jsonText, RpcRequest.class);
Invoker invoker = new Invoker(new DefaultParamProcessor());
RpcResponse response = invoker.doInvoke(request, someObject);
jsonText = gson.toJson(response);
```

To expose a method for remote invocation, use the _@RMI_ annotation;  methods are matched by name and number of arguments.  Parameters and return values can be arbitrary Java objects.  If a parameter is a superclass type, a specific subclass can be specified with the _@ParamType_ annotation.  The _DefaultParamProcessor_ can be overridden to inject custom parameters or do additional annotation processing.

## Basic RPC Server ##

A basic RPC server implementation is provided.  This server listens for TCP connections, receives RPC requests (sent as a stream of JSON objects one after another with no delimiter), and invokes them on a pre-specified object.
```
public class SampleServer {
	@RMI
	public String someMethod(String name) {
		return "Hello, " + name;
	}

	public static void main(String[] args) throws IOException {
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();
		
		new RpcServer(30100, new RpcTarget(new SampleServer(), gson), gson).start();
	}
}
```

## Transport Stack ##

In the basic RPC server, we defined a basic transport protocol for JSON-RPC messages.  JSON-RPC itself does not specify how messages are exchanged, that is because it's only a presentation protocol (OSI layer 5).

This library also includes a generic object-to-object message transport and session management framework (OSI layer 3 and 4), with proxy and multicasting capabilities.  Currently it has a TCP implementation and a Websocket adapter.  But it can be easily extended to support any networking protocol.

The goal of the framework is to ease development of distributed applications.  It allows RMI between objects residing in many processes in a network of arbitrary topology, without having to worry about setting up and tearing down connections, serialization, deserialization, routing and addressing of RPC messages.

With this framework, once the transport layer is setup, you invoke a remote object simply by specifying the route to the object.  A route is a list of URIs identifying the hops that the message needs to traverse to reach the destination.  Following is a simple program that shows an object in one process invoking an object in another process over TCP:

### Callee ###
```
public class SampleServer {
	@RMI
	public String someMethod(String name) {
		return "Hello, " + name;
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		//setup the transport layer
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30100)), t, gson).start();
		new RmiService(t, gson).start();

		//register an object for remote invocation
		new Call(new Route(new URI("rmi:service")), "register", "herObject", new SampleServer()).send(t);
	}
}
```

### Caller ###
```
public class SampleClient {	
	@RMI
	public void returnValueMethod(String greeting, RpcError error) {
		System.out.println(greeting);
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		//setup the transport layer
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30101)), t, gson).start();
		new RmiService(t, gson).start();

		//register an object for receiving return values
		new Call(new Route(new URI("rmi:service")), "register", "myObject", new SampleClient()).send(t);

		//invoke the remote object
		new Call(new Route(new URI("tcp://localhost:30100"), new URI("rmi:herObject")), "someMethod", "Jack")
		    .callback(new URI("rmi:myObject"), "returnValueMethod")
		    .send(t);
	}
}
```

The source code for these examples can be found under the test/ folder.