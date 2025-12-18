package ru.kpfu.itis.jackal.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import ru.kpfu.itis.jackal.network.protocol.GameMessage;
import ru.kpfu.itis.jackal.network.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

public class NetworkClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    @Setter
    private Consumer<GameMessage> messageListener;
    private volatile boolean connected = false;
    private static final Gson gson = new GsonBuilder().create();
    @Getter
    private String playerId;

    public void connect(String host, int port, String playerName) throws IOException {
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.playerId = UUID.randomUUID().toString();
            this.connected = true;

            System.out.println("Подключились к серверу: " + host + ":" + port);
            System.out.println("PlayerId: " + playerId);

            GameMessage joinMessage = new GameMessage();
            joinMessage.setType(MessageType.PLAYER_JOIN);
            joinMessage.setPlayerId(playerId);
            joinMessage.setData(gson.toJson(new PlayerJoinPayload(playerName)));
            sendMessage(joinMessage);
            startListeningThread();

        } catch (IOException e) {
            this.connected = false;
            System.err.println("Ошибка подключения: " + e.getMessage());
            throw e;
        }
    }

    private void startListeningThread() {
        new Thread(() -> {
            try {
                while (connected && !socket.isClosed()) {
                    String jsonLine = in.readLine();

                    if (jsonLine == null) {
                        System.out.println("Сервер закрыл соединение");
                        connected = false;
                        break;
                    }

                    try {
                        GameMessage message = gson.fromJson(jsonLine, GameMessage.class);
                        if (messageListener != null) messageListener.accept(message);
                    } catch (Exception e) {
                        System.err.println("Ошибка парсинга JSON: " + e.getMessage());
                        System.err.println("Получена строка: " + jsonLine);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Ошибка при чтении из сокета: " + e.getMessage());
                }
            } finally {
                connected = false;
            }
        }, "NetworkListener").start();
    }

    public void sendMessage(GameMessage message) {
        if (!connected || out == null) {
            System.err.println("Не подключены к серверу!");
            return;
        }

        try {
            String json = gson.toJson(message);
            out.println(json);
            out.flush();
            System.out.println("Отправлено: " + message.getType());

        } catch (Exception e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();

            System.out.println("Отключились от сервера");
        } catch (IOException e) {
            System.err.println("Ошибка при отключении: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public static class PlayerJoinPayload {
        public String playerName;

        public PlayerJoinPayload(String playerName) {
            this.playerName = playerName;
        }
    }
}