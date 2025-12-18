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

/**
 * NetworkClient - отвечает за подключение к серверу и обмен сообщениями
 * ✅ Версия [FIXED] - ВЕЗДЕ ТОЛЬКО GSON!
 */
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

    /**
     * Подключается к серверу и отправляет PLAYER_JOIN
     */
    public void connect(String host, int port, String playerName) throws IOException {
        try {
            this.socket = new Socket(host, port);

            // ✅ ИСПРАВЛЕНО: Правильное создание PrintWriter с UTF-8
            this.out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
            );

            this.in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );

            // ✅ ИСПРАВЛЕНО: UUID вместо playerName!
            this.playerId = UUID.randomUUID().toString();
            this.connected = true;

            System.out.println("✓ Подключились к серверу: " + host + ":" + port);
            System.out.println("✓ PlayerId: " + playerId);

            // Отправляем PLAYER_JOIN сообщение (Gson!)
            GameMessage joinMessage = new GameMessage();
            joinMessage.setType(MessageType.PLAYER_JOIN);
            joinMessage.setPlayerId(playerId);
            joinMessage.setData(gson.toJson(new PlayerJoinPayload(playerName)));

            sendMessage(joinMessage);

            // Запускаем поток слушания
            startListeningThread();

        } catch (IOException e) {
            this.connected = false;
            System.err.println("✗ Ошибка подключения: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Запускает отдельный поток для слушания сообщений от сервера
     */
    private void startListeningThread() {
        new Thread(() -> {
            try {
                while (connected && !socket.isClosed()) {
                    String jsonLine = in.readLine();

                    if (jsonLine == null) {
                        System.out.println("✗ Сервер закрыл соединение");
                        connected = false;
                        break;
                    }

                    try {
                        // ✅ ИСПРАВЛЕНО: Везде Gson!
                        GameMessage message = gson.fromJson(jsonLine, GameMessage.class);

                        if (messageListener != null) {
                            messageListener.accept(message);
                        }

                    } catch (Exception e) {
                        System.err.println("✗ Ошибка парсинга JSON: " + e.getMessage());
                        System.err.println("  Получена строка: " + jsonLine);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("✗ Ошибка при чтении из сокета: " + e.getMessage());
                }
            } finally {
                connected = false;
            }
        }, "NetworkListener").start();
    }

    /**
     * Отправляет сообщение на сервер
     */
    public void sendMessage(GameMessage message) {
        if (!connected || out == null) {
            System.err.println("✗ Не подключены к серверу!");
            return;
        }

        try {
            // ✅ ИСПРАВЛЕНО: Везде Gson!
            String json = gson.toJson(message);
            out.println(json);
            out.flush();
            System.out.println("→ Отправлено: " + message.getType());

        } catch (Exception e) {
            System.err.println("✗ Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    /**
     * Отключается от сервера
     */
    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();

            System.out.println("✓ Отключились от сервера");
        } catch (IOException e) {
            System.err.println("✗ Ошибка при отключении: " + e.getMessage());
        }
    }

    /**
     * Проверяет подключены ли мы к серверу
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * ✅ НОВЫЙ класс для JSON payload
     */
    public static class PlayerJoinPayload {
        public String playerName;

        public PlayerJoinPayload(String playerName) {
            this.playerName = playerName;
        }
    }
}
