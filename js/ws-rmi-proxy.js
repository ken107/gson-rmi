// WebSocket RMI proxy for node.js

var net = require('net');
var url = require('url');
var WebSocketServer = require('ws').Server;

function WsAccessProxy(transport, listeningAddress) {
	var proxy = new Proxy(transport);
	this.handle = function(m) {
		proxy.handle(m);
	};
	var hostPort = listeningAddress.split(":");
	var wss = new WebSocketServer({port: hostPort[1], host: hostPort[0]});
	wss.on('connection', function(ws) {
		var auth = ws._socket.remoteAddress + ":" + ws._socket.remotePort;
		var remoteAddr = "wsa://" + auth + "?a=1";
		ws.on('message', function(text) {
			try {
				var m = JSON.parse(text);
				m.src.hops.unshift(remoteAddr);
				transport.send(m);
			}
			catch (err) {
				console.error(err.stack);
				console.error(text);
			}
		});
		var onConnectionClosed = [];
		ws.on('close', function() {
			ws = null;
			for (var i=0; i<onConnectionClosed.length; i++) {
				var failure = {message: onConnectionClosed[i]};
				transport.send(new Message(new Route(), [onConnectionClosed[i].src], MessageTypes.DELIVERY_FAILURE, failure));
			}
		});
		ws.on('error', function(err) {
			console.error(remoteAddr, err.stack || err);
		});
		proxy.addConnection(new Connection(auth, function() {
			return ws != null;
		},
		function(m) {
			if (m.contentType == MessageTypes.CHECK_CONNECTION);
			else if (m.contentType == MessageTypes.ON_CONNECTION_CLOSED) onConnectionClosed.push(m);
			else {
				var dests = [];
				for (var i=0; i<m.dests.length; i++) dests.push(new Route(m.dests[i].hops.slice(1), m.dests[i].trackingId));
				ws.send(JSON.stringify(new Message(m.src, dests, m.contentType, m.content)));
			}
		}));
	});
	this.shutdown = function() {
		wss.close();
	};
}

(function() {
	var t = new Transport();
	var wsProxy = new WsAccessProxy(t, process.argv.length == 3 ? process.argv[2] : ":8080")
	t.addHandler("wsa:", wsProxy);
	t.addHandler("tcp:", new TcpProxy(t));
	console.log("Reminder: ulimit -n 2048");
	
	process.on("SIGINT", function quit() {
		wsProxy.shutdown();
		process.removeListener("SIGINT", quit);
		console.log("CTRL-C again to quit");
	});
})();





var MessageTypes = {
	DELIVERY_FAILURE: "com.google.code.gsonrmi.transport.DeliveryFailure",
	CHECK_CONNECTION: "com.google.code.gsonrmi.transport.Proxy$CheckConnection",
	ON_CONNECTION_CLOSED: "com.google.code.gsonrmi.transport.Proxy$OnConnectionClosed"
};

function Transport() {
	var handlers = {};
	this.addHandler = function(proto, handler) {
		handlers[proto] = handler;
	};
	this.send = function(m) {
		try {
			var byProto = {};
			for (var i=0; i<m.dests.length; i++) {
				var proto = url.parse(m.dests[i].hops[0]).protocol;
				if (byProto.hasOwnProperty(proto)) byProto[proto].push(m.dests[i]);
				else byProto[proto] = [m.dests[i]];
			}
			for (var proto in byProto) {
				if (handlers.hasOwnProperty(proto)) handlers[proto].handle(new Message(m.src, byProto[proto], m.contentType, m.content));
				else console.error("No handler for protocol", proto);
			}
		}
		catch (err) {
			console.error(err.stack);
			console.error(m);
		}
	};
};

function Message(src, dests, contentType, content) {
	if (arguments.length != 4 || typeof(contentType) != 'string') throw new Error("Invalid arguments");
	this.src = src;
	this.dests = dests;
	this.contentType = contentType;
	this.content = content;
}

function Route(hops, trackingId) {
	this.hops = hops || [];
	if (trackingId) this.trackingId = trackingId;
}

