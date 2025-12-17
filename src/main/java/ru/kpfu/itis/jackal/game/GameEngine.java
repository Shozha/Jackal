package ru.kpfu.itis.jackal.game;

import ru.kpfu.itis.jackal.common.*;
import ru.kpfu.itis.jackal.network.protocol.*;
import ru.kpfu.itis.jackal.server.ClientHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GameEngine - игровая логика
 * Версия [94] - ИСПРАВЛЕНО:
 *
 * ✅ START_GAME обрабатывается отдельно (не как PLAYER_ACTION)
 * ✅ Нет проверки "сейчас не ваш ход" при START_GAME
 * ✅ Игра запускается корректно
 */
public class GameEngine {

    private GameState gameState;
    private List<ClientHandler> clients;
    private Random random;

    public GameEngine() {
        this.gameState = new GameState();
        this.clients = new ArrayList<>();
        this.random = new Random();
        initializeGame();
    }

    private void initializeGame() {
        Board board = new Board(GameConfig.BOARD_WIDTH, GameConfig.BOARD_HEIGHT);
        initializeBoard(board);
        gameState.setBoard(board);
        System.out.println("Игра инициализирована");
    }

    private void initializeBoard(Board board) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setCell(x, y, new Cell(CellType.SEA));
            }
        }

        for (int x = 1; x < 8; x++) {
            for (int y = 1; y < 8; y++) {
                CellType terrain = getRandomTerrain();
                board.setCell(x, y, new Cell(terrain));
            }
        }

        board.setCell(0, 0, new Cell(CellType.BEACH_RED));
        board.setCell(8, 0, new Cell(CellType.BEACH_BLUE));
        board.setCell(0, 8, new Cell(CellType.BEACH_GREEN));
        board.setCell(8, 8, new Cell(CellType.BEACH_YELLOW));

        board.setCell(4, 4, new Cell(CellType.FORT));

        initializeGold(board);
    }

    private CellType getRandomTerrain() {
        double rand = random.nextDouble();
        if (rand < 0.6) return CellType.PLAIN;
        if (rand < 0.8) return CellType.FOREST;
        return CellType.MOUNTAIN;
    }

    private void initializeGold(Board board) {
        int[] goldAmounts = GameConfig.GOLD_VALUES;
        for (int amount : goldAmounts) {
            for (int i = 0; i < 2; i++) {
                placeGoldRandomly(board, amount);
            }
        }
    }

    private void placeGoldRandomly(Board board, int amount) {
        int attempts = 0;
        while (attempts < 50) {
            int x = random.nextInt(7) + 1;
            int y = random.nextInt(7) + 1;
            Cell cell = board.getCell(x, y);
            if (cell != null && !cell.hasGold() && cell.getType() != CellType.FORT) {
                cell.setGold(new Gold(amount, x, y));
                System.out.println("Размещено золото " + amount + " в (" + x + "," + y + ")");
                return;
            }
            attempts++;
        }
    }

    public void processMessage(GameMessage message, ClientHandler client) {
        System.out.println("Получено сообщение: " + message.getType() + " от " + message.getPlayerId());
        try {
            switch (message.getType()) {
                case PLAYER_JOIN:
                    handlePlayerJoin(message, client);
                    break;
                case PLAYER_ACTION:
                    // ⭐ НОВОЕ: проверяем если это START_GAME
                    if (isStartGameAction(message)) {
                        handleStartGameRequest(message, client);
                    } else {
                        handlePlayerAction(message, client);
                    }
                    break;
                case CHAT_MESSAGE:
                    handleChatMessage(message, client);
                    break;
                case PLAYER_READY:
                    handlePlayerReady(message, client);
                    break;
                default:
                    System.out.println("Неизвестный тип сообщения: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки сообщения: " + e.getMessage());
            sendError(client, "Ошибка обработки: " + e.getMessage());
        }
    }

    /**
     * ⭐ НОВОЕ: проверяем если это START_GAME
     */
    private boolean isStartGameAction(GameMessage message) {
        if (message.getData() == null) return false;
        return message.getData().contains("START_GAME");
    }

    /**
     * ⭐ НОВОЕ: обработка START_GAME отдельно
     */
    private void handleStartGameRequest(GameMessage message, ClientHandler client) {
        System.out.println("[GameEngine] 🎮 Получена команда START_GAME от " + message.getPlayerId());

        // Проверяем что все готовы
        if (!allPlayersReady()) {
            sendError(client, "Не все игроки готовы");
            return;
        }

        if (gameState.getPlayers().size() < 2) {
            sendError(client, "Нужно минимум 2 игрока");
            return;
        }

        // Запускаем игру
        startGame();

        // Отправляем всем GAME_START
        GameMessage startMessage = new GameMessage();
        startMessage.setType(MessageType.GAME_START);
        startMessage.setData("{\"status\": \"game_started\"}");

        for (ClientHandler ch : clients) {
            ch.sendMessage(startMessage);
        }

        System.out.println("[GameEngine] 🎮 ИГРА ЗАПУЩЕНА!");
        broadcastGameState();
    }

    private void handlePlayerJoin(GameMessage message, ClientHandler client) {
        PlayerJoinData joinData = MessageParser.dataFromJson(message.getData(), PlayerJoinData.class);
        if (getPlayer(message.getPlayerId()) != null) {
            sendError(client, "Игрок с ID " + message.getPlayerId() + " уже подключен");
            return;
        }

        String requestedColor = joinData.getTeamColor();
        String teamColor = requestedColor;
        if (teamColor == null || teamColor.isBlank()) {
            teamColor = assignFreeColor();
            if (teamColor == null) {
                sendError(client, "Нет свободных цветов команд");
                return;
            }
        } else if (isTeamColorTaken(teamColor)) {
            sendError(client, "Цвет команды " + teamColor + " уже занят");
            return;
        }

        Player player = new Player(message.getPlayerId(), joinData.getPlayerName(), teamColor);
        initializePlayerPirates(player);
        gameState.addPlayer(player);
        client.setPlayerId(player.getId());
        clients.add(client);

        System.out.println("Игрок подключен: " + player.getName() + " (" + player.getTeamColor() + ")");
        broadcastGameState();
    }

    private String assignFreeColor() {
        String[] colors = {"RED", "BLUE", "GREEN", "YELLOW"};
        for (String c : colors) {
            if (!isTeamColorTaken(c)) return c;
        }
        return null;
    }

    private boolean isTeamColorTaken(String teamColor) {
        if (teamColor == null) return false;
        return gameState.getPlayers().stream()
                .anyMatch(p -> p.getTeamColor().equals(teamColor));
    }

    private void handlePlayerAction(GameMessage message, ClientHandler client) {
        if (!gameState.isGameStarted()) {
            sendError(client, "Игра еще не началась");
            return;
        }

        if (gameState.isGameFinished()) {
            sendError(client, "Игра уже завершена");
            return;
        }

        if (!message.getPlayerId().equals(gameState.getCurrentPlayerId())) {
            sendError(client, "Сейчас не ваш ход");
            return;
        }

        ActionData actionData = MessageParser.dataFromJson(message.getData(), ActionData.class);
        boolean actionProcessed = false;
        if ("MOVE".equals(actionData.getActionType())) {
            MoveActionData moveData = MessageParser.dataFromJson(message.getData(), MoveActionData.class);
            actionProcessed = handleMoveAction(moveData, message.getPlayerId());
        }

        if (actionProcessed) {
            checkGameEnd();
            if (!gameState.isGameFinished()) {
                nextTurn();
            }
        }
        broadcastGameState();
    }

    private void handleChatMessage(GameMessage message, ClientHandler client) {
        broadcastMessage(message);
    }

    /**
     * ⭐ ГЛАВНОЕ ИСПРАВЛЕНИЕ [91]: toggle() вместо setReady(true)
     */
    private void handlePlayerReady(GameMessage message, ClientHandler client) {
        Player player = getPlayer(message.getPlayerId());
        if (player != null) {
            // ⭐ НОВОЕ: переключаем статус вместо установки true
            boolean newReady = !player.isReady();
            player.setReady(newReady);

            System.out.println("[GameEngine] 🔘 Игрок " + player.getName() +
                    " готовность: " + (newReady ? "готов" : "не готов"));

            // Если все готовы и минимум 2 игрока, начинаем игру
            if (allPlayersReady() && gameState.getPlayers().size() >= 2) {
                System.out.println("[GameEngine] 🎮 ВСЕ ИГРОКИ ГОТОВЫ! Ожидаем кнопку 'Начать игру'");
            }
        }

        broadcastGameState();
    }

    private boolean handleMoveAction(MoveActionData moveData, String playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return false;
        Pirate pirate = player.getPirate(moveData.getPirateId());
        if (pirate == null) {
            System.err.println("Пират не найден: " + moveData.getPirateId());
            return false;
        }

        if (!isValidMove(pirate, moveData.getToX(), moveData.getToY())) {
            System.out.println("Недопустимый ход для пирата " + moveData.getPirateId());
            return false;
        }

        Cell fromCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        Cell toCell = gameState.getBoard().getCell(moveData.getToX(), moveData.getToY());
        if (fromCell == null || toCell == null) return false;

        if (toCell.hasPirate() && isSameTeam(toCell.getPirate(), player)) {
            System.out.println("На целевой клетке уже стоит пират вашей команды");
            return false;
        }

        if (toCell.hasPirate() && !isSameTeam(toCell.getPirate(), player)) {
            boolean combatResult = handleCombat(pirate, toCell.getPirate(), toCell);
            if (!combatResult) {
                return false;
            }
        }

        fromCell.setPirate(null);
        toCell.setPirate(pirate);
        pirate.setX(moveData.getToX());
        pirate.setY(moveData.getToY());

        if (toCell.hasGold() && pirate.getGoldCarrying() == 0) {
            collectGold(pirate, toCell);
        }

        return true;
    }

    private boolean isValidMove(Pirate pirate, int toX, int toY) {
        if (toX < 0 || toX >= GameConfig.BOARD_WIDTH || toY < 0 || toY >= GameConfig.BOARD_HEIGHT) {
            return false;
        }

        int distance = Math.abs(pirate.getX() - toX) + Math.abs(pirate.getY() - toY);
        return distance <= 1;
    }

    private boolean handleCombat(Pirate attacker, Pirate defender, Cell cell) {
        if (random.nextBoolean()) {
            cell.setPirate(null);
            return true;
        } else {
            return false;
        }
    }

    private void collectGold(Pirate pirate, Cell cell) {
        Gold gold = cell.getGold();
        if (gold != null) {
            pirate.collectGold(gold.getAmount());
            cell.setGold(null);
            System.out.println("Пират собрал золото: " + gold.getAmount());
        }
    }

    private boolean isSameTeam(Pirate pirate, Player player) {
        return player.getPirates().contains(pirate);
    }

    private void nextTurn() {
        List<Player> players = gameState.getPlayers();
        if (players.isEmpty()) return;

        int currentIndex = -1;
        String currentPlayerId = gameState.getCurrentPlayerId();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(currentPlayerId)) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % players.size();
        gameState.setCurrentPlayerId(players.get(nextIndex).getId());
        gameState.nextTurn();
    }

    private void checkGameEnd() {
        // TODO: Реализовать условия окончания игры
    }

    private void startGame() {
        gameState.setGameStarted(true);
        gameState.resetTurns();
        if (gameState.getPlayers().size() > 0) {
            gameState.setCurrentPlayerId(gameState.getPlayers().get(0).getId());
        }
        System.out.println("[GameEngine] ✅ Игра запущена! Первый ход: " + gameState.getCurrentPlayerId());
    }

    private boolean allPlayersReady() {
        if (gameState.getPlayers().size() < 2) return false;
        return gameState.getPlayers().stream().allMatch(Player::isReady);
    }

    /**
     * ⭐ ИСПРАВЛЕН конструктор Pirate [91]
     */
    private void initializePlayerPirates(Player player) {
        for (int i = 1; i <= 3; i++) {
            // ⭐ НОВОЕ: передаем int ID (не String!)
            Pirate pirate = new Pirate(i, 0, 0);
            player.addPirate(pirate);
        }
    }

    private Player getPlayer(String playerId) {
        return gameState.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public void broadcastGameState() {
        GameMessage stateMessage = new GameMessage();
        stateMessage.setType(MessageType.GAME_STATE);
        stateMessage.setData(buildGameStateJson());

        for (ClientHandler client : clients) {
            client.sendMessage(stateMessage);
        }
    }

    public void broadcastMessage(GameMessage message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void sendError(ClientHandler client, String errorMsg) {
        GameMessage errorMessage = new GameMessage();
        errorMessage.setType(MessageType.ERROR);
        errorMessage.setData("{\"error\": \"" + errorMsg + "\"}");
        client.sendMessage(errorMessage);
    }

    private String buildGameStateJson() {
        StringBuilder json = new StringBuilder("{");
        json.append("\"players\": [");
        boolean first = true;
        for (Player player : gameState.getPlayers()) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"id\": \"").append(player.getId()).append("\",");
            json.append("\"name\": \"").append(player.getName()).append("\",");
            json.append("\"ready\": ").append(player.isReady()).append(",");
            json.append("\"score\": ").append(player.getScore());
            json.append("}");
            first = false;
        }
        json.append("],");
        json.append("\"currentPlayerId\": \"").append(gameState.getCurrentPlayerId()).append("\",");
        json.append("\"turnNumber\": ").append(gameState.getTurnNumber()).append(",");
        json.append("\"board\": ").append(buildBoardJson());
        json.append("}");
        return json.toString();
    }

    private String buildBoardJson() {
        StringBuilder json = new StringBuilder("[");
        Board board = gameState.getBoard();
        for (int y = 0; y < board.getHeight(); y++) {
            if (y > 0) json.append(",");
            json.append("[");
            for (int x = 0; x < board.getWidth(); x++) {
                if (x > 0) json.append(",");
                Cell cell = board.getCell(x, y);
                json.append(cellToJson(cell));
            }
            json.append("]");
        }
        json.append("]");
        return json.toString();
    }

    private String cellToJson(Cell cell) {
        if (cell == null) return "{}";
        StringBuilder json = new StringBuilder("{");
        json.append("\"type\": \"").append(cell.getType().name()).append("\"");
        if (cell.hasPirate()) {
            Pirate pirate = cell.getPirate();
            json.append(",\"pirate\": {\"id\": ").append(pirate.getId()).append("}");
        }
        if (cell.hasGold()) {
            Gold gold = cell.getGold();
            json.append(",\"gold\": {\"amount\": ").append(gold.getAmount()).append("}");
        }
        json.append("}");
        return json.toString();
    }

    public void onClientDisconnect(ClientHandler client) {
        if (client.getPlayerId() != null) {
            Player player = getPlayer(client.getPlayerId());
            if (player != null) {
                gameState.getPlayers().remove(player);
                System.out.println("Игрок отключен: " + player.getName());
                if (gameState.getPlayers().size() < 2 && gameState.isGameStarted()) {
                    gameState.setGameFinished(true);
                    System.out.println("Игра прервана - недостаточно игроков");
                }
            }
        }
        clients.remove(client);
        broadcastGameState();
    }
}
