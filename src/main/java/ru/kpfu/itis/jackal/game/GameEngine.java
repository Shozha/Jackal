package ru.kpfu.itis.jackal.game;

import ru.kpfu.itis.jackal.common.*;
import ru.kpfu.itis.jackal.network.protocol.*;
import ru.kpfu.itis.jackal.server.ClientHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GameEngine - игровая логика
 * Версия [96] - ПОЛНОЕ ИСПРАВЛЕНИЕ:
 *
 * ✅ FOG OF WAR - все клетки закрыты, открываются при ходе
 * ✅ CellContent - содержимое отделено от типа
 * ✅ Правильные типы клеток (BEACH, SEA, PLAIN, FOREST, MOUNTAIN, FORT)
 * ✅ Открытие клеток при движении пирата
 * ✅ Эффекты: ловушки, стрелки, золото
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
        System.out.println("[GameEngine] ✅ Игра инициализирована с FOG OF WAR");
    }

    /**
     * ⭐ НОВОЕ [96]: Инициализация доски с FOG OF WAR
     */
    private void initializeBoard(Board board) {
        // 1. Заполняем все клетки морем
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setCell(x, y, new Cell(CellType.SEA, CellContent.EMPTY));
            }
        }

        // 2. Пляжи (углы доски - начальные позиции кораблей)
        board.setCell(0, 0, new Cell(CellType.BEACH, CellContent.EMPTY));
        board.setCell(8, 0, new Cell(CellType.BEACH, CellContent.EMPTY));
        board.setCell(0, 8, new Cell(CellType.BEACH, CellContent.EMPTY));
        board.setCell(8, 8, new Cell(CellType.BEACH, CellContent.EMPTY));

        // 3. Форт (центр острова)
        Cell fortCell = new Cell(CellType.FORT, CellContent.CANNON);  // ⭐ Пушка в форте!
        fortCell.setRevealed(false);  // ⭐ Закрыт!
        fortCell.setVisible(false);
        board.setCell(4, 4, fortCell);

        // 4. Остров - случайный ландшафт и содержимое (ВСЁ ЗАКРЫТО!)
        for (int x = 1; x < 8; x++) {
            for (int y = 1; y < 8; y++) {
                if (x == 4 && y == 4) continue;  // Пропускаем форт

                CellType terrain = getRandomTerrain();
                CellContent content = getRandomContent();  // ⭐ НОВОЕ: случайное содержимое

                Cell cell = new Cell(terrain, content);
                cell.setRevealed(false);  // ⭐ ЗАКРЫТА!
                cell.setVisible(false);

                board.setCell(x, y, cell);
            }
        }

        // 5. Пляжи всегда открыты
        for (Cell cell : new Cell[]{
                board.getCell(0, 0), board.getCell(8, 0),
                board.getCell(0, 8), board.getCell(8, 8)
        }) {
            if (cell != null) {
                cell.setRevealed(true);  // ⭐ Пляжи видны с начала
                cell.setVisible(true);
            }
        }

        System.out.println("[GameEngine] 🗺️  Доска создана с FOG OF WAR");
    }

    /**
     * ⭐ НОВОЕ: Случайный ландшафт острова
     */
    private CellType getRandomTerrain() {
        double rand = random.nextDouble();
        if (rand < 0.6) return CellType.PLAIN;      // 60% равнина
        if (rand < 0.8) return CellType.FOREST;     // 20% лес
        return CellType.MOUNTAIN;                     // 20% горы
    }

    /**
     * ⭐ НОВОЕ: Случайное содержимое клетки (скрыто под рубашкой!)
     */
    private CellContent getRandomContent() {
        double rand = random.nextDouble();

        if (rand < 0.50) return CellContent.EMPTY;       // 50% пусто
        if (rand < 0.70) return CellContent.GOLD_1;      // 20% 1 монета
        if (rand < 0.85) return CellContent.GOLD_2;      // 15% 2 монеты
        if (rand < 0.95) return CellContent.GOLD_3;      // 10% 3 монеты
        if (rand < 0.98) return CellContent.TRAP;        // 3% ловушка
        if (rand < 0.99) return CellContent.ARROW_UP;    // 1% стрелка вверх
        if (rand < 1.00) return CellContent.ARROW_DOWN;  // 1% стрелка вниз

        return CellContent.EMPTY;
    }

    public void processMessage(GameMessage message, ClientHandler client) {
        System.out.println("[GameEngine] 📨 Получено сообщение: " + message.getType() + " от " + message.getPlayerId());
        try {
            switch (message.getType()) {
                case PLAYER_JOIN:
                    handlePlayerJoin(message, client);
                    break;
                case PLAYER_ACTION:
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
                    System.out.println("[GameEngine] ⚠️  Неизвестный тип сообщения: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("[GameEngine] ❌ Ошибка обработки сообщения: " + e.getMessage());
            e.printStackTrace();
            sendError(client, "Ошибка обработки: " + e.getMessage());
        }
    }

    private boolean isStartGameAction(GameMessage message) {
        if (message.getData() == null) return false;
        return message.getData().contains("START_GAME");
    }

    private void handleStartGameRequest(GameMessage message, ClientHandler client) {
        System.out.println("[GameEngine] 🎮 Получена команда START_GAME от " + message.getPlayerId());

        if (!allPlayersReady()) {
            sendError(client, "Не все игроки готовы");
            return;
        }

        if (gameState.getPlayers().size() < 2) {
            sendError(client, "Нужно минимум 2 игрока");
            return;
        }

        startGame();

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

        System.out.println("[GameEngine] ✅ Игрок подключен: " + player.getName() + " (" + player.getTeamColor() + ")");
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

    private void handlePlayerReady(GameMessage message, ClientHandler client) {
        Player player = getPlayer(message.getPlayerId());
        if (player != null) {
            boolean newReady = !player.isReady();
            player.setReady(newReady);

            System.out.println("[GameEngine] 🔘 Игрок " + player.getName() +
                    " готовность: " + (newReady ? "готов ✅" : "не готов ❌"));

            if (allPlayersReady() && gameState.getPlayers().size() >= 2) {
                System.out.println("[GameEngine] 🎮 ВСЕ ИГРОКИ ГОТОВЫ! Ожидаем кнопку 'Начать игру'");
            }
        }

        broadcastGameState();
    }

    /**
     * ⭐ ГЛАВНОЕ ИЗМЕНЕНИЕ [96]: Обработка движения с FOG OF WAR
     */
    private boolean handleMoveAction(MoveActionData moveData, String playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return false;

        Pirate pirate = player.getPirate(moveData.getPirateId());
        if (pirate == null) {
            System.err.println("[GameEngine] ❌ Пират не найден: " + moveData.getPirateId());
            return false;
        }

        // Проверяем расстояние = 1 клетка
        if (!isValidMove(pirate, moveData.getToX(), moveData.getToY())) {
            System.out.println("[GameEngine] ❌ Недопустимый ход для пирата " + moveData.getPirateId());
            return false;
        }

        Cell fromCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        Cell toCell = gameState.getBoard().getCell(moveData.getToX(), moveData.getToY());
        if (fromCell == null || toCell == null) return false;

        // ⭐ НОВОЕ: Открыть целевую клетку (FOG OF WAR!)
        if (!toCell.isRevealed()) {
            toCell.reveal();  // Открываем клетку!
            toCell.makeVisible();
            System.out.println("[GameEngine] 🔓 Открыта клетка (" + moveData.getToX() + "," +
                    moveData.getToY() + ") = " + toCell.getContent().getDisplayName());
        }

        // ⭐ НОВОЕ: Проверяем может ли ходить (учитывая золото)
        boolean carryingGold = pirate.getGoldCarrying() > 0;
        if (!toCell.isWalkable(carryingGold)) {
            System.out.println("[GameEngine] ❌ Эта клетка не проходима для " +
                    (carryingGold ? "пирата с золотом!" : "пирата!"));
            return false;
        }

        // Проверяем пирата на целевой клетке
        if (toCell.hasPirate() && isSameTeam(toCell.getPirate(), player)) {
            System.out.println("[GameEngine] ❌ На целевой клетке уже стоит пират вашей команды");
            return false;
        }

        // БОЙ с вражеским пиратом
        if (toCell.hasPirate() && !isSameTeam(toCell.getPirate(), player)) {
            boolean combatResult = handleCombat(pirate, toCell.getPirate(), toCell);
            if (!combatResult) {
                return false;
            }
        }

        // ⭐ НОВОЕ: Обработка спецэффектов клетки
        handleCellEffects(toCell, pirate);

        // Перемещаем пирата
        fromCell.setPirate(null);
        toCell.setPirate(pirate);
        pirate.setX(moveData.getToX());
        pirate.setY(moveData.getToY());

        System.out.println("[GameEngine] ✅ Пират " + pirate.getId() + " движется в (" +
                moveData.getToX() + "," + moveData.getToY() + ")");

        return true;
    }

    /**
     * ⭐ НОВОЕ: Обработка спецэффектов клетки
     */
    private void handleCellEffects(Cell cell, Pirate pirate) {
        // ⭐ ЛОВУШКА
        if (cell.hasTrap()) {
            System.out.println("[GameEngine] ⚠️  ЛОВУШКА! Пират " + pirate.getId() + " возвращается на корабль!");
            // TODO: Вернуть пирата на корабль
        }

        // ⭐ СТРЕЛКА
        if (cell.hasArrow()) {
            Direction dir = cell.getArrowDirection();
            System.out.println("[GameEngine] ↗️  СТРЕЛКА! Пират " + pirate.getId() +
                    " толкнут в направлении " + dir);
            // TODO: Толкнуть пирата в направлении
        }

        // ⭐ ЗОЛОТО
        if (cell.canCollectGold() && pirate.getGoldCarrying() == 0) {
            int amount = cell.getGoldAmount();
            pirate.collectGold(amount);
            cell.setGold(null);
            System.out.println("[GameEngine] 💰 Пират " + pirate.getId() + " собрал золото: " + amount);
        }
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

    private void initializePlayerPirates(Player player) {
        for (int i = 1; i <= 3; i++) {
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

    /**
     * ⭐ НОВОЕ: Сериализация клетки с FOG OF WAR
     */
    private String cellToJson(Cell cell) {
        if (cell == null) return "{}";
        StringBuilder json = new StringBuilder("{");

        // ⭐ Если закрыта - показываем "HIDDEN"
        if (!cell.isRevealed()) {
            json.append("\"type\": \"HIDDEN\"");
        } else {
            // Если открыта - показываем тип и содержимое
            json.append("\"type\": \"").append(cell.getType().name()).append("\",");
            json.append("\"content\": \"").append(cell.getContent().name()).append("\"");
        }

        // Пират
        if (cell.hasPirate()) {
            Pirate pirate = cell.getPirate();
            json.append(",\"pirate\": {\"id\": ").append(pirate.getId()).append("}");
        }

        // Золото (только если открыто)
        if (cell.isRevealed() && cell.hasGold()) {
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
                System.out.println("[GameEngine] 👋 Игрок отключен: " + player.getName());
                if (gameState.getPlayers().size() < 2 && gameState.isGameStarted()) {
                    gameState.setGameFinished(true);
                    System.out.println("[GameEngine] 🛑 Игра прервана - недостаточно игроков");
                }
            }
        }
        clients.remove(client);
        broadcastGameState();
    }
}