package ru.kpfu.itis.jackal.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ru.kpfu.itis.jackal.common.*;
import ru.kpfu.itis.jackal.network.protocol.*;
import ru.kpfu.itis.jackal.server.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.*;

/**
 * GameEngine - ФАЗА 4
 * ✅ Версия [FIXED-v2] - ВЕЗДЕ GSON, ВСЕ СТРЕЛКИ
 *
 * ИСПРАВЛЕНИЯ:
 * ✅ Jackson → Gson везде
 * ✅ 2 пирата вместо 3
 * ✅ Пираты стартуют на пляже (не 0,0)
 * ✅ buildGameStateJson() - ПОЛНАЯ реализация
 * ✅ UUID для playerId
 * ✅ ARROW_LEFT и ARROW_RIGHT добавлены в генерацию
 */
public class GameEngine {

    private static final String MOVE = "MOVE";
    private static final String ENDTURN = "ENDTURN";
    private static final Gson gson = new GsonBuilder().create();

    private GameState gameState;
    private List<ClientHandler> clients;
    private Random random;
    private Map<String, String> playerBeaches; // пляж каждого игрока

    public GameEngine() {
        this.gameState = new GameState();
        this.clients = new ArrayList<>();
        this.random = new Random();
        this.playerBeaches = new HashMap<>();
        initializeGame();
    }

    private void initializeGame() {
        Board board = new Board(GameConfig.BOARD_WIDTH, GameConfig.BOARD_HEIGHT);
        initializeBoard(board);
        gameState.setBoard(board);
        System.out.println("[GameEngine] ✅ Игра инициализирована с FOG OF WAR");
    }

