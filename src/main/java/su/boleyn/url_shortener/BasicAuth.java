package su.boleyn.url_shortener;

import java.util.Collections;
import java.util.List;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

class BasicAuth implements HttpHandler {
	private HttpHandler next;
	private IdentityManager identityManager;

	public BasicAuth(HttpHandler next, IdentityManager identityManager) {
		this.next = next;
		this.identityManager = identityManager;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		HttpHandler handler = next;
		handler = new AuthenticationCallHandler(handler);
		handler = new AuthenticationConstraintHandler(handler);
		final List<AuthenticationMechanism> mechanisms = Collections
				.<AuthenticationMechanism>singletonList(new BasicAuthenticationMechanism(exchange.getHostName()));
		handler = new AuthenticationMechanismsHandler(handler, mechanisms);
		handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
		handler.handleRequest(exchange);
	}
}