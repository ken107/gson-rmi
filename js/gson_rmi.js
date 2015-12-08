
function Route() {
	this.hops = Array.prototype.slice.apply(arguments);
}

function RmiService(sendFunc, errorFunc) {
	var RPC_REQUEST = "com.google.code.gsonrmi.RpcRequest";
	var RPC_RESPONSE = "com.google.code.gsonrmi.RpcResponse";
	var DELIVERY_FAILURE = "com.google.code.gsonrmi.transport.DeliveryFailure";
	
	var handlers = {};
	this.register = function(targetId, target) {
		handlers[targetId] = {
			target: target,
			sessions: {}
		};
	};
	this.unregister = function(targetId) {
		delete handlers[targetId];
	};
	
	var pendingCalls = {};
	var idGen = 0;
	this.call = function(dest, method, args, callback, session) {
		var id = null;
		if (callback) {
			callback.lastSent = new Date().getTime();
			if (session) {
				if (typeof(session.id) == 'undefined') session.id = new Date().getTime();
				callback.target.hops[0] = callback.target.hops[0].split(/#/)[0] + "#" + session.id;
				callback.session = session;
			}
			pendingCalls[id = ++idGen] = callback;
		}
		sendFunc({
			src: callback ? callback.target : new Route("rmi:service"),
			dests: [dest],
			content: {jsonrpc: "2.0", method: method, params: args, id: id},
			contentType: RPC_REQUEST
		});
	};
	
	this.receive = function(m) {
		if (m.contentType == RPC_REQUEST) for (var i in m.dests) handleRequest(m.content, m.dests[i], m.src);
		else if (m.contentType == RPC_RESPONSE) handleResponse(m.content, m.dests[0], [m.src]);
		else if (m.contentType == DELIVERY_FAILURE) handleDeliveryFailure(m.content, m.dests[0], m.src);
		else errorFunc("Unhandled message type", m.contentType);
	};
	
	function handleRequest(request, dest, src) {
		var response = {jsonrpc: "2.0", id: request.id};
		var targetUri = dest.hops[0];
		var tokens = targetUri.split(/[:#]/);
		var handler = handlers[tokens[1]];
		if (handler) {
			if (typeof(handler.target[request.method]) != 'undefined') {
				var sessionId = tokens[2];
				var session = sessionId ? handler.sessions[sessionId] : null;
				var context = {src: src, dest: dest, session: session};
					response.result = handler.target[request.method].apply(context, request.params);
					if (context.session != session) {
						if (context.session) {
							if (sessionId) {
								context.session.id = sessionId;
								handler.sessions[sessionId] = context.session;
							}
							else {
								response.result = null;
								response.error = {code: -32001, message: "Session could not be created"};
							}
						}
						else delete handler.sessions[sessionId];
					}
			}
			else response.error = {code: -32601, message: "Method not found"};
		}
		else response.error = {code: -32010, message: "Target not found", data: targetUri};
		
		if (response.id) sendFunc({src: dest, dests: [src], content: response, contentType: RPC_RESPONSE});
		else if (response.error) errorFunc("Notification failed", targetUri, request.method, response.error);
	}
	
	function handleResponse(response, dest, srcs) {
		var callback = pendingCalls[response.id];
		if (callback) {
			var targetUri = dest.hops[0];
			var tokens = targetUri.split(/[:#]/);
			var handler = handlers[tokens[1]];
			if (handler) {
				if (typeof(handler.target[callback.method]) != 'undefined') {
					if (callback.session) handler.sessions[callback.session.id] = callback.session;
					var sessionId = tokens[2];
					var session = sessionId ? handler.sessions[sessionId] : null;
					for (var i in srcs) {
						var context = {src: srcs[i], dest: dest, session: session};
						try {
							handler.target[callback.method].apply(context, callback.args.concat([response.result, response.error]));
							if (context.session != session) {
								if (context.session) {
									if (sessionId) {
										context.session.id = sessionId;
										handler.sessions[sessionId] = context.session;
									}
									else errorFunc("Session could not be created");
								}
								else delete handler.sessions[sessionId];
							}
						}
						catch (exception) {
							errorFunc("Invoke response failed", exception);
						}
					}
				}
				else errorFunc("Callback method not found", targetUri, callback.method);
			}
			else errorFunc("Callback target not found", targetUri);
		}
		else errorFunc("No pending request", response.id);
	}
	
	function handleDeliveryFailure(failure, dest, src) {
		var m = failure.message;
		if (m.contentType == RPC_REQUEST) {
			handleResponse({
				id: m.content.id,
				error: {code: -32011, message: "Unreachable"}
			},
			dest, prependToEach(m.dests, src));
		}
		else if (m.contentType == RPC_RESPONSE) errorFunc("Failed to deliver response", m.content.id);
		else errorFunc("Unhandled delivery failure", m.contentType);
		
		function prependToEach(dests, src) {
			var out = [];
			for (var i in dests) {
				var dest = new Route();
				dest.hops = src.hops.concat(dests[i].hops);
				dest.trackingId = dests[i].trackingId;
				out.push(dest);
			}
			return out;
		}
	}
	
	setInterval(function() {
		var now = new Date().getTime();
		for (var i in pendingCalls) if (now-pendingCalls[i].lastSent > 60*1000) delete pendingCalls[i];
	},
	30*1000);
}

function Callback(target, method, args) {
	this.target = target;
	this.method = method;
	this.args = args || [];
	this.lastSent = 0;
}

if (typeof exports != "undefined") {
	exports.Callback = Callback;
	exports.RmiService = RmiService;
	exports.Route = Route;
}
