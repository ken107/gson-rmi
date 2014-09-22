package com.google.code.gsonrmi.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.annotations.ParamType;
import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.annotations.Session;
import com.google.code.gsonrmi.annotations.Src;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.code.gsonrmi.transport.rmi.AbstractSession;
import com.google.code.gsonrmi.transport.rmi.Call;
import com.google.code.gsonrmi.transport.rmi.RmiService;
import com.google.code.gsonrmi.transport.tcp.TcpProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestServer {
	
	private final Transport t;
	
	public TestServer(Transport t) {
		this.t = t;
	}
	
	@RMI
	public void noop() {
	}
	
	@RMI
	public String echo(String s) {
		return s;
	}
	
	@RMI
	public Mapper<Person> basic(int id, String name, Date birthday, Person person, List<Person> people, @ParamType(Roster.class) Mapper<Person> roster) {
		roster.get().put(id, new Person(id, name, birthday));
		roster.get().put(person.id, person);
		for (Person p : people) roster.get().put(p.id, p);
		return roster;
	}
	
	@RMI
	public void throwError() throws Exception {
		throw new InterruptedException("interrupted");
	}
	
	@RMI
	public void throwRuntimeError() {
		int[] a = new int[0];
		a[0] = 1;
	}
	
	@RMI
	private void privateMethod() {
	}
	
	@RMI
	protected void protectedMethod() {
	}
	
	@RMI(value="alternateName")
	public String realName() {
		return "OK";
	}
	
	@RMI
	public Route sourceInject(@Src Route from) {
		return from;
	}
	
	@RMI
	public void sessionWithCreate(@Session(create=true) MySession session, Person person) {
		session.person = person;
	}
	
	@RMI
	public Person sessionWithoutCreate(@Session MySession session) {
		return session.person;
	}
	
	@RMI
	public void shutdown() {
		t.shutdown();
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Exception.class, new ExceptionSerializer())
				.registerTypeAdapter(Parameter.class, new ParameterSerializer()).create();
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30100)), t, gson).start();
		new RmiService(t, gson).start();
		new Call(new Route(new URI("rmi:service")), "register", "test", new TestServer(t)).send(t);
	}
	
	public static class Person {
		public int id;
		public String name;
		public Date birthday;
		public Person(int id, String name, Date birthday) {
			this.id = id;
			this.name = name;
			this.birthday = birthday;
		}
		@Override
		public String toString() {
			return "[" + id + " " + name + " " + birthday.getTime() + "]";
		}
	}
	
	public static interface Mapper<T> {
		Map<Integer, T> get();
	}
	
	public static class Roster implements Mapper<Person> {
		private Map<Integer, Person> m = new HashMap<Integer, Person>();
		@Override
		public Map<Integer, Person> get() {
			return m;
		}
	}
	
	public static class MySession extends AbstractSession {
		public Person person;
	}
}
