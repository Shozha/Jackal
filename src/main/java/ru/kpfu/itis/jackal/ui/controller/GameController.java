package ru.kpfu.itis.jackal.ui.controller;

import javax.swing.*;

import ru.kpfu.itis.jackal.ui.AppFrame;
import ru.kpfu.itis.jackal.ui.screens.MainMenuScreen;
import ru.kpfu.itis.jackal.ui.screens.LobbyScreen;
import ru.kpfu.itis.jackal.ui.screens.GameScreen;
import ru.kpfu.itis.jackal.network.NetworkClient;
import ru.kpfu.itis.jackal.network.protocol.GameMessage;
import ru.kpfu.itis.jackal.network.protocol.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.event.ActionEvent;

/**
 * GameController - главный контроллер приложения
 * Управляет переходами между экранами и логикой игры
 */
public class GameController {

    private AppFrame appFrame;
    private NetworkClient networkClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    // Текущие экраны
    private MainMenuScreen mainMenuScreen;
    private LobbyScreen lobbyScreen;
    private GameScreen gameScreen;

    // Текущее состояние
    private String playerName;
    private String currentPlayer;
    private int currentRound;

    public GameController() {
        this.appFrame = new AppFrame("Шакал - Pirates Game", 1200, 800);
        this.networkClient = new NetworkClient();

        showMainMenu();
    }

    /**
     * Показывает главное меню
     */
    private void showMainMenu() {
        mainMenuScreen = new MainMenuScreen();
        appFrame.setContent(mainMenuScreen);

        mainMenuScreen.setConnectListener(e -> handleConnect());
        mainMenuScreen.setExitListener(e -> System.exit(0));
    }

