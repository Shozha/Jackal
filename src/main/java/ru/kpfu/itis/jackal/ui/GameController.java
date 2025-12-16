package ru.kpfu.itis.jackal.ui;

import javax.swing.*;
import ru.kpfu.itis.jackal.ui.screens.MainMenuScreen;
import ru.kpfu.itis.jackal.ui.screens.LobbyScreen;
import ru.kpfu.itis.jackal.ui.screens.GameScreen;
import ru.kpfu.itis.jackal.network.NetworkClient;
import ru.kpfu.itis.jackal.network.protocol.GameMessage;
import ru.kpfu.itis.jackal.network.protocol.MessageType;
import ru.kpfu.itis.jackal.server.GameServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * GameController - главный контроллер приложения (Swing UI + встроенный сервер)
 * Версия [77] - упрощенная, без обработки логики (это делает GameEngine)
 *
 * Эта версия:
 * - Получает GAME_STATE и отображает состояние
 * - Отправляет действия игрока
 * - НЕ обрабатывает типы сообщений (это делает GameEngine)
 * - Работает с текущим GameServer.java (без shutdown())
 */
public class GameController {

    private AppFrame appFrame;
    private NetworkClient networkClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private GameServer gameServer;
    private Thread serverThread;

    private MainMenuScreen mainMenuScreen;
    private LobbyScreen lobbyScreen;
    private GameScreen gameScreen;

    private String playerName;
    private String playerId;
    private String currentPlayer;
    private int currentRound;
    private boolean isHost = false;
    private Integer selectedPirateId = null;

    public GameController() {
        this.appFrame = new AppFrame("Шакал - Pirates Game", 1200, 800);
        this.networkClient = new NetworkClient();

        showMainMenu();
    }

    private void showMainMenu() {
        mainMenuScreen = new MainMenuScreen();
        appFrame.setContent(mainMenuScreen);

        mainMenuScreen.setConnectListener(e -> handleConnect());
        mainMenuScreen.setExitListener(e -> System.exit(0));
    }

