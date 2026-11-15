package org.voxelware.coretuff.dashboard.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.logging.Logger;

public final class WebSocketClient {

    private static final Logger LOG = Logger.getLogger(WebSocketClient.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OPCODE_TEXT = 0x1;
    private static final int OPCODE_CLOSE = 0x8;
    private static final int OPCODE_PING = 0x9;
    private static final int OPCODE_PONG = 0xA;

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final String clientName;
    private final String clientUuid;
    private final String sessionId;
    private volatile boolean connected;

    public WebSocketClient(Socket socket, String clientName, String clientUuid) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(30000);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.clientName = clientName;
        this.clientUuid = clientUuid;
        this.sessionId = UUID.randomUUID().toString();
        this.connected = true;
    }

    public String getClientName() { return clientName; }
    public String getClientUuid() { return clientUuid; }
    public String getSessionId() { return sessionId; }
    public boolean isConnected() { return connected; }

    public void send(String message) throws IOException {
        if (!connected) throw new IOException("Client not connected");
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        sendFrame(OPCODE_TEXT, payload);
    }

    public void sendPong() throws IOException {
        sendFrame(OPCODE_PONG, new byte[0]);
    }

    public void close() {
        connected = false;
        try {
            sendFrame(OPCODE_CLOSE, ByteBuffer.allocate(2).putShort((short) 1000).array());
        } catch (IOException ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
    }

    public void readLoop() {
        try {
            while (connected && !socket.isClosed()) {
                try {
                    int firstByte = in.read();
                    if (firstByte == -1) break;
                    int secondByte = in.read();
                    if (secondByte == -1) break;

                    int opcode = firstByte & 0x0F;
                    boolean masked = (secondByte & 0x80) != 0;
                    int payloadLength = secondByte & 0x7F;

                    if (payloadLength == 126) {
                        payloadLength = (in.read() << 8) | in.read();
                    } else if (payloadLength == 127) {
                        byte[] extended = new byte[8];
                        in.readNBytes(extended, 0, 8);
                        payloadLength = (int) ByteBuffer.wrap(extended).getLong();
                    }

                    byte[] maskKey = new byte[4];
                    if (masked) {
                        in.readNBytes(maskKey, 0, 4);
                    }

                    byte[] payload = new byte[payloadLength];
                    in.readNBytes(payload, 0, payloadLength);

                    if (masked) {
                        for (int i = 0; i < payload.length; i++) {
                            payload[i] ^= maskKey[i % 4];
                        }
                    }

                    switch (opcode) {
                        case OPCODE_TEXT -> {
                            String text = new String(payload, StandardCharsets.UTF_8);
                            handleMessage(text);
                        }
                        case OPCODE_CLOSE -> {
                            connected = false;
                            return;
                        }
                        case OPCODE_PING -> sendPong();
                        case OPCODE_PONG -> {}
                    }
                } catch (SocketTimeoutException e) {
                    send("{\"type\":\"ping\"}");
                }
            }
        } catch (IOException ignored) {
        } finally {
            connected = false;
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleMessage(String message) {
    }

    private void sendFrame(int opcode, byte[] payload) throws IOException {
        byte[] header;
        int len = payload.length;
        if (len < 126) {
            header = new byte[2];
            header[1] = (byte) len;
        } else if (len < 65536) {
            header = new byte[4];
            header[1] = (byte) 126;
            header[2] = (byte) (len >> 8);
            header[3] = (byte) (len & 0xFF);
        } else {
            header = new byte[10];
            header[1] = (byte) 127;
            ByteBuffer.wrap(header, 2, 8).putLong(len);
        }
        header[0] = (byte) (0x80 | (opcode & 0x0F));

        synchronized (this) {
            out.write(header);
            out.write(payload);
            out.flush();
        }
    }
}
