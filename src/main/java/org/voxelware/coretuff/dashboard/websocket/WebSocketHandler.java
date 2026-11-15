package org.voxelware.coretuff.dashboard.websocket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.voxelware.coretuff.dashboard.auth.JwtProvider;
import org.voxelware.coretuff.dashboard.auth.JwtProvider.JwtClaims;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WebSocketHandler implements HttpHandler {

    private static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtProvider jwtProvider;
    private final SettingsRegistry settingsRegistry;
    private final List<WebSocketClient> clients = new CopyOnWriteArrayList<>();

    public WebSocketHandler(JwtProvider jwtProvider, SettingsRegistry settingsRegistry) {
        this.jwtProvider = jwtProvider;
        this.settingsRegistry = settingsRegistry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
        if (!"websocket".equalsIgnoreCase(upgrade)) {
            exchange.sendResponseHeaders(426, -1);
            return;
        }

        String token = extractToken(exchange);
        if (token == null) {
            sendForbidden(exchange);
            return;
        }

        JwtClaims claims = jwtProvider.verify(token);
        if (claims == null) {
            sendForbidden(exchange);
            return;
        }

        String key = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
        if (key == null || key.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String acceptKey;
        try {
            acceptKey = generateAcceptKey(key);
        } catch (NoSuchAlgorithmException e) {
            exchange.sendResponseHeaders(500, -1);
            return;
        }

        var headers = exchange.getResponseHeaders();
        headers.add("Upgrade", "websocket");
        headers.add("Connection", "Upgrade");
        headers.add("Sec-WebSocket-Accept", acceptKey);
        headers.add("Sec-WebSocket-Protocol", "json");

        try {
            exchange.sendResponseHeaders(101, -1);
        } catch (IOException e) {
            return;
        }

        Socket socket = getSocket(exchange);
        if (socket == null) return;

        var client = new WebSocketClient(socket, claims.name(), claims.serverUuid());
        clients.add(client);

        try {
            client.readLoop();
        } finally {
            clients.remove(client);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public List<WebSocketClient> getClients() {
        return List.copyOf(clients);
    }

    public int getClientCount() {
        return clients.size();
    }

    public void broadcast(String message) {
        for (var client : clients) {
            try {
                client.send(message);
            } catch (IOException e) {
                clients.remove(client);
            }
        }
    }

    private String extractToken(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2 && "token".equalsIgnoreCase(parts[0])) {
                    return java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }
        }
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private void sendForbidden(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(403, -1);
    }

    private Socket getSocket(HttpExchange exchange) {
        try {
            var field = exchange.getClass().getDeclaredField("connection");
            field.setAccessible(true);
            Object connection = field.get(exchange);
            var socketField = connection.getClass().getDeclaredField("socket");
            socketField.setAccessible(true);
            return (Socket) socketField.get(connection);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateAcceptKey(String key) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(key.getBytes(StandardCharsets.UTF_8));
        sha1.update(WS_MAGIC.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sha1.digest());
    }
}
