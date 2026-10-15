package io.github.allannava.cragcast.testing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A local HTTP server for integration tests: each request goes to a handler that returns
 * (status, body) for its URI, and every URI asked is kept. Stands in for Open-Meteo and the
 * avalanche bulletins, so the real clients run over real HTTP.
 */
public final class FakeHttp implements AutoCloseable {

    public record Answer(int status, String body) {
        public static Answer ok(String body) {
            return new Answer(200, body);
        }

        public static final Answer NOT_FOUND = new Answer(404, "");
    }

    private final HttpServer server;
    private final List<URI> asked = Collections.synchronizedList(new ArrayList<>());

    public FakeHttp(Function<URI, Answer> handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> respond(ex, handler));
        server.start();
    }

    private void respond(HttpExchange ex, Function<URI, Answer> handler) throws IOException {
        asked.add(ex.getRequestURI());
        Answer a = handler.apply(ex.getRequestURI());
        byte[] body = a.body().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(a.status(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            ex.getResponseBody().write(body);
        }
        ex.close();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<URI> asked() {
        return List.copyOf(asked);
    }

    /** The query string of a URI as a map (values not decoded twice). */
    public static Map<String, String> query(URI uri) {
        Map<String, String> q = new LinkedHashMap<>();
        if (uri.getRawQuery() != null) {
            for (String kv : uri.getRawQuery().split("&")) {
                int i = kv.indexOf('=');
                q.put(kv.substring(0, i), java.net.URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
            }
        }
        return q;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