    private void handleConnect() {
        String name = mainMenuScreen.getPlayerName();
        String host = mainMenuScreen.getHost();
        int port = mainMenuScreen.getPort();

        if (name == null || name.trim().isEmpty()) {
            mainMenuScreen.setStatus("✗ Введите имя игрока", true);
            return;
        }

        if (host == null || host.trim().isEmpty()) {
            mainMenuScreen.setStatus("✗ Введите адрес сервера", true);
            return;
        }

        if (port < 1 || port > 65535) {
            mainMenuScreen.setStatus("✗ Неправильный порт", true);
            return;
        }

        this.playerName = name.trim();
        mainMenuScreen.setStatus("Инициализация...", false);
        mainMenuScreen.enableConnect(false);

        new Thread(() -> {
            try {
                // Если хост = localhost, запускаем встроенный сервер
                if (isLocalhost(host)) {
                    startEmbeddedServer(port);
                    this.isHost = true;
                    mainMenuScreen.setStatus("Сервер запущен, подключение...", false);
                    Thread.sleep(1000);
                }

                // Подключаемся как клиент
                networkClient.connect(host, port, playerName);
                networkClient.setMessageListener(this::handleMessage);

                SwingUtilities.invokeLater(this::showLobby);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    mainMenuScreen.setStatus("✗ Ошибка: " + ex.getMessage(), true);
                    mainMenuScreen.enableConnect(true);
                });
            }
        }).start();
    }

    private boolean isLocalhost(String host) {
        return host.equals("localhost") || host.equals("127.0.0.1");
    }

    private void startEmbeddedServer(int port) throws Exception {
        gameServer = new GameServer();

        serverThread = new Thread(() -> {
            try {
                System.out.println("[GameController] ⭐ Запуск встроенного сервера на порту 8888...");
                gameServer.start();
            } catch (Exception e) {
                System.err.println("[GameController] ✗ Ошибка при запуске сервера:");
                e.printStackTrace();
            }
        });

        serverThread.setName("GameServer-Thread");
        serverThread.setDaemon(false);
        serverThread.start();
    }

    private void showLobby() {
        lobbyScreen = new LobbyScreen();
        appFrame.setContent(lobbyScreen);

        lobbyScreen.setStartGameListener(e -> handleStartGame());
        lobbyScreen.setExitListener(e -> handleExit());
    }

    private void handleStartGame() {
        try {
            // ⭐ ОТПРАВЛЯЕМ PLAYER_READY - GameEngine обработает
            GameMessage readyMessage = new GameMessage();
            readyMessage.setType(MessageType.PLAYER_READY);
            readyMessage.setPlayerId(networkClient.getPlayerId());
            readyMessage.setData("{}");

            networkClient.sendMessage(readyMessage);
            lobbyScreen.setStatus("Отправлен сигнал готовности...", false);
        } catch (Exception ex) {
            lobbyScreen.setStatus("✗ Ошибка: " + ex.getMessage(), false);
        }
    }

    private void showGame() {
        gameScreen = new GameScreen();
        appFrame.setContent(gameScreen);

        gameScreen.setEndTurnListener(e -> handleEndTurn());
        gameScreen.setExitListener(e -> handleExit());
        gameScreen.setCellClickListener(this::handleCellClick);
    }

    private void handleCellClick(Integer x, Integer y) {

        // 1️⃣ Выбор пирата
        if (x == -1) {
            selectedPirateId = y;
            gameScreen.setSelectedPirate(y);
            return;
        }

        // 2️⃣ Проверка хода
        if (currentPlayer == null || !currentPlayer.equals(playerId)) {
            gameScreen.setActionStatus("✗ Это не ваш ход!");
            return;
        }

        if (selectedPirateId == null) {
            gameScreen.setActionStatus("✗ Сначала выберите пирата");
            return;
        }

        try {
            String actionData = """
                {
                  "actionType": "MOVE",
                  "pirateId": %d,
                  "toX": %d,
                  "toY": %d
                }
                """.formatted(selectedPirateId, x, y);

            GameMessage moveMessage = new GameMessage();
            moveMessage.setType(MessageType.PLAYER_ACTION);
            moveMessage.setPlayerId(networkClient.getPlayerId());
            moveMessage.setData(actionData);

            networkClient.sendMessage(moveMessage);
            gameScreen.setActionStatus(
                    "Пират #" + selectedPirateId + " → (" + x + ", " + y + ")"
            );

        } catch (Exception ex) {
            gameScreen.setActionStatus("✗ Ошибка: " + ex.getMessage());
        }
    }

    private void handleEndTurn() {
        try {
            // ⭐ ОТПРАВЛЯЕМ действие - GameEngine обработает
            String turnData = "{\"action\": \"END_TURN\"}";
            GameMessage turnMessage = new GameMessage();
            turnMessage.setType(MessageType.PLAYER_ACTION);
            turnMessage.setPlayerId(networkClient.getPlayerId());
            turnMessage.setData(turnData);

            networkClient.sendMessage(turnMessage);
            gameScreen.setActionStatus("Ход завершен, ожидаем ответа сервера...");
        } catch (Exception ex) {
            gameScreen.setActionStatus("✗ Ошибка: " + ex.getMessage());
        }
    }

    private void handleExit() {
        System.out.println("[GameController] 👋 Выход из приложения...");

        networkClient.disconnect();

        if (isHost && serverThread != null && serverThread.isAlive()) {
            System.out.println("[GameController] 🛑 Завершение встроенного сервера...");

            try {
                // ⭐ ПРОСТО прерываем поток сервера
                serverThread.interrupt();

                // ⭐ ЖДЕМ завершения потока сервера
                System.out.println("[GameController] ⏳ Ожидание завершения потока...");
                serverThread.join(5000);  // максимум 5 секунд
                System.out.println("[GameController] ✓ Поток сервера завершен");
            } catch (InterruptedException e) {
                System.err.println("[GameController] ⚠️ Ошибка при завершении сервера: " + e.getMessage());
            }
        }

        System.exit(0);
    }

    /**
     * ⭐ УПРОЩЕННАЯ обработка сообщений
     * Принимаем только GAME_STATE и отображаем
     * Остальное обрабатывает GameEngine на сервере
     */
    private void handleMessage(GameMessage message) {
        if (message == null || message.getType() == null) {
            System.err.println("[GameController] ❌ Получено null сообщение");
            return;
        }

        MessageType type = message.getType();
        System.out.println("[GameController] 📬 Получено: " + type);

        SwingUtilities.invokeLater(() -> {
            try {
                // ⭐ ГЛАВНОЕ: обрабатываем только GAME_STATE
                if (type == MessageType.GAME_STATE) {
                    updateGameState(message);
                }
                else if (type == MessageType.GAME_START) {
                    showGame();
                }
                else if (type == MessageType.GAME_END) {
                    handleGameEnd(message);
                }
                else if (type == MessageType.ERROR) {
                    JOptionPane.showMessageDialog(appFrame,
                            "Ошибка от сервера: " + message.getData(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
                else {
                    // ⭐ ДРУГИЕ типы сообщений игнорируем
                    // Они обрабатываются на сервере (GameEngine)
                    System.out.println("[GameController] ℹ️ Сообщение типа " + type + " обрабатывается сервером");
                }
            } catch (Exception ex) {
                System.err.println("[GameController] ❌ Ошибка при обработке сообщения:");
                ex.printStackTrace();
            }
        });
    }

    /**
     * ⭐ ГЛАВНЫЙ метод - обновляем состояние игры с сервера
     */
    private void updateGameState(GameMessage message) throws Exception {
        if (message.getData() == null || gameScreen == null) {
            return;
        }

        JsonNode data = objectMapper.readTree(message.getData());

        // Получаем текущего игрока и раунд
        if (data.has("currentPlayerId")) {
            currentPlayer = data.get("currentPlayerId").asText();
            this.playerId = networkClient.getPlayerId();
        }
        if (data.has("turnNumber")) {
            currentRound = data.get("turnNumber").asInt(0);
        }

        // Отображаем информацию
        if (currentPlayer != null) {
            gameScreen.setCurrentPlayer(currentPlayer, currentRound);
        }

        // Обновляем доску (если есть в state)
        if (data.has("board")) {
            JsonNode boardNode = data.get("board");
            if (boardNode != null && boardNode.isArray()) {
                String[][] board = new String[9][9];
                for (int y = 0; y < 9 && y < boardNode.size(); y++) {
                    JsonNode row = boardNode.get(y);
                    if (row != null && row.isArray()) {
                        for (int x = 0; x < 9 && x < row.size(); x++) {
                            board[y][x] = formatCell(row.get(x));
                        }
                    }
                }
                gameScreen.updateBoard(board);
            }
        }

        // Обновляем список игроков
        if (data.has("players")) {
            JsonNode playersNode = data.get("players");
            if (playersNode != null && playersNode.isArray()) {
                String[] playerInfos = new String[playersNode.size()];
                int idx = 0;
                for (JsonNode player : playersNode) {
                    String name = player.get("name").asText("?");
                    int score = player.get("score").asInt(0);
                    playerInfos[idx++] = name + ": " + score + " очков";
                }
                gameScreen.updatePlayersInfo(playerInfos);
            }
        }

        // Обновляем статус (чей ход)
        boolean isOurTurn = currentPlayer != null && currentPlayer.equals(playerId);
        if (isOurTurn) {
            gameScreen.setGameStatus("✓ Ваш ход!", true);
        } else {
            gameScreen.setGameStatus("Ход: " + (currentPlayer != null ? currentPlayer : "?"), false);
        }
    }

    private String formatCell(JsonNode cellNode) {
        if (cellNode == null || cellNode.isNull()) return " ";

        try {
            // 1️⃣ ПИРАТ (самый приоритетный)
            JsonNode pirateNode = cellNode.get("pirate");
            if (pirateNode != null && !pirateNode.isNull()) {
                int pirateId = pirateNode.get("id").asInt();
                return "P" + pirateId; // P1, P2
            }

            // 2️⃣ ЗОЛОТО
            JsonNode goldNode = cellNode.get("gold");
            if (goldNode != null && !goldNode.isNull()) {
                int amount = goldNode.get("amount").asInt();
                return String.valueOf(amount); // 1,2,3,5
            }

            // 3️⃣ ТИП КЛЕТКИ
            String type = cellNode.get("type").asText("SEA");
            return switch (type) {
                case "PLAIN" -> "PLAIN";
                case "FOREST" -> "FOREST";
                case "MOUNTAIN" -> "MOUNTAIN";
                case "FORT" -> "FORT";
                case "BEACH_RED",
                     "BEACH_BLUE",
                     "BEACH_GREEN",
                     "BEACH_YELLOW" -> type;
                case "SEA" -> "SEA";
                default -> " ";
            };
        } catch (Exception e) {
            return " ";
        }
    }

    private void handleGameEnd(GameMessage message) throws Exception {
        if (message.getData() == null) return;

        JsonNode data = objectMapper.readTree(message.getData());
        String winner = data.has("winnerName") ? data.get("winnerName").asText("?") : "?";

        JOptionPane.showMessageDialog(appFrame,
                "Победитель: " + winner,
                "Игра завершена",
                JOptionPane.INFORMATION_MESSAGE);

        handleExit();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GameController();
        });
    }
}