    /**
     * Обработчик подключения
     */
    private void handleConnect() {
        String name = mainMenuScreen.getPlayerName();
        String host = mainMenuScreen.getHost();
        int port = mainMenuScreen.getPort();

        // Валидация
        if (name.isEmpty()) {
            mainMenuScreen.setStatus("✗ Введите имя игрока", true);
            return;
        }

        if (host.isEmpty()) {
            mainMenuScreen.setStatus("✗ Введите адрес сервера", true);
            return;
        }

        if (port < 1 || port > 65535) {
            mainMenuScreen.setStatus("✗ Неправильный порт", true);
            return;
        }

        this.playerName = name;
        mainMenuScreen.setStatus("Подключение...", false);
        mainMenuScreen.enableConnect(false);

        // Подключаемся в отдельном потоке
        new Thread(() -> {
            try {
                networkClient.connect(host, port, name);
                networkClient.setMessageListener(this::handleMessage);

                SwingUtilities.invokeLater(this::showLobby);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    mainMenuScreen.setStatus("✗ Ошибка подключения: " + ex.getMessage(), true);
                    mainMenuScreen.enableConnect(true);
                });
            }
        }).start();
    }

    /**
     * Показывает экран лобби
     */
    private void showLobby() {
        lobbyScreen = new LobbyScreen();
        appFrame.setContent(lobbyScreen);

        lobbyScreen.setStartGameListener(e -> handleStartGame());
        lobbyScreen.setExitListener(e -> handleExit());
    }

    /**
     * Обработчик начала игры
     */
    private void handleStartGame() {
        try {
            String readyData = "{\"status\": \"ready\"}";
            GameMessage readyMessage = new GameMessage(
                    MessageType.PLAYER_READY,
                    networkClient.getPlayerId(),
                    readyData
            );
            networkClient.sendMessage(readyMessage);
            lobbyScreen.setStatus("Отправлен сигнал готовности...", false);
        } catch (Exception ex) {
            lobbyScreen.setStatus("✗ Ошибка: " + ex.getMessage(), false);
        }
    }

    /**
     * Показывает игровой экран
     */
    private void showGame() {
        gameScreen = new GameScreen();
        appFrame.setContent(gameScreen);

        gameScreen.setEndTurnListener(e -> handleEndTurn());
        gameScreen.setExitListener(e -> handleExit());
        gameScreen.setCellClickListener(this::handleCellClick);
    }

    /**
     * Обработчик клика на клетку
     */
    private void handleCellClick(Integer x, Integer y) {
        if (currentPlayer == null || !currentPlayer.equals(playerName)) {
            gameScreen.setActionStatus("✗ Это не ваш ход!");
            return;
        }

        try {
            String actionData = "{\"x\": " + x + ", \"y\": " + y + ", \"action\": \"move\"}";
            GameMessage moveMessage = new GameMessage(
                    MessageType.PLAYER_ACTION,
                    networkClient.getPlayerId(),
                    actionData
            );

            networkClient.sendMessage(moveMessage);
            gameScreen.setActionStatus("Отправлен ход: (" + x + ", " + y + ")");
        } catch (Exception ex) {
            gameScreen.setActionStatus("✗ Ошибка: " + ex.getMessage());
        }
    }

    /**
     * Обработчик завершения хода
     */
    private void handleEndTurn() {
        if (currentPlayer == null || !currentPlayer.equals(playerName)) {
            gameScreen.setActionStatus("✗ Это не ваш ход!");
            return;
        }

        try {
            String endTurnData = "{\"action\": \"end_turn\"}";
            GameMessage endTurnMessage = new GameMessage(
                    MessageType.PLAYER_ACTION,
                    networkClient.getPlayerId(),
                    endTurnData
            );

            networkClient.sendMessage(endTurnMessage);
            gameScreen.setActionStatus("Ход завершен, ожидаем ответа сервера...");
        } catch (Exception ex) {
            gameScreen.setActionStatus("✗ Ошибка: " + ex.getMessage());
        }
    }

    /**
     * Обработчик выхода
     */
    private void handleExit() {
        networkClient.disconnect();
        System.exit(0);
    }

    /**
     * Обработчик сообщений от сервера
     */
    private void handleMessage(GameMessage message) {
        if (message == null || message.getType() == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                switch (message.getType()) {
                    case GAME_STATE:
                        handleGameState(message);
                        break;

                    case GAME_START:
                        showGame();
                        break;

                    case GAME_END:
                        handleGameEnd(message);
                        break;

                    case ERROR:
                        handleError(message);
                        break;

                    case PLAYER_READY:
                        handlePlayerReady(message);
                        break;
                }
            } catch (Exception ex) {
                System.err.println("Ошибка при обработке сообщения:");
                ex.printStackTrace();
            }
        });
    }

    /**
     * Обработка GAME_STATE
     */
    private void handleGameState(GameMessage message) throws Exception {
        if (message.getData() == null || gameScreen == null) {
            return;
        }

        JsonNode dataNode = objectMapper.readTree(message.getData());

        // Получаем текущего игрока и раунд
        currentPlayer = dataNode.get("currentPlayer").asText();
        currentRound = dataNode.get("round").asInt(0);

        gameScreen.setCurrentPlayer(currentPlayer, currentRound);

        // Обновляем доску
        JsonNode boardNode = dataNode.get("board");
        if (boardNode != null && boardNode.isArray()) {
            String[][] board = new String[9][9];
            for (int y = 0; y < 9; y++) {
                JsonNode row = boardNode.get(y);
                if (row != null && row.isArray()) {
                    for (int x = 0; x < 9; x++) {
                        JsonNode cell = row.get(x);
                        board[y][x] = formatCell(cell);
                    }
                }
            }
            gameScreen.updateBoard(board);
        }

        // Обновляем информацию об игроках
        JsonNode playersNode = dataNode.get("players");
        if (playersNode != null && playersNode.isArray()) {
            String[] playerInfos = new String[playersNode.size()];
            int idx = 0;
            for (JsonNode player : playersNode) {
                String name = player.get("name").asText();
                int gold = player.get("gold").asInt(0);
                playerInfos[idx++] = String.format("%s: %d золота", name, gold);
            }
            gameScreen.updatePlayersInfo(playerInfos);
        }

        // Обновляем статус
        boolean isOurTurn = currentPlayer.equals(playerName);
        if (isOurTurn) {
            gameScreen.setGameStatus("✓ Ваш ход!", true);
        } else {
            gameScreen.setGameStatus("Ход: " + currentPlayer, false);
        }
    }

    /**
     * Форматирование ячейки
     */
    private String formatCell(JsonNode cellNode) {
        if (cellNode == null) {
            return " ";
        }

        try {
            String cellType = cellNode.get("type").asText("GRASS");
            String result = switch (cellType) {
                case "GRASS" -> "G";
                case "WATER" -> "W";
                case "SAND" -> "S";
                case "MOUNTAIN" -> "M";
                case "FORT" -> "F";
                default -> "?";
            };

            if (cellNode.has("pirate") && cellNode.get("pirate").asBoolean(false)) {
                result += "\nP";
            }

            if (cellNode.has("gold") && cellNode.get("gold").asInt(0) > 0) {
                result += "\nG";
            }

            return result;
        } catch (Exception e) {
            return " ";
        }
    }

    /**
     * Обработка PLAYER_READY
     */
    private void handlePlayerReady(GameMessage message) throws Exception {
        if (message.getData() == null || lobbyScreen == null) {
            return;
        }

        JsonNode dataNode = objectMapper.readTree(message.getData());
        String playerName = dataNode.get("playerName").asText();

        lobbyScreen.addPlayer(playerName);
    }

    /**
     * Обработка конца игры
     */
    private void handleGameEnd(GameMessage message) throws Exception {
        JsonNode dataNode = objectMapper.readTree(message.getData());
        String winner = dataNode.get("winner").asText("Неизвестно");
        String endMessage = dataNode.get("message").asText("Игра завершена");

        JOptionPane.showMessageDialog(appFrame,
                endMessage,
                "Победитель: " + winner,
                JOptionPane.INFORMATION_MESSAGE);

        handleExit();
    }

    /**
     * Обработка ошибок
     */
    private void handleError(GameMessage message) throws Exception {
        JsonNode dataNode = objectMapper.readTree(message.getData());
        String errorMsg = dataNode.get("message").asText("Неизвестная ошибка");

        if (gameScreen != null) {
            gameScreen.setActionStatus("✗ Ошибка: " + errorMsg);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameController::new);
    }
}