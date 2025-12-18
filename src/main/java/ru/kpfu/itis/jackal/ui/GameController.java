package ru.kpfu.itis.jackal.ui;

import javax.swing.*;
import ru.kpfu.itis.jackal.ui.screens.MainMenuScreen;
import ru.kpfu.itis.jackal.ui.screens.LobbyScreen;
import ru.kpfu.itis.jackal.ui.screens.GameScreen;
import ru.kpfu.itis.jackal.network.NetworkClient;
import ru.kpfu.itis.jackal.network.protocol.GameMessage;
import ru.kpfu.itis.jackal.network.protocol.MessageType;
import ru.kpfu.itis.jackal.server.GameServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * GameController - главный контроллер приложения
 * ✅ Версия [FIXED] - ВЕЗДЕ GSON!
 *
 * ИСПРАВЛЕНИЯ:
 * ✅ Jackson → Gson везде
 * ✅ ObjectMapper → Gson
 * ✅ JsonNode → JsonObject
 * ✅ readTree() → JsonParser.parseString()
 */
public class GameController {

    private AppFrame appFrame;
    private NetworkClient networkClient;
    private static final Gson gson = new GsonBuilder().create();
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
    private boolean gameStarting = false;

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
        boolean createServer = mainMenuScreen.isHostSelected();

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
                if (createServer) {
                    startEmbeddedServer(port);
                    this.isHost = true;
                    mainMenuScreen.setStatus("Сервер запущен, подключение...", false);
                    Thread.sleep(1000);
                }

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