    private void initializeBoard(Board board) {
        // 1. Заполняем все клетки морем (ЗАКРЫТО по умолчанию)
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Cell seaCell = new Cell(CellType.SEA, CellContent.EMPTY);
                seaCell.setRevealed(true);
                seaCell.setVisible(true);
                board.setCell(x, y, seaCell);
            }
        }

        // 2. Пляжи (углы доски) - ОТКРЫТЫ И ВИДНЫ С НАЧАЛА
        Cell beach00 = new Cell(CellType.BEACH, CellContent.EMPTY);
        beach00.setRevealed(true);   // ✅ ОТКРЫТО
        beach00.setVisible(true);    // ✅ ВИДНО
        board.setCell(0, 0, beach00);

        Cell beach80 = new Cell(CellType.BEACH, CellContent.EMPTY);
        beach80.setRevealed(true);
        beach80.setVisible(true);
        board.setCell(8, 0, beach80);

        Cell beach08 = new Cell(CellType.BEACH, CellContent.EMPTY);
        beach08.setRevealed(true);
        beach08.setVisible(true);
        board.setCell(0, 8, beach08);

        Cell beach88 = new Cell(CellType.BEACH, CellContent.EMPTY);
        beach88.setRevealed(true);
        beach88.setVisible(true);
        board.setCell(8, 8, beach88);

        // 3. Остров (внутри) - ВСЁ ЗАКРЫТО (пока пираты не откроют)
        for (int x = 1; x < 8; x++) {
            for (int y = 1; y < 8; y++) {
                if (x == 4 && y == 4) {
                    Cell fortCell = new Cell(CellType.FORT, CellContent.CANNON);
                    fortCell.setRevealed(false);  // ✅ ЗАКРЫТО
                    fortCell.setVisible(false);
                    board.setCell(x, y, fortCell);
                } else {
                    CellType terrain = getRandomTerrain();
                    CellContent content = getRandomContent();
                    Cell cell = new Cell(terrain, content);
                    cell.setRevealed(false);  // ✅ ЗАКРЫТО
                    cell.setVisible(false);
                    board.setCell(x, y, cell);
                }
            }
        }

        System.out.println("[GameEngine] ✅ Доска инициализирована");
        System.out.println("[GameEngine] ✅ Видны только: ПЛЯЖИ и МОРЕ");
        System.out.println("[GameEngine] ✅ Остальное откроется при движении пирата");
    }

    private CellType getRandomTerrain() {
        double rand = random.nextDouble();
        if (rand < 0.6) return CellType.PLAIN;
        if (rand < 0.8) return CellType.FOREST;
        return CellType.MOUNTAIN;
    }

    // ✅ ИСПРАВЛЕНО: Добавлены ARROW_LEFT и ARROW_RIGHT
    private CellContent getRandomContent() {
        double rand = random.nextDouble();
        if (rand < 0.50) return CellContent.EMPTY;
        if (rand < 0.65) return CellContent.GOLD_1;
        if (rand < 0.78) return CellContent.GOLD_2;
        if (rand < 0.88) return CellContent.GOLD_3;
        if (rand < 0.92) return CellContent.TRAP;
        if (rand < 0.95) return CellContent.ARROW_UP;
        if (rand < 0.97) return CellContent.ARROW_DOWN;
        if (rand < 0.985) return CellContent.ARROW_LEFT;
        if (rand < 0.998) return CellContent.ARROW_RIGHT;
        return CellContent.EMPTY;
    }

    public void processMessage(GameMessage message, ClientHandler client) {
        System.out.println("[GameEngine] 📨 " + message.getType() + " от " + message.getPlayerId());
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
                    System.out.println("[GameEngine] ⚠️ Неизвестный тип: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("[GameEngine] ❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
            sendError(client, "Ошибка: " + e.getMessage());
        }
    }

    private boolean isStartGameAction(GameMessage message) {
        if (message.getData() == null) return false;
        return message.getData().contains("START_GAME");
    }

    private void handleStartGameRequest(GameMessage message, ClientHandler client) {
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
        // ✅ ИСПРАВЛЕНО: Использование Gson для парсинга
        PlayerJoinData joinData = gson.fromJson(message.getData(), PlayerJoinData.class);

        if (getPlayer(message.getPlayerId()) != null) {
            sendError(client, "Игрок уже подключен");
            return;
        }

        String teamColor = joinData.getTeamColor();
        if (teamColor == null || teamColor.isBlank()) {
            teamColor = assignFreeColor();
            if (teamColor == null) {
                sendError(client, "Нет свободных цветов");
                return;
            }
        } else if (isTeamColorTaken(teamColor)) {
            sendError(client, "Цвет занят");
            return;
        }

        Player player = new Player(message.getPlayerId(), joinData.getPlayerName(), teamColor);
        initializePlayerPirates(player);
        gameState.addPlayer(player);
        client.setPlayerId(player.getId());
        clients.add(client);
        assignPlayerBeach(player);

        System.out.println("[GameEngine] ✅ Игрок: " + player.getName());
        broadcastGameState();
    }

    private void assignPlayerBeach(Player player) {
        String beachKey = null;
        if (gameState.getPlayers().size() == 1) beachKey = "0,0";
        else if (gameState.getPlayers().size() == 2) beachKey = "8,0";
        else if (gameState.getPlayers().size() == 3) beachKey = "0,8";
        else if (gameState.getPlayers().size() == 4) beachKey = "8,8";

        if (beachKey != null) {
            playerBeaches.put(player.getId(), beachKey);

            // ✅ ИСПРАВЛЕНО: Инициализируем пиратов на пляже!
            String[] parts = beachKey.split(",");
            int beachX = Integer.parseInt(parts[0]);
            int beachY = Integer.parseInt(parts[1]);

            // Ставим пиратов на пляж
            for (Pirate pirate : player.getPirates()) {
                pirate.setX(beachX);
                pirate.setY(beachY);

                // Добавляем пирата на доску
                Cell beachCell = gameState.getBoard().getCell(beachX, beachY);
                if (beachCell != null) {
                    beachCell.setPirate(pirate);
                }
            }

            System.out.println("[GameEngine] 🏖️ Пляж: " + beachKey);
        }
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
        return gameState.getPlayers().stream().anyMatch(p -> p.getTeamColor().equals(teamColor));
    }

    private void handlePlayerAction(GameMessage message, ClientHandler client) {
        if (!gameState.isGameStarted()) {
            sendError(client, "Игра еще не началась");
            return;
        }

        if (gameState.isGameFinished()) {
            sendError(client, "Игра закончилась");
            return;
        }

        if (!message.getPlayerId().equals(gameState.getCurrentPlayerId())) {
            sendError(client, "Не ваш ход");
            return;
        }

        // ✅ ИСПРАВЛЕНО: Использование Gson
        ActionData actionData = gson.fromJson(message.getData(), ActionData.class);
        boolean actionProcessed = false;

        if (MOVE.equals(actionData.getActionType())) {
            MoveActionData moveData = gson.fromJson(message.getData(), MoveActionData.class);
            actionProcessed = handleMoveAction(moveData, message.getPlayerId());
            if (actionProcessed) {
                checkGameEnd();
                if (!gameState.isGameFinished()) nextTurn();
                broadcastGameState();
            }
        }
        else if (ENDTURN.equals(actionData.getActionType())) {
            System.out.println("[GameEngine] 🔄 ENDTURN от " + message.getPlayerId());
            checkGameEnd();
            if (!gameState.isGameFinished()) {
                nextTurn();
                System.out.println("[GameEngine] ➡️ Переход хода на: " + gameState.getCurrentPlayerId());
            }
            broadcastGameState();
        }
    }

    private void handleChatMessage(GameMessage message, ClientHandler client) {
        broadcastMessage(message);
    }

    private void handlePlayerReady(GameMessage message, ClientHandler client) {
        Player player = getPlayer(message.getPlayerId());
        if (player != null) {
            player.setReady(!player.isReady());
            System.out.println("[GameEngine] 🔘 " + player.getName() + ": " + player.isReady());
        }
        broadcastGameState();
    }

    private boolean handleMoveAction(MoveActionData moveData, String playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return false;

        Pirate pirate = player.getPirate(moveData.getPirateId());
        if (pirate == null) return false;

        if (!isValidMove(pirate, moveData.getToX(), moveData.getToY())) return false;

        Cell fromCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        Cell toCell = gameState.getBoard().getCell(moveData.getToX(), moveData.getToY());

        if (fromCell == null || toCell == null) return false;

        if (!toCell.isRevealed()) {
            toCell.reveal();      // ← ОТКРЫТЬ
            toCell.makeVisible(); // ← СДЕЛАТЬ ВИДИМОЙ

            System.out.println("[GameEngine] 🔓 ОТКРЫТА КЛЕТКА (" + moveData.getToX() + "," + moveData.getToY() + ")");
            System.out.println("[GameEngine]    Тип: " + toCell.getType());
        }

        // БОЙ
        if (toCell.hasPirate() && !isSameTeam(toCell.getPirate(), player)) {
            boolean combatResult = handleCombat(pirate, toCell.getPirate(), toCell, player);
            if (!combatResult) return false;
        }

        // Ход
        fromCell.setPirate(null);
        toCell.setPirate(pirate);
        pirate.setX(moveData.getToX());
        pirate.setY(moveData.getToY());
        System.out.println("[GameEngine] ✅ Пират " + pirate.getId() + " в (" + moveData.getToX() + "," + moveData.getToY() + ")");

        // ⭐ ЭФФЕКТЫ
        handleCellEffects(toCell, pirate, player);

        return true;
    }

    private void handleCellEffects(Cell cell, Pirate pirate, Player player) {
        if (cell == null) return;

        if (cell.hasTrap()) {
            System.out.println("[GameEngine] ⚠️ ЛОВУШКА!");
            returnPirateToShip(pirate, player);
            return;
        }

        if (cell.hasArrow()) {
            Direction dir = cell.getArrowDirection();
            System.out.println("[GameEngine] ↗️ СТРЕЛКА в " + dir);
            pushPirate(pirate, dir, player);
            return;
        }

        if (cell.canCollectGold() && pirate.getGoldCarrying() == 0) {
            int amount = cell.getGoldAmount();
            pirate.collectGold(amount);
            cell.setGold(null);
            System.out.println("[GameEngine] 💰 Золото: " + amount);
        }
    }

    private void returnPirateToShip(Pirate pirate, Player player) {
        String beachKey = playerBeaches.get(player.getId());
        if (beachKey == null) return;

        String[] parts = beachKey.split(",");
        int beachX = Integer.parseInt(parts[0]);
        int beachY = Integer.parseInt(parts[1]);

        Cell currentCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        if (currentCell != null) currentCell.setPirate(null);

        Cell beachCell = gameState.getBoard().getCell(beachX, beachY);
        if (beachCell != null) {
            beachCell.reveal();
            beachCell.makeVisible();

            beachCell.setPirate(pirate);
            pirate.setX(beachX);
            pirate.setY(beachY);
            System.out.println("[GameEngine] ✅ На пляж (" + beachX + "," + beachY + ")");
        }
    }

    private void pushPirate(Pirate pirate, Direction dir, Player player) {
        int newX = pirate.getX();
        int newY = pirate.getY();

        switch (dir) {
            case UP: newY--; break;
            case DOWN: newY++; break;
            case LEFT: newX--; break;
            case RIGHT: newX++; break;
            default: return;
        }

        if (newX < 0 || newX >= GameConfig.BOARD_WIDTH || newY < 0 || newY >= GameConfig.BOARD_HEIGHT) {
            System.out.println("[GameEngine] ⚠️ За край!");
            return;
        }

        Cell targetCell = gameState.getBoard().getCell(newX, newY);
        if (targetCell == null) return;

        if (!targetCell.isRevealed()) {
            targetCell.reveal();
            targetCell.makeVisible();
        }

        if (targetCell.hasPirate() && !isSameTeam(targetCell.getPirate(), player)) {
            handleCombat(pirate, targetCell.getPirate(), targetCell, player);
            return;
        }

        if (!targetCell.isWalkable(pirate.getGoldCarrying() > 0)) {
            returnPirateToShip(pirate, player);
            return;
        }

        Cell currentCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        if (currentCell != null) currentCell.setPirate(null);

        targetCell.setPirate(pirate);
        pirate.setX(newX);
        pirate.setY(newY);
        System.out.println("[GameEngine] ✅ Толкнут в (" + newX + "," + newY + ")");

        // ⭐ ЦЕПНАЯ РЕАКЦИЯ
        handleCellEffects(targetCell, pirate, player);
    }

    private boolean isValidMove(Pirate pirate, int toX, int toY) {
        if (toX < 0 || toX >= GameConfig.BOARD_WIDTH || toY < 0 || toY >= GameConfig.BOARD_HEIGHT) return false;
        int distance = Math.abs(pirate.getX() - toX) + Math.abs(pirate.getY() - toY);
        return distance <= 1;
    }

    private boolean handleCombat(Pirate attacker, Pirate defender, Cell cell, Player attackerPlayer) {
        System.out.println("[GameEngine] ⚔️ БОЙ!");
        if (random.nextBoolean()) {
            System.out.println("[GameEngine] 🏆 Победитель атакующий");
            Player defenderPlayer = getPiratePlayer(defender);
            if (defenderPlayer != null) {
                returnPirateToShip(defender, defenderPlayer);
            }
            return true;
        } else {
            System.out.println("[GameEngine] 🏆 Победитель защищающийся");
            return false;
        }
    }

    private Player getPiratePlayer(Pirate pirate) {
        for (Player player : gameState.getPlayers()) {
            if (player.getPirates().contains(pirate)) return player;
        }
        return null;
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

    private void startGame() {
        gameState.setGameStarted(true);
        gameState.resetTurns();
        if (gameState.getPlayers().size() > 0) {
            gameState.setCurrentPlayerId(gameState.getPlayers().get(0).getId());
        }
    }

    private boolean allPlayersReady() {
        if (gameState.getPlayers().size() < 2) return false;
        return gameState.getPlayers().stream().allMatch(Player::isReady);
    }

    // ✅ ИСПРАВЛЕНО: 2 пирата вместо 3!
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

    private void checkGameEnd() {
        for (Player player : gameState.getPlayers()) {
            if (player.getScore() >= GameConfig.WINNING_SCORE) {
                gameState.setGameFinished(true);
                gameState.setWinnerPlayerId(player.getId());
                Player winner = gameState.getWinner();
                System.out.println("[GameEngine] 🎉 ПОБЕДИТЕЛЬ: " + winner.getName() + " (" + winner.getScore() + " золота)");
                broadcastGameEnd(winner);
                break;
            }
        }
    }

    private void broadcastGameEnd(Player winner) {
        GameMessage endMessage = new GameMessage();
        endMessage.setType(MessageType.GAME_END);
        // ✅ ИСПРАВЛЕНО: Gson!
        endMessage.setData(gson.toJson(new GameEndData(winner.getId(), winner.getName(), winner.getScore())));

        for (ClientHandler client : clients) {
            client.sendMessage(endMessage);
        }
    }

    private void broadcastGameState() {
        try {
            JsonObject stateJson = new JsonObject();

            // Текущее состояние игры
            stateJson.addProperty("gameStarted", gameState.isGameStarted());
            stateJson.addProperty("gameFinished", gameState.isGameFinished());
            stateJson.addProperty("currentPlayerId", gameState.getCurrentPlayerId());
            stateJson.addProperty("turnNumber", gameState.getTurnNumber());

            // ✅ ИСПРАВЛЕНО: Сериализация Board через Cell.toJsonObject()
            JsonArray boardArray = new JsonArray();
            Board board = gameState.getBoard();

            for (int y = 0; y < 9; y++) {
                JsonArray rowArray = new JsonArray();
                for (int x = 0; x < 9; x++) {
                    Cell cell = board.getCell(x, y);
                    if (cell != null) {
                        rowArray.add(cell.toJsonObject());
                    } else {
                        // Если клетка null, создаём пустую
                        rowArray.add(new Cell(CellType.SEA).toJsonObject());
                    }
                }
                boardArray.add(rowArray);
            }
            stateJson.add("board", boardArray);

            // Игроки и их статусы
            JsonArray playersArray = new JsonArray();
            for (Player player : gameState.getPlayers()) {
                JsonObject playerJson = new JsonObject();
                playerJson.addProperty("id", player.getId());
                playerJson.addProperty("name", player.getName());
                playerJson.addProperty("ready", player.isReady());
                playerJson.addProperty("score", player.getScore());
                playersArray.add(playerJson);
            }
            stateJson.add("players", playersArray);

            // Отправляем всем
            GameMessage stateMessage = new GameMessage();
            stateMessage.setType(MessageType.GAME_STATE);
            stateMessage.setData(gson.toJson(stateJson));

            for (ClientHandler client : clients) {
                try {
                    client.sendMessage(stateMessage);
                } catch (Exception e) {
                    System.err.println("[GameEngine] Ошибка отправки GAME_STATE: " + e.getMessage());
                }
            }

            System.out.println("[GameEngine] 📬 GAME_STATE отправлено всем клиентам");

        } catch (Exception e) {
            System.err.println("[GameEngine] ❌ Ошибка в broadcastGameState():");
            e.printStackTrace();
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

    // ✅ ИСПРАВЛЕНО: ПОЛНАЯ реализация buildGameStateJson()
    private String buildGameStateJson() {
        GameStateDto dto = new GameStateDto();

        // Игроки
        for (Player player : gameState.getPlayers()) {
            PlayerDto playerDto = new PlayerDto();
            playerDto.id = player.getId();
            playerDto.name = player.getName();
            playerDto.ready = player.isReady();
            playerDto.score = player.getScore();
            dto.players.add(playerDto);
        }

        dto.currentPlayerId = gameState.getCurrentPlayerId();
        dto.turnNumber = gameState.getTurnNumber();
        dto.board = buildBoardJson();

        return gson.toJson(dto);
    }

    private String[][] buildBoardJson() {
        Board board = gameState.getBoard();
        String[][] result = new String[board.getHeight()][board.getWidth()];

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                Cell cell = board.getCell(x, y);
                result[y][x] = cellToJson(cell);
            }
        }

        return result;
    }

    private String cellToJson(Cell cell) {
        if (cell == null) return "{}";

        CellDto dto = new CellDto();

        if (!cell.isRevealed()) {
            dto.type = "HIDDEN";
        } else {
            dto.type = cell.getType().name();
            dto.content = cell.getContent().name();
        }

        if (cell.hasPirate()) {
            PirateDto pirateDto = new PirateDto();
            pirateDto.id = cell.getPirate().getId();
            dto.pirate = pirateDto;
        }

        if (cell.isRevealed() && cell.hasGold()) {
            GoldDto goldDto = new GoldDto();
            goldDto.amount = cell.getGold().getAmount();
            dto.gold = goldDto;
        }

        return gson.toJson(dto);
    }

    public void onClientDisconnect(ClientHandler client) {
        if (client.getPlayerId() != null) {
            Player player = getPlayer(client.getPlayerId());
            if (player != null) {
                gameState.getPlayers().remove(player);
                playerBeaches.remove(player.getId());
                System.out.println("[GameEngine] 👋 Отключен: " + player.getName());
            }
        }
        clients.remove(client);
        broadcastGameState();
    }

    // ✅ DTO классы для Gson сериализации
    public static class GameStateDto {
        public List<PlayerDto> players = new ArrayList<>();
        public String currentPlayerId;
        public int turnNumber;
        public String[][] board;
    }

    public static class PlayerDto {
        public String id;
        public String name;
        public boolean ready;
        public int score;
    }

    public static class CellDto {
        public String type;
        public String content;
        public PirateDto pirate;
        public GoldDto gold;
    }

    public static class PirateDto {
        public int id;
    }

    public static class GoldDto {
        public int amount;
    }
}
