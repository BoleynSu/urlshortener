package su.boleyn.url_shortener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.validator.routines.UrlValidator;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.Headers;

public class URLShortenerServer {
	ReadWriteLock lock;
	URLShortener shortener;
	Undertow server;

	public URLShortenerServer(String db, int port) {
		try {
			lock = new ReentrantReadWriteLock();

			shortener = new URLShortener(db);

			server = Undertow.builder().addHttpListener(port, "localhost").setHandler(Handlers.exceptionHandler(
					Handlers.path().addPrefixPath("/", new LockHandler(lock.readLock(), new HttpHandler() {
						@Override
						public void handleRequest(final HttpServerExchange exchange) throws Exception {
							String code = exchange.getRelativePath();
							String url = shortener.getURL(code);
							if (url == null) {
								exchange.setStatusCode(404);
								exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
								exchange.getResponseSender().send("404 Not Found");
							} else {
								exchange.setStatusCode(301);
								exchange.getResponseHeaders().put(Headers.LOCATION, url);
							}
						}
					})).addPrefixPath("/history", new LockHandler(lock.readLock(), new HttpHandler() {
						@Override
						public void handleRequest(final HttpServerExchange exchange) throws Exception {
							String code = exchange.getRelativePath();
							ArrayList<URLInfo> history = shortener.getHistory(code);
							if (history == null) {
								exchange.setStatusCode(404);
								exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
								exchange.getResponseSender().send("404 Not Found");
							} else {
								exchange.setStatusCode(200);
								exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
								StringBuffer sb = new StringBuffer();
								sb.append("code: " + code + "\n\n\n");
								for (URLInfo info : history) {
									sb.append("url: " + info.url + "\n");
									sb.append("createdAt: " + info.createdAt + "\n");
									sb.append("expiresAt: " + info.expiresAt + "\n\n");
								}
								exchange.getResponseSender().send(sb.toString());
							}
						}
					})).addPrefixPath("/list", new LockHandler(lock.readLock(), new HttpHandler() {
						@Override
						public void handleRequest(final HttpServerExchange exchange) throws Exception {
							exchange.setStatusCode(200);
							exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
							Map<String, String> list = shortener.getAll();
							StringBuffer sb = new StringBuffer();
							for (Entry<String, String> entry : list.entrySet()) {
								sb.append(entry.getKey() + " -> " + entry.getValue() + "\n");
							}
							exchange.getResponseSender().send(sb.toString());
						}
					})).addPrefixPath("/create", new LockHandler(lock.writeLock(), new HttpHandler() {
						@Override
						public void handleRequest(final HttpServerExchange exchange) throws Exception {
							String code = exchange.getRelativePath();

							String url;
							if (exchange.getQueryParameters().get("url") != null
									&& exchange.getQueryParameters().get("url").size() == 1
									&& UrlValidator.getInstance()
											.isValid(exchange.getQueryParameters().get("url").getFirst())) {
								url = exchange.getQueryParameters().get("url").getFirst();
							} else {
								exchange.setStatusCode(400);
								exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
								exchange.getResponseSender().send("400 Bad Request ");
								return;
							}

							Date createdAt = new Date();
							Date expiresAt;
							if (exchange.getQueryParameters().get("month") != null) {
								Calendar c = Calendar.getInstance();
								c.setTime(createdAt);
								c.add(Calendar.MONTH, 1);
								expiresAt = c.getTime();
							} else if (exchange.getQueryParameters().get("week") != null) {
								Calendar c = Calendar.getInstance();
								c.setTime(createdAt);
								c.add(Calendar.WEEK_OF_YEAR, 1);
								expiresAt = c.getTime();
							} else if (exchange.getQueryParameters().get("day") != null) {
								Calendar c = Calendar.getInstance();
								c.setTime(createdAt);
								c.add(Calendar.DATE, 1);
								expiresAt = c.getTime();
							} else {
								if (exchange.getQueryParameters().get("expires_after") != null
										&& exchange.getQueryParameters().get("expires_after").size() == 1
										&& isInt(exchange.getQueryParameters().get("expires_after").getFirst())) {
									Calendar c = Calendar.getInstance();
									c.setTime(createdAt);
									c.add(Calendar.SECOND, Integer
											.parseInt(exchange.getQueryParameters().get("expires_after").getFirst()));
									expiresAt = c.getTime();
								}
								expiresAt = null;
							}

							URLInfo info = new URLInfo();
							info.code = code;
							info.url = url;
							info.createdAt = createdAt;
							info.expiresAt = expiresAt;

							shortener.shorten(info);

							exchange.setStatusCode(200);
							exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
							StringBuffer sb = new StringBuffer();
							sb.append("code: " + code + "\n\n\n");
							sb.append("url: " + info.url + "\n");
							sb.append("createdAt: " + info.createdAt + "\n");
							sb.append("expiresAt: " + info.expiresAt + "\n\n");
							exchange.getResponseSender().send(sb.toString());
						}
						boolean isInt(String s) {
							try {
								Integer.parseInt(s);
							} catch (Exception e) {
								return false;
							}
							return true;
						}
					}))).addExceptionHandler(Exception.class, new HttpHandler() {
						@Override
						public void handleRequest(HttpServerExchange exchange) throws Exception {
							exchange.setStatusCode(500);
							exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
							exchange.getResponseSender().send("500 Internal Server Error");
							exchange.getAttachment(ExceptionHandler.THROWABLE).printStackTrace();
						}
					})).build();
		} catch (Exception e) {
			throw new RuntimeException("failed to start the server", e);
		}
	}

	public void start() {
		server.start();
	}

	public static void main(final String[] args) {
		new URLShortenerServer(System.getProperty("url-shortener-db", "url-shortener-db"),
				Integer.parseInt(System.getProperty("url-shortener-port", "8080"))).start();
	}
}
