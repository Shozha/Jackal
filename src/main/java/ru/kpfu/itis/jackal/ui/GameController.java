package ru.kpfu.itis.jackal.ui;

import javax.swing.*;

import com.google.gson.*;
import ru.kpfu.itis.jackal.ui.screens.MainMenuScreen;
import ru.kpfu.itis.jackal.ui.screens.LobbyScreen;
import ru.kpfu.itis.jackal.ui.screens.GameScreen;
import ru.kpfu.itis.jackal.network.NetworkClient;
import ru.kpfu.itis.jackal.network.protocol.GameMessage;
import ru.kpfu.itis.jackal.network.protocol.MessageType;
import ru.kpfu.itis.jackal.server.GameServer;

import java.util.ArrayList;
import java.util.List;


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
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
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

    private void handleStartGame() {
        if (gameStarting) {
            System.out.println("[GameController] ⚠️ Игра уже запускается");
            return;
        }

        gameStarting = true;

        try {
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
            JsonObject actionJson = new JsonObject();
            actionJson.addProperty("actionType", "MOVE");
            actionJson.addProperty("pirateId", selectedPirateId);
            actionJson.addProperty("toX", x);
            actionJson.addProperty("toY", y);

            GameMessage moveMessage = new GameMessage();
            moveMessage.setType(MessageType.PLAYER_ACTION);
            moveMessage.setPlayerId(networkClient.getPlayerId());
            moveMessage.setData(gson.toJson(actionJson));

            networkClient.sendMessage(moveMessage);
            gameScreen.setActionStatus("Пират #" + selectedPirateId + " → (" + x + ", " + y + ")");

        } catch (Exception ex) {
            gameScreen.setActionStatus("✗ Ошибка: " + ex.getMessage());
        }
    }

    private void handleEndTurn() {
        try {
            JsonObject turnJson = new JsonObject();
            turnJson.addProperty("actionType", "ENDTURN");

            GameMessage turnMessage = new GameMessage();
            turnMessage.setType(MessageType.PLAYER_ACTION);
            turnMessage.setPlayerId(networkClient.getPlayerId());
            turnMessage.setData(gson.toJson(turnJson));

            networkClient.sendMessage(turnMessage);
            gameScreen.setActionStatus("Ход завершен, ожидаем ответа сервера...");

            System.out.println("[GameController] 🔄 ENDTURN отправлено");
        } catch (Exception ex) {
            gameScreen.setActionStatus("✗ Ошибка: " + ex.getMessage());
        }
    }

    private void handleExit() {
        System.out.println("[GameController] 👋 Выход из приложения...");
        networkClient.disconnect();

        if (isHost && serverThread != null && serverThread.isAlive()) {
            try {
                serverThread.interrupt();
                serverThread.join(5000);
            } catch (InterruptedException e) {
                System.err.println("[GameController] ⚠️ Ошибка: " + e.getMessage());
            }
        }

        System.exit(0);
    }

    private void handleMessage(GameMessage message) {
        if (message == null || message.getType() == null) {
            System.err.println("[GameController] ❌ Null сообщение");
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
                    System.out.println("[GameController] 🎮 GAME_START, переходим в игру");
                    showGame();
                }
                else if (type == MessageType.GAME_END) {
                    handleGameEnd(message);
                }
                else if (type == MessageType.ERROR) {
                    JOptionPane.showMessageDialog(appFrame,
                            "Ошибка: " + message.getData(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                System.err.println("[GameController] ❌ Ошибка при обработке сообщения:");
                ex.printStackTrace();
            }
        });
    }

    private void updateGameState(GameMessage message) throws Exception {
        if (message.getData() == null) {
            return;
        }

        JsonElement jsonElement = JsonParser.parseString(message.getData());
        JsonObject data = jsonElement.getAsJsonObject();

        if (data.has("currentPlayerId")) {
            currentPlayer = data.get("currentPlayerId").getAsString();
            this.playerId = networkClient.getPlayerId();
        }
        if (data.has("turnNumber")) {
            currentRound = data.get("turnNumber").getAsInt();
        }

        if (lobbyScreen != null && data.has("players")) {
            JsonArray playersArray = data.getAsJsonArray("players");
            if (playersArray != null && playersArray.size() > 0) {
                String[] playerNames = new String[playersArray.size()];
                boolean[] readyStatus = new boolean[playersArray.size()];
                int idx = 0;
                for (JsonElement playerElem : playersArray) {
                    JsonObject player = playerElem.getAsJsonObject();
                    String name = player.get("name").getAsString();
                    boolean ready = player.has("ready") && player.get("ready").getAsBoolean();
                    playerNames[idx] = name;
                    readyStatus[idx] = ready;
                    idx++;
                }

                System.out.println("[GameController] 📋 Обновляем список: " + java.util.Arrays.toString(playerNames));
                lobbyScreen.updatePlayersWithReadyStatus(playerNames, readyStatus);
                lobbyScreen.setPlayerCount(playerNames.length, 4);

                boolean allReady = true;
                for (boolean ready : readyStatus) {
                    if (!ready) {
                        allReady = false;
                        break;
                    }
                }

                if (playerNames.length >= 2 && allReady) {
                    System.out.println("[GameController] ✅ ВСЕ ГОТОВЫ!");
                    lobbyScreen.setStatus("Все готовы! Нажмите 'Начать игру'", true);
                } else {
                    System.out.println("[GameController] ❌ НЕ ВСЕ ГОТОВЫ");
                    lobbyScreen.setStatus("Ожидаем готовности...", false);
                    gameStarting = false;
                }
            }
        }

        if (gameScreen != null) {
            if (data.has("board")) {
                JsonArray boardArray = data.getAsJsonArray("board");
                if (boardArray != null && boardArray.size() == 9) {
                    String[][] board = new String[9][9];
                    for (int y = 0; y < 9; y++) {
                        JsonArray row = boardArray.get(y).getAsJsonArray();
                        if (row != null && row.size() == 9) {
                            for (int x = 0; x < 9; x++) {
                                board[y][x] = formatCell(row.get(x));
                            }
                        }
                    }
                    gameScreen.updateBoard(board);

                    if (selectedPirateId != null) {
                        List<String> possibleMoves = calculatePossibleMoves(selectedPirateId, data);
                        gameScreen.updatePossibleMoves(possibleMoves);
                        System.out.println("[GameController] 🎯 Ходы: " + possibleMoves);
                    }
                }
            }

            if (data.has("players")) {
                JsonArray playersArray = data.getAsJsonArray("players");
                if (playersArray != null && playersArray.size() > 0) {
                    String[] playerInfos = new String[playersArray.size()];
                    int idx = 0;
                    for (JsonElement playerElem : playersArray) {
                        JsonObject player = playerElem.getAsJsonObject();
                        String name = player.get("name").getAsString();
                        int score = player.has("score") ? player.get("score").getAsInt() : 0;
                        playerInfos[idx++] = name + ": " + score + " очков";
                    }
                    gameScreen.updatePlayersInfo(playerInfos);
                }
            }

            if (currentPlayer != null) {
                gameScreen.setCurrentPlayer(currentPlayer, currentRound);
                boolean isOurTurn = currentPlayer.equals(playerId);

                if (!isOurTurn) {
                    selectedPirateId = null;
                }

                if (isOurTurn) {
                    gameScreen.setGameStatus("✅ Ваш ход!", true);
                } else {
                    gameScreen.setGameStatus("⏳ Ход " + currentPlayer, false);
                }
            }
        }
    }

    private List<String> calculatePossibleMoves(int pirateId, JsonObject gameStateData) {
        List<String> moves = new ArrayList<>();

        int pirateX = -1, pirateY = -1;

        JsonArray boardArray = gameStateData.getAsJsonArray("board");
        if (boardArray != null && boardArray.size() == 9) {
            for (int y = 0; y < 9; y++) {
                JsonArray row = boardArray.get(y).getAsJsonArray();
                if (row != null && row.size() == 9) {
                    for (int x = 0; x < 9; x++) {
                        JsonObject cell = row.get(x).getAsJsonObject();

                        if (cell != null && cell.has("pirate")) {
                            JsonObject pirateObj = cell.getAsJsonObject("pirate");
                            if (pirateObj != null && pirateObj.has("id")) {
                                int id = pirateObj.get("id").getAsInt();
                                if (id == pirateId) {
                                    pirateX = x;
                                    pirateY = y;
                                    System.out.println("[GameController] 🧭 Пират найден на (" + x + "," + y + ")");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (pirateX >= 0 && pirateY >= 0) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    int newX = pirateX + dx;
                    int newY = pirateY + dy;

                    if (newX >= 0 && newX < 9 && newY >= 0 && newY < 9) {
                        moves.add(newX + "," + newY);
                    }
                }
            }
        }

        return moves;
    }

    private String formatCell(JsonElement cellElem) {
        if (cellElem == null || cellElem.isJsonNull()) return " ";
        try {
            JsonObject cell = cellElem.getAsJsonObject();

            if (cell.has("pirate") && !cell.get("pirate").isJsonNull()) {
                JsonObject pirate = cell.getAsJsonObject("pirate");
                int pirateId = pirate.get("id").getAsInt();
                return "P" + pirateId;
            }

            if (cell.has("gold")) {
                int amount = cell.get("gold").getAsInt();
                return String.valueOf(amount);
            }

            String type = cell.has("type") ? cell.get("type").getAsString() : "SEA";
            return type;
        } catch (Exception e) {
            return " ";
        }
    }

    private void handleGameEnd(GameMessage message) throws Exception {
        if (message.getData() == null) return;

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
