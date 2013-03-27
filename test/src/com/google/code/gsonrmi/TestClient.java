package com.google.code.gsonrmi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import com.google.code.gsonrmi.Test.MySession;
import com.google.code.gsonrmi.Test.Person;
import com.google.code.gsonrmi.Test.Roster;
import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.annotations.Session;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.rmi.AbstractSession;
import com.google.code.gsonrmi.transport.rmi.Call;
import com.google.code.gsonrmi.transport.rmi.RmiService;
import com.google.code.gsonrmi.transport.tcp.TcpProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestClient {
	
	private final Gson gson;
	
	public TestClient(Gson gson) {
		this.gson = gson;
	}

	@RMI
	public void returnRoster(Roster value, RpcError error) {
		for (Person p : value.get().values()) System.out.println(p);
	}
	
	@RMI
	public void returnRoute(String marker, Route value, RpcError error) {
		System.out.println(marker + " " + value.hops);
	}
	
	@RMI
	public void returnString(String marker, String value, RpcError error) {
		System.out.println(marker + " " + value);
	}
	
	@RMI
	public void returnError(String marker, Object value, RpcError error) {
		String details = "";
		if (error.equals(RpcError.INVOCATION_EXCEPTION)) details = error.data.getValue(Exception.class, gson).toString();
		else if (error.equals(RpcError.PARAM_VALIDATION_FAILED)) details = error.data.getValue(String.class, gson);
		System.out.println(marker + " [" + error + " " + details + "]");
	}
	
	@RMI
	public void returnWithNewSession(String marker, Person value, @Session(create=true) AbstractSession session, RpcError error) {
		System.out.println(marker + " OK");
	}
	
	@RMI
	public void returnPersonWithExistingSession(String marker, Person value, @Session AbstractSession session, RpcError error) {
		System.out.println(marker + " " + value);
	}
	
	@RMI
	public void returnWithSpecifiedSession(String marker, Object value, @Session MySession session, RpcError error) {
		System.out.println(marker + " " + session.person);
	}
	
	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Exception.class, new ExceptionSerializer())
				.registerTypeAdapter(Parameter.class, new ParameterSerializer()).create();
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30101)), t, gson).start();
		new RmiService(t, gson).start();
		new Call(new Route(new URI("rmi:service")), "register", "testClient", new TestClient(gson)).send(t);
		
		//data
		Person john = new Person(1, "John", new Date(1364227080000L));
		Person jane = new Person(2, "Jane", new Date(1364226080000L));
		Person jami = new Person(3, "Jami", new Date(1364225080000L));
		Person jack = new Person(4, "Jack", new Date(1364224080000L));
		Person joey = new Person(5, "Joey", new Date(1364223080000L));
		Roster roster = new Roster();
		roster.get().put(joey.id, joey);
		
		//normal tests
		Route to = new Route(new URI("tcp://localhost:30100"), new URI("rmi:test"));
		URI from = new URI("rmi:testClient");
		new Call(to, "basic", john.id, john.name, john.birthday, jane, Arrays.asList(jami, jack), roster).callback(from, "returnRoster").send(t);
		
		//error tests
		Route unreachable = new Route(new URI("tcp://somewhere:100"), new URI("rmi:test"));
		Route badTarget = new Route(new URI("tcp://localhost:30100"), new URI("rmi:badTarget"));
		new Call(to, "throwError").callback(from, "returnError", "throwError").send(t);
		new Call(to, "throwRuntimeError").callback(from, "returnError", "throwRuntimeError").send(t);
		new Call(unreachable, "noop").callback(from, "returnError", "unreachable").send(t);
		new Call(to, "noMethod").callback(from, "returnError", "noMethod").send(t);
		new Call(badTarget, "noop").callback(from, "returnError", "badTarget").send(t);
		new Call(to, "noop", "unexpected param").callback(from, "returnError", "unexpectedParam").send(t);
		new Call(to, "echo", joey).callback(from, "returnError", "badParamType").send(t);
		new Call(to, "privateMethod").callback(from, "returnError", "privateMethod").send(t);
		new Call(to, "protectedMethod").callback(from, "returnError", "protectedMethod").send(t);
		new Call(to, "alternateName").callback(from, "returnString", "alternateName").send(t);
		new Call(to, "sourceInject").callback(from, "returnRoute", "sourceInject").send(t);
		
		//session tests
		Route toSession = new Route(new URI("tcp://localhost:30100"), new URI("rmi:test#" + UUID.randomUUID()));
		Route toBadSession = new Route(new URI("tcp://localhost:30100"), new URI("rmi:test#" + UUID.randomUUID()));
		URI fromSession = new URI("rmi:testClient#" + UUID.randomUUID());
		MySession session = new MySession();
		session.person = jack;
		new Call(to, "sessionWithCreate", joey).callback(fromSession, "returnError", "sessionWithCreate-noSessionId").send(t);
		new Call(to, "sessionWithoutCreate").callback(fromSession, "returnError", "sessionWithoutCreate-noSessionId").send(t);
		new Call(toSession, "sessionWithCreate", joey).callback(fromSession, "returnWithNewSession", "sessionWithCreate").send(t);
		new Call(toSession, "sessionWithoutCreate").callback(fromSession, "returnPersonWithExistingSession", "sessionWithoutCreate").send(t);
		new Call(toBadSession, "sessionWithoutCreate").callback(fromSession, "returnError", "sessionWithoutCreate-badSessionId").send(t);
		new Call(toSession, "sessionWithoutCreate").callback(from, "returnWithSpecifiedSession", "specifyLocalSession").session(session).send(t);
		
		//shutdown
		Thread.sleep(3000);
		new Call(to, "shutdown").send(t);
		Thread.sleep(2000);
		t.shutdown();
	}
}