function Proxy(transport, createConnectionFunc) {
	var conns = {};
	this.handle = function(m) {
		var failedRoutes = [];
		var byAuth = {};
		for (var i=0; i<m.dests.length; i++) {
			var auth = url.parse(m.dests[i].hops[0]).host;
			if (byAuth.hasOwnProperty(auth)) byAuth[auth].push(m.dests[i]);
			else byAuth[auth] = [m.dests[i]];
		}
		for (var auth in byAuth) {
			var c = conns[auth];
			if (c == null || !c.isAlive()) {
				delete conns[auth];
				c = createConnectionFunc != null ? createConnectionFunc(auth) : null;
				if (c != null) conns[auth] = c;
			}
			if (c != null) c.send(new Message(m.src, byAuth[auth], m.contentType, m.content));
			else failedRoutes = failedRoutes.concat(byAuth[auth]);
		}
		if (failedRoutes.length && m.contentType != MessageTypes.DELIVERY_FAILURE) {
			var failure = {message: new Message(m.src, failedRoutes, m.contentType, m.content)};
			transport.send(new Message(new Route(), [m.src], MessageTypes.DELIVERY_FAILURE, failure));
		}
	};
	this.addConnection = function(c) {
		conns[c.auth] = c;
	};
	setInterval(function() {
		var count = Object.keys(conns).length;
		for (var auth in conns) if (!conns[auth].isAlive()) delete conns[auth];
		var newCount = Object.keys(conns).length;
		if (newCount < count) console.log("Proxy cleanup connections " + count + " -> " + newCount);
	}, 60000);
}

function Connection(auth, isAliveFunc, sendFunc) {
	if (arguments.length != 3) throw new Error("Invalid arguments");
	this.auth = auth;
	this.send = sendFunc;
	this.isAlive = isAliveFunc;
}

function TcpProxy(transport, listeningAddress) {
	function send(socket, m) {
		var dests = [];
		for (var i=0; i<m.dests.length; i++) dests.push(new Route(m.dests[i].hops.slice(1), m.dests[i].trackingId));
		socket.write(JSON.stringify(new Message(m.src, dests, m.contentType, m.content)) + "\n");
	}
	var proxy = new Proxy(transport, function(auth) {
		var remoteAddr = "tcp://" + auth;
		var hostPort = auth.split(":");
		var queue = [];
		var socket = net.createConnection(hostPort[1], hostPort[0], function() {
			for (var i=0; i<queue.length; i++) send(socket, queue[i]);
			queue = null;
		});
		socket.on("error", function(err) {
			console.error(remoteAddr, err.stack);
			if (queue != null) for (var i=0; i<queue.length; i++) if (queue[i].contentType != MessageTypes.DELIVERY_FAILURE) {
				var failure = {message: queue[i]};
				transport.send(new Message(new Route(), [queue[i].src], MessageTypes.DELIVERY_FAILURE, failure));
			}
		});
		socket.on("close", function() {
			socket = null;
		});
		var buf = new Buffer(16384);
		var pos = 0;
		socket.on("data", function(data) {
			if (pos + data.length > buf.length) {
				var tmp = new Buffer(pos + data.length + 1024);
				buf.copy(tmp);
				buf = tmp;
			}
			data.copy(buf, pos);
			var end = pos + data.length;
			var start = 0;
			for (; pos<end; pos++) {
				if (buf[pos] == 10) {
					var text = buf.toString(null, start, pos);
					try {
						var m = JSON.parse(text);
						m.src.hops.unshift(remoteAddr);
						transport.send(m);
					}
					catch (err) {
						console.error(err.stack);
						console.error(text);
					}
					start = pos + 1;
				}
			}
			if (start > 0) {
				buf.copy(buf, 0, start, end);
				pos = end - start;
			}
		});
		return new Connection(auth, function() {
			return socket != null;
		},
		function(m) {
			if (queue != null) queue.push(m);
			else send(socket, m);
		});
	});
	this.handle = function(m) {
		proxy.handle(m);
	};
	if (listeningAddress != null) {
		var hostPort = listeningAddress.split(":");
		var server = net.createServer(function(socket) {
			socket.on("close", function() {
				socket = null;
			});
			proxy.addConnection(new Connection(listeningAddress, function() {
				return socket != null;
			},
			function(m) {
				send(socket, m);
			}));
		});
		server.listen(hostPort[1], hostPort[0]);
	}
	return proxy;
}
