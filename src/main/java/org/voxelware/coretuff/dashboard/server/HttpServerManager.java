package org.voxelware.coretuff.dashboard.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.voxelware.coretuff.dashboard.DashboardConfig;
import org.voxelware.coretuff.dashboard.auth.AuthMiddleware;
import org.voxelware.coretuff.dashboard.auth.AuthMiddleware.AuthResult;
import org.voxelware.coretuff.dashboard.websocket.WebSocketManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class HttpServerManager {

    private static final Logger LOG = Logger.getLogger(HttpServerManager.class.getName());

    private final DashboardConfig config;
    private final AuthMiddleware authMiddleware;
    private final WebSocketManager webSocketManager;
    private final Map<String, HttpHandler> endpoints = new ConcurrentHashMap<>();

    private HttpServer server;

    public HttpServerManager(DashboardConfig config, AuthMiddleware authMiddleware, WebSocketManager webSocketManager) {
        this.config = config;
        this.authMiddleware = authMiddleware;
        this.webSocketManager = webSocketManager;
    }

    public void registerEndpoint(String path, HttpHandler handler) {
        endpoints.put(path, handler);
    }

    public void start() throws IOException {
        InetSocketAddress address = new InetSocketAddress(config.getBindAddress(), config.getPort());
        server = HttpServer.create(address, 0);
        server.setExecutor(Executors.newCachedThreadPool());

        for (var entry : endpoints.entrySet()) {
            String path = entry.getKey();
            HttpHandler handler = new CorsHandler(
                    new AuthWrappingHandler(entry.getValue(), authMiddleware, path),
                    config.getAllowedOrigins()
            );
            server.createContext(path, handler);
        }

        var wsHandler = new CorsHandler(webSocketManager.getHttpHandler(), config.getAllowedOrigins());
        server.createContext("/ws", wsHandler);

        server.start();
        LOG.info("HTTP server listening on " + config.getBindAddress() + ":" + config.getPort());
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            LOG.info("HTTP server stopped.");
        }
    }

    public int getPort() {
        return server != null ? server.getAddress().getPort() : -1;
    }

    private static final class AuthWrappingHandler implements HttpHandler {

        private final HttpHandler delegate;
        private final AuthMiddleware authMiddleware;
        private final String pathPrefix;

        AuthWrappingHandler(HttpHandler delegate, AuthMiddleware authMiddleware, String pathPrefix) {
            this.delegate = delegate;
            this.authMiddleware = authMiddleware;
            this.pathPrefix = pathPrefix;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            AuthResult auth = authMiddleware.authenticate(exchange);
            if (!auth.allowed()) {
                byte[] body = ("{\"error\":\"" + auth.errorMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
                var headers = exchange.getResponseHeaders();
                headers.add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(401, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
                return;
            }
            exchange.setAttribute("auth.claims", auth.claims());
            delegate.handle(exchange);
        }
    }

    private static final class CorsHandler implements HttpHandler {

        private final HttpHandler delegate;
        private final List<String> allowedOrigins;

        CorsHandler(HttpHandler delegate, List<String> allowedOrigins) {
            this.delegate = delegate;
            this.allowedOrigins = allowedOrigins;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var headers = exchange.getResponseHeaders();
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin != null && !origin.isBlank()) {
                if (allowedOrigins.contains("*")) {
                    headers.add("Access-Control-Allow-Origin", origin);
                } else if (allowedOrigins.contains(origin)) {
                    headers.add("Access-Control-Allow-Origin", origin);
                }
            } else {
                headers.add("Access-Control-Allow-Origin", "*");
            }
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, X-Forwarded-For");
            headers.add("Access-Control-Allow-Credentials", "true");
            headers.add("Access-Control-Max-Age", "3600");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            delegate.handle(exchange);
        }
    }
}