    private void startEmbeddedServer(int port) {
        gameServer = new GameServer(port);
        serverThread = new Thread(() -> {
            try {
                System.out.println("[GameController] ⭐ Запуск встроенного сервера на порту " + port + "...");
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
        lobbyScreen.setReadyListener(e -> handleReadyToggle());
        lobbyScreen.setStartGameListener(e -> handleStartGame());
        lobbyScreen.setExitListener(e -> handleExit());
    }

    private void handleReadyToggle() {
        boolean currentReady = lobbyScreen.getReadyStatus();
        boolean newReady = !currentReady;

        try {
            GameMessage readyMessage = new GameMessage();
            readyMessage.setType(MessageType.PLAYER_READY);
            readyMessage.setPlayerId(networkClient.getPlayerId());
            readyMessage.setData("{\"ready\": " + newReady + "}");

            networkClient.sendMessage(readyMessage);
            lobbyScreen.setReadyButtonStatus(newReady);
            lobbyScreen.setStatus(newReady ? "Вы готовы! Ожидаем других..." : "Вы не готовы", false);
            System.out.println("[GameController] 🔘 Ready toggled: " + newReady);

        } catch (Exception ex) {
            lobbyScreen.setStatus("✗ Ошибка: " + ex.getMessage(), false);
        }
    }

    /**
     * ⭐ ИСПРАВЛЕНО [FIXED]: НЕ отправляем PLAYER_READY (не сбрасывает готовность)
     */
    private void handleStartGame() {
        if (gameStarting) {
            System.out.println("[GameController] ⚠️ Игра уже запускается, игнорируем повторное нажатие");
            return;
        }

        gameStarting = true;

        try {
            // ⭐ ГЛАВНОЕ ИСПРАВЛЕНИЕ: НЕ отправляем PLAYER_READY (было раньше)
            // Просто уведомляем сервер что нажали кнопку "Начать игру"
            GameMessage startMessage = new GameMessage();
            startMessage.setType(MessageType.PLAYER_ACTION);
            startMessage.setPlayerId(networkClient.getPlayerId());
            startMessage.setData("{\"action\": \"START_GAME\"}");

            networkClient.sendMessage(startMessage);
            lobbyScreen.setStatus("Запуск игры...", false);
            System.out.println("[GameController] 🎮 Нажата кнопка 'Начать игру'");

        } catch (Exception ex) {
            gameStarting = false;
            lobbyScreen.setStatus("✗ Ошибка: " + ex.getMessage(), false);
        }
    }

    private void showGame() {
        gameScreen = new GameScreen();
        appFrame.setContent(gameScreen);
        selectedPirateId = null;
        gameScreen.setEndTurnListener(e -> handleEndTurn());
        gameScreen.setExitListener(e -> handleExit());
        gameScreen.setCellClickListener(this::handleCellClick);
    }

    private void handleCellClick(Integer x, Integer y) {
        if (x == -1) {
            selectedPirateId = y;
            gameScreen.setSelectedPirate(y);
            return;
        }

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
                    {"actionType": "MOVE", "pirateId": %d, "toX": %d, "toY": %d}
                    """.formatted(selectedPirateId, x, y);

            GameMessage moveMessage = new GameMessage();
            moveMessage.setType(MessageType.PLAYER_ACTION);
            moveMessage.setPlayerId(networkClient.getPlayerId());
            moveMessage.setData(actionData);

            networkClient.sendMessage(moveMessage);
            gameScreen.setActionStatus("Пират #" + selectedPirateId + " → (" + x + ", " + y + ")");

        } catch (Exception ex) {
            gameScreen.setActionStatus("✗ Ошибка: " + ex.getMessage());
        }
    }

    private void handleEndTurn() {
        try {
            String turnData = "{\"actionType\": \"ENDTURN\"}";
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
                serverThread.interrupt();
                System.out.println("[GameController] ⏳ Ожидание завершения потока...");
                serverThread.join(5000);
                System.out.println("[GameController] ✓ Поток сервера завершен");
            } catch (InterruptedException e) {
                System.err.println("[GameController] ⚠️ Ошибка при завершении сервера: " + e.getMessage());
            }
        }

        System.exit(0);
    }

    private void handleMessage(GameMessage message) {
        if (message == null || message.getType() == null) {
            System.err.println("[GameController] ❌ Получено null сообщение");
            return;
        }

        MessageType type = message.getType();
        System.out.println("[GameController] 📬 Получено: " + type);

        SwingUtilities.invokeLater(() -> {
            try {
                if (type == MessageType.GAME_STATE) {
                    updateGameState(message);
                }
                else if (type == MessageType.GAME_START) {
                    System.out.println("[GameController] 🎮 GAME_START получен, переходим в игру");
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
                    System.out.println("[GameController] ℹ️ Сообщение типа " + type + " обрабатывается сервером");
                }

            } catch (Exception ex) {
                System.err.println("[GameController] ❌ Ошибка при обработке сообщения:");
                ex.printStackTrace();
            }
        });
    }

    /**
     * ⭐ ГЛАВНЫЙ МЕТОД - ОТЛАДКА ЛОББИ
     * ✅ ИСПРАВЛЕНО: Везде Gson!
     */
    private void updateGameState(GameMessage message) throws Exception {
        if (message.getData() == null) {
            return;
        }

        // ✅ ИСПРАВЛЕНО: Использование Gson вместо Jackson
        JsonObject data = JsonParser.parseString(message.getData()).getAsJsonObject();

        if (data.has("currentPlayerId")) {
            currentPlayer = data.get("currentPlayerId").getAsString();
            this.playerId = networkClient.getPlayerId();
        }

        if (data.has("turnNumber")) {
            currentRound = data.get("turnNumber").getAsInt();
        }

        // ⭐ ОБНОВЛЯЕМ ЛОББИ
        if (lobbyScreen != null && data.has("players")) {
            JsonArray playersNode = data.getAsJsonArray("players");

            if (playersNode != null) {
                String[] playerNames = new String[playersNode.size()];
                boolean[] readyStatus = new boolean[playersNode.size()];

                for (int i = 0; i < playersNode.size(); i++) {
                    JsonObject player = playersNode.get(i).getAsJsonObject();
                    String name = player.get("name").getAsString();
                    boolean ready = player.has("ready") && player.get("ready").getAsBoolean();

                    playerNames[i] = name;
                    readyStatus[i] = ready;
                }

                // ⭐ ГЛАВНОЕ: обновляем список с галочками
                System.out.println("[GameController] 📋 Обновляем список: " + java.util.Arrays.toString(playerNames));
                System.out.println("[GameController] 📊 Статусы готовности: " + java.util.Arrays.toString(readyStatus));

                lobbyScreen.updatePlayersWithReadyStatus(playerNames, readyStatus);
                lobbyScreen.setPlayerCount(playerNames.length, 4);

                // ⭐ Проверяем, все ли готовы
                boolean allReady = true;
                for (boolean ready : readyStatus) {
                    if (!ready) {
                        allReady = false;
                        break;
                    }
                }

                // ⭐ КРИТИЧНО: вызываем setStatus с правильным флагом!
                if (playerNames.length >= 2 && allReady) {
                    System.out.println("[GameController] ✅ ВСЕ " + playerNames.length + " ИГРОКОВ ГОТОВЫ!");
                    lobbyScreen.setStatus("Все готовы! Нажмите 'Начать игру'", true);
                } else {
                    System.out.println("[GameController] ❌ ЕСТЬ НЕ ГОТОВЫЕ ИГРОКИ - отключаем кнопку");
                    lobbyScreen.setStatus("Ожидаем готовности остальных игроков...", false);
                    gameStarting = false;
                }
            }
        }

        // Обновляем игру если она уже запущена
        if (gameScreen != null && data.has("players")) {
            JsonArray playersNode = data.getAsJsonArray("players");

            if (playersNode != null) {
                String[] playerInfos = new String[playersNode.size()];

                for (int i = 0; i < playersNode.size(); i++) {
                    JsonObject player = playersNode.get(i).getAsJsonObject();
                    String name = player.get("name").getAsString();
                    int score = player.get("score").getAsInt();
                    playerInfos[i] = name + ": " + score + " очков";
                }

                gameScreen.updatePlayersInfo(playerInfos);
            }
        }

        if (gameScreen != null && data.has("board")) {
            JsonArray boardNode = data.getAsJsonArray("board");

            if (boardNode != null) {
                String[][] board = new String[9][9];

                for (int y = 0; y < 9 && y < boardNode.size(); y++) {
                    JsonArray row = boardNode.get(y).getAsJsonArray();

                    if (row != null) {
                        for (int x = 0; x < 9 && x < row.size(); x++) {
                            // ✅ ИСПРАВЛЕНО: Парсим строку как JSON!
                            String cellStr = row.get(x).getAsString();
                            JsonObject cellObj = JsonParser.parseString(cellStr).getAsJsonObject();
                            board[y][x] = formatCell(cellObj);
                        }
                    }
                }

                gameScreen.updateBoard(board);
            }
        }

        if (gameScreen != null && currentPlayer != null) {
            gameScreen.setCurrentPlayer(currentPlayer, currentRound);
            boolean isOurTurn = currentPlayer.equals(playerId);

            // ⭐ НОВОЕ: Сброс выбора пирата при смене хода
            selectedPirateId = null;

            if (isOurTurn) {
                gameScreen.setGameStatus("✅ Ваш ход! Выберите пирата", true);
            } else {
                gameScreen.setGameStatus("⏳ Ход " + currentPlayer, false);
            }
        }
    }

    private String formatCell(JsonObject cellNode) {
        if (cellNode == null) return " ";

        try {
            if (cellNode.has("pirate")) {
                JsonObject pirateObj = cellNode.getAsJsonObject("pirate");
                int pirateId = pirateObj.get("id").getAsInt();
                return "P" + pirateId;
            }

            if (cellNode.has("gold")) {
                JsonObject goldObj = cellNode.getAsJsonObject("gold");
                int amount = goldObj.get("amount").getAsInt();
                return String.valueOf(amount);
            }

            String type = cellNode.has("type") ? cellNode.get("type").getAsString() : "SEA";

            return switch (type) {
                case "PLAIN" -> "PLAIN";
                case "FOREST" -> "FOREST";
                case "MOUNTAIN" -> "MOUNTAIN";
                case "FORT" -> "FORT";
                case "BEACH_RED", "BEACH_BLUE", "BEACH_GREEN", "BEACH_YELLOW" -> type;
                case "SEA" -> "SEA";
                default -> " ";
            };

        } catch (Exception e) {
            return " ";
        }
    }

    private void handleGameEnd(GameMessage message) throws Exception {
        if (message.getData() == null) return;

        // ✅ ИСПРАВЛЕНО: Gson!
        JsonObject data = JsonParser.parseString(message.getData()).getAsJsonObject();
        String winner = data.has("winnerName") ? data.get("winnerName").getAsString() : "?";

        JOptionPane.showMessageDialog(appFrame,
                "Победитель: " + winner,
                "Игра завершена",
                JOptionPane.INFORMATION_MESSAGE);

        handleExit();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameController::new);
    }
}
