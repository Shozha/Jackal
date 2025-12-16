package ru.kpfu.itis.jackal.test;

import ru.kpfu.itis.jackal.network.protocol.*;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TestClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private String playerName;
    private String teamColor;

    public TestClient(String playerId, String playerName, String teamColor) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.teamColor = teamColor;
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Подключен к серверу: " + host + ":" + port);

            // Отправляем сообщение о подключении
            sendPlayerJoin();

            // Запускаем поток для чтения сообщений от сервера
            startMessageReader();

            // Запускаем обработку команд пользователя
            startCommandProcessor();

        } catch (IOException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
        }
    }

    private void sendPlayerJoin() {
        PlayerJoinData joinData = new PlayerJoinData(playerName, teamColor);
        GameMessage message = new GameMessage(MessageType.PLAYER_JOIN, playerId,
                MessageParser.dataToJson(joinData));
        sendMessage(message);
        System.out.println("Отправлен запрос на подключение: " + playerName + " (" + teamColor + ")");
    }

    public void sendReady() {
        GameMessage message = new GameMessage(MessageType.PLAYER_READY, playerId, "{}");
        sendMessage(message);
        System.out.println("Отправлен статус готовности");
    }

    public void sendMove(int pirateId, int fromX, int fromY, int toX, int toY) {
        MoveActionData moveData = new MoveActionData(pirateId, fromX, fromY, toX, toY);
        GameMessage message = new GameMessage(MessageType.PLAYER_ACTION, playerId,
                MessageParser.dataToJson(moveData));
        sendMessage(message);
        System.out.println("Отправлен ход: пират " + pirateId + " из (" + fromX + "," + fromY + ") в (" + toX + "," + toY + ")");
    }

    private void sendMessage(GameMessage message) {
        String json = MessageParser.toJson(message);
        out.println(json);
    }

    private void startMessageReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    GameMessage message = MessageParser.fromJson(response);
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Соединение с сервером разорвано");
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void handleServerMessage(GameMessage message) {
        System.out.println("\n=== СООБЩЕНИЕ ОТ СЕРВЕРА ===");
        System.out.println("Тип: " + message.getType());

        switch (message.getType()) {
            case GAME_STATE:
                System.out.println("Получено состояние игры");
                // Здесь можно парсить и выводить детали GameState
                break;
            case ERROR:
                ErrorData error = MessageParser.dataFromJson(message.getData(), ErrorData.class);
                System.out.println("ОШИБКА: " + error.getError());
                break;
            case CHAT_MESSAGE:
                System.out.println("Чат: " + message.getData());
                break;
            case GAME_START:
                System.out.println("=== ИГРА НАЧАЛАСЬ! ===");
                break;
            default:
                System.out.println("Данные: " + message.getData());
        }
        System.out.println("============================\n");
    }

    private void startCommandProcessor() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nДоступные команды:");
        System.out.println("ready - отметить готовность");
        System.out.println("move <pirateId> <toX> <toY> - переместить пирата");
        System.out.println("exit - выход");

        while (true) {
            System.out.print("Введите команду: ");
            String command = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(command)) {
                break;
            } else if ("ready".equalsIgnoreCase(command)) {
                sendReady();
            } else if (command.startsWith("move")) {
                try {
                    String[] parts = command.split(" ");
                    int pirateId = Integer.parseInt(parts[1]);
                    int toX = Integer.parseInt(parts[2]);
                    int toY = Integer.parseInt(parts[3]);
                    sendMove(pirateId, 0, 0, toX, toY); // fromX, fromY пока фиктивные
                } catch (Exception e) {
                    System.out.println("Неверный формат команды. Используйте: move pirateId toX toY");
                }
            } else {
                System.out.println("Неизвестная команда");
            }
        }

        scanner.close();
        disconnect();
    }

    private void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Отключен от сервера");
        } catch (IOException e) {
            System.err.println("Ошибка при отключении: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Использование: java TestClient <playerId> <playerName> <teamColor> <serverHost>");
            System.out.println("Пример: java TestClient player1 Сергей RED localhost");
            return;
        }

        String playerId = args[0];
        String playerName = args[1];
        String teamColor = args[2];
        String serverHost = args[3];

        TestClient client = new TestClient(playerId, playerName, teamColor);
        client.connect(serverHost, 8888);
    }
}