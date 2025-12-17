package ru.kpfu.itis.jackal.game;

import ru.kpfu.itis.jackal.common.*;
import ru.kpfu.itis.jackal.network.protocol.*;
import ru.kpfu.itis.jackal.server.ClientHandler;
import java.util.*;

/**
 * GameEngine - ФАЗА 2
 * Версия [97] - ПОЛНАЯ РЕАЛИЗАЦИЯ ЭФФЕКТОВ
 */
public class GameEngine {

    private static final String MOVE = "MOVE";
    private static final String ENDTURN = "ENDTURN";

    private GameState gameState;
    private List<ClientHandler> clients;
    private Random random;
    private Map<String, String> playerBeaches;  // ⭐ НОВОЕ: пляж каждого игрока

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
        // 1. Заполняем все клетки морем
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setCell(x, y, new Cell(CellType.SEA, CellContent.EMPTY));
            }
        }

        // 2. Пляжи (углы доски)
        board.setCell(0, 0, new Cell(CellType.BEACH, CellContent.EMPTY));
        board.setCell(8, 0, new Cell(CellType.BEACH, CellContent.EMPTY));
        board.setCell(0, 8, new Cell(CellType.BEACH, CellContent.EMPTY));
        board.setCell(8, 8, new Cell(CellType.BEACH, CellContent.EMPTY));

        // 3. Форт (центр острова)
        Cell fortCell = new Cell(CellType.FORT, CellContent.CANNON);
        fortCell.setRevealed(false);
        fortCell.setVisible(false);
        board.setCell(4, 4, fortCell);

        // 4. Остров - ландшафт (ВСЁ ЗАКРЫТО!)
        for (int x = 1; x < 8; x++) {
            for (int y = 1; y < 8; y++) {
                if (x == 4 && y == 4) continue;
                CellType terrain = getRandomTerrain();
                CellContent content = getRandomContent();
                Cell cell = new Cell(terrain, content);
                cell.setRevealed(false);
                cell.setVisible(false);
                board.setCell(x, y, cell);
            }
        }

        // 5. Пляжи открыты
        board.getCell(0, 0).setRevealed(true);
        board.getCell(0, 0).setVisible(true);
        board.getCell(8, 0).setRevealed(true);
        board.getCell(8, 0).setVisible(true);
        board.getCell(0, 8).setRevealed(true);
        board.getCell(0, 8).setVisible(true);
        board.getCell(8, 8).setRevealed(true);
        board.getCell(8, 8).setVisible(true);
    }

    private CellType getRandomTerrain() {
        double rand = random.nextDouble();
        if (rand < 0.6) return CellType.PLAIN;
        if (rand < 0.8) return CellType.FOREST;
        return CellType.MOUNTAIN;
    }

    private CellContent getRandomContent() {
        double rand = random.nextDouble();
        if (rand < 0.50) return CellContent.EMPTY;
        if (rand < 0.70) return CellContent.GOLD_1;
        if (rand < 0.85) return CellContent.GOLD_2;
        if (rand < 0.95) return CellContent.GOLD_3;
        if (rand < 0.97) return CellContent.TRAP;
        if (rand < 0.985) return CellContent.ARROW_UP;
        if (rand < 0.998) return CellContent.ARROW_DOWN;
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
                    System.out.println("[GameEngine] ⚠️  Неизвестный тип: " + message.getType());
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
        PlayerJoinData joinData = MessageParser.dataFromJson(message.getData(), PlayerJoinData.class);
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
            System.out.println("[GameEngine] 🏖️  Пляж: " + beachKey);
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

        ActionData actionData = MessageParser.dataFromJson(message.getData(), ActionData.class);

        boolean actionProcessed = false;
        if (MOVE.equals(actionData.getActionType())) {
            MoveActionData moveData = MessageParser.dataFromJson(message.getData(), MoveActionData.class);
            actionProcessed = handleMoveAction(moveData, message.getPlayerId());
            if (actionProcessed) {
                checkGameEnd();
                if (!gameState.isGameFinished()) nextTurn();
                broadcastGameState();
            }
        }
        // ⭐ НОВОЕ: Обработка ENDTURN
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

        // Открыть клетку
        if (!toCell.isRevealed()) {
            toCell.reveal();
            toCell.makeVisible();
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

    /**
     * ⭐ ПОЛНАЯ РЕАЛИЗАЦИЯ ЭФФЕКТОВ
     */
    private void handleCellEffects(Cell cell, Pirate pirate, Player player) {
        if (cell == null) return;

        if (cell.hasTrap()) {
            System.out.println("[GameEngine] ⚠️  ЛОВУШКА!");
            returnPirateToShip(pirate, player);
            return;
        }

        if (cell.hasArrow()) {
            Direction dir = getArrowDirection(cell);
            System.out.println("[GameEngine] ↗️  СТРЕЛКА в " + dir);
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

    /**
     * ⭐ ВОЗВРАТ НА ПЛЯЖ
     */
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
            beachCell.setPirate(pirate);
            pirate.setX(beachX);
            pirate.setY(beachY);
            System.out.println("[GameEngine] ✅ На пляж (" + beachX + "," + beachY + ")");
        }
    }

    /**
     * ⭐ ТОЛКАНИЕ ПИРАТА
     */
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
            System.out.println("[GameEngine] ⚠️  За край!");
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

    private Direction getArrowDirection(Cell cell) {
        CellContent content = cell.getContent();
        return switch (content) {
            case ARROW_UP -> Direction.UP;
            case ARROW_DOWN -> Direction.DOWN;
            case ARROW_LEFT -> Direction.LEFT;
            case ARROW_RIGHT -> Direction.RIGHT;
            default -> Direction.UP;
        };
    }

    private boolean isValidMove(Pirate pirate, int toX, int toY) {
        if (toX < 0 || toX >= GameConfig.BOARD_WIDTH || toY < 0 || toY >= GameConfig.BOARD_HEIGHT) return false;
        int distance = Math.abs(pirate.getX() - toX) + Math.abs(pirate.getY() - toY);
        return distance <= 1;
    }

    /**
     * ⭐ БОЙ
     */
    private boolean handleCombat(Pirate attacker, Pirate defender, Cell cell, Player attackerPlayer) {
        System.out.println("[GameEngine] ⚔️  БОЙ!");

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
            if (player.getScore() >= GameConfig.WINNINGSCORE) {
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
        endMessage.setType(MessageType.GAMEEND);
        endMessage.setData(MessageParser.dataToJson(new GameEndData(winner.getId(), winner.getName(), winner.getScore())));
        for (ClientHandler client : clients) {
            client.sendMessage(endMessage);
        }
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
            json.append("{\"id\": \"").append(player.getId()).append("\",")
                    .append("\"name\": \"").append(player.getName()).append("\",")
                    .append("\"ready\": ").append(player.isReady()).append(",")
                    .append("\"score\": ").append(player.getScore()).append("}");
            first = false;
        }
        json.append("],\"currentPlayerId\": \"").append(gameState.getCurrentPlayerId())
                .append("\",\"turnNumber\": ").append(gameState.getTurnNumber())
                .append(",\"board\": ").append(buildBoardJson()).append("}");
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

        if (!cell.isRevealed()) {
            json.append("\"type\": \"HIDDEN\"");
        } else {
            json.append("\"type\": \"").append(cell.getType().name()).append("\",")
                    .append("\"content\": \"").append(cell.getContent().name()).append("\"");
        }

        if (cell.hasPirate()) {
            json.append(",\"pirate\": {\"id\": ").append(cell.getPirate().getId()).append("}");
        }

        if (cell.isRevealed() && cell.hasGold()) {
            json.append(",\"gold\": {\"amount\": ").append(cell.getGold().getAmount()).append("}");
        }

        json.append("}");
        return json.toString();
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
}
