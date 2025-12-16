package ru.kpfu.itis.jackal.game;

import ru.kpfu.itis.jackal.common.*;
import ru.kpfu.itis.jackal.network.protocol.*;
import ru.kpfu.itis.jackal.server.ClientHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        // Инициализация игрового поля
        Board board = new Board(GameConfig.BOARD_WIDTH, GameConfig.BOARD_HEIGHT);
        initializeBoard(board);
        gameState.setBoard(board);

        System.out.println("Игра инициализирована");
    }

    private void initializeBoard(Board board) {
        // Заполняем поле водой
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setCell(x, y, new Cell(CellType.SEA));
            }
        }

        // Создаем остров (7x7 в центре 9x9 поля)
        for (int x = 1; x < 8; x++) {
            for (int y = 1; y < 8; y++) {
                // Случайный рельеф для разнообразия
                CellType terrain = getRandomTerrain();
                board.setCell(x, y, new Cell(terrain));
            }
        }

        // Устанавливаем пляжи для команд
        board.setCell(0, 0, new Cell(CellType.BEACH_RED));
        board.setCell(8, 0, new Cell(CellType.BEACH_BLUE));
        board.setCell(0, 8, new Cell(CellType.BEACH_GREEN));
        board.setCell(8, 8, new Cell(CellType.BEACH_YELLOW));

        // Форт в центре
        board.setCell(4, 4, new Cell(CellType.FORT));

        // Размещаем золото на поле
        initializeGold(board);
    }

    private CellType getRandomTerrain() {
        double rand = random.nextDouble();
        if (rand < 0.6) return CellType.PLAIN;    // 60% равнины
        if (rand < 0.8) return CellType.FOREST;   // 20% лес
        return CellType.MOUNTAIN;                 // 20% горы
    }

    private void initializeGold(Board board) {
        // Размещаем золото разных номиналов в случайных местах на острове
        int[] goldAmounts = GameConfig.GOLD_VALUES;

        for (int amount : goldAmounts) {
            for (int i = 0; i < 2; i++) { // По 2 золота каждого номинала
                placeGoldRandomly(board, amount);
            }
        }
    }

    private void placeGoldRandomly(Board board, int amount) {
        int attempts = 0;
        while (attempts < 50) { // Максимум 50 попыток
            int x = random.nextInt(7) + 1; // 1-7
            int y = random.nextInt(7) + 1; // 1-7

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
                    handlePlayerAction(message, client);
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

    private void handlePlayerJoin(GameMessage message, ClientHandler client) {
        // Парсим данные из JSON
        PlayerJoinData joinData = MessageParser.dataFromJson(message.getData(), PlayerJoinData.class);

        // Проверяем, не подключен ли уже игрок
        if (getPlayer(message.getPlayerId()) != null) {
            sendError(client, "Игрок с ID " + message.getPlayerId() + " уже подключен");
            return;
        }

        // Проверяем, не занят ли цвет команды
        if (isTeamColorTaken(joinData.getTeamColor())) {
            sendError(client, "Цвет команды " + joinData.getTeamColor() + " уже занят");
            return;
        }

        // Создаем игрока
        Player player = new Player(message.getPlayerId(), joinData.getPlayerName(), joinData.getTeamColor());
        initializePlayerPirates(player);

        // Добавляем в игру
        gameState.addPlayer(player);
        client.setPlayerId(player.getId());
        clients.add(client);

        System.out.println("Игрок подключен: " + player.getName() + " (" + player.getTeamColor() + ")");
        broadcastGameState();
    }

    private boolean isTeamColorTaken(String teamColor) {
        return gameState.getPlayers().stream()
                .anyMatch(p -> p.getTeamColor().equals(teamColor));
    }

    private void handlePlayerAction(GameMessage message, ClientHandler client) {
        // Если игра еще не началась, игнорируем действия
        if (!gameState.isGameStarted()) {
            sendError(client, "Игра еще не началась");
            return;
        }

        // Если игра уже завершена, игнорируем действия
        if (gameState.isGameFinished()) {
            sendError(client, "Игра уже завершена");
            return;
        }

        // Проверяем очередь хода
        if (!message.getPlayerId().equals(gameState.getCurrentPlayerId())) {
            sendError(client, "Сейчас не ваш ход");
            return;
        }

        // Определяем тип действия и парсим данные
        ActionData actionData = MessageParser.dataFromJson(message.getData(), ActionData.class);

        boolean actionProcessed = false;

        if ("MOVE".equals(actionData.getActionType())) {
            MoveActionData moveData = MessageParser.dataFromJson(message.getData(), MoveActionData.class);
            actionProcessed = handleMoveAction(moveData, message.getPlayerId());
        }
        // TODO: Добавить другие типы действий (COLLECT_GOLD, COMBAT, etc.)

        if (actionProcessed) {
            checkGameEnd();
            if (!gameState.isGameFinished()) {
                nextTurn();
            }
            broadcastGameState();
        }
    }

    private void handleChatMessage(GameMessage message, ClientHandler client) {
        // Рассылаем сообщение чата всем клиентам
        broadcastMessage(message);
    }

    private void handlePlayerReady(GameMessage message, ClientHandler client) {
        Player player = getPlayer(message.getPlayerId());
        if (player != null) {
            player.setReady(true);
            System.out.println("Игрок " + player.getName() + " готов");
        }

        // Если все готовы и минимум 2 игрока, начинаем игру
        if (allPlayersReady() && gameState.getPlayers().size() >= 2) {
            startGame();
        }

        broadcastGameState();
    }

    // === ОСНОВНЫЕ МЕТОДЫ ЛОГИКИ ИГРЫ ===

    private boolean handleMoveAction(MoveActionData moveData, String playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return false;

        Pirate pirate = player.getPirate(moveData.getPirateId());
        if (pirate == null) {
            System.err.println("Пират не найден: " + moveData.getPirateId());
            return false;
        }

        // Проверяем валидность хода
        if (!isValidMove(pirate, moveData.getToX(), moveData.getToY())) {
            System.out.println("Недопустимый ход для пирата " + moveData.getPirateId());
            return false;
        }

        // Получаем клетки
        Cell fromCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        Cell toCell = gameState.getBoard().getCell(moveData.getToX(), moveData.getToY());

        if (fromCell == null || toCell == null) return false;

        // Проверяем, не стоит ли на целевой клетке пират той же команды
        if (toCell.hasPirate() && isSameTeam(toCell.getPirate(), player)) {
            System.out.println("На целевой клетке уже стоит пират вашей команды");
            return false;
        }

        // Проверяем бой, если на целевой клетке есть чужой пират
        if (toCell.hasPirate() && !isSameTeam(toCell.getPirate(), player)) {
            boolean combatResult = handleCombat(pirate, toCell.getPirate(), toCell);
            if (!combatResult) {
                return false; // Пират проиграл бой и не перемещается
            }
        }

        // Перемещаем пирата
        fromCell.setPirate(null);
        toCell.setPirate(pirate);
        pirate.setX(moveData.getToX());
        pirate.setY(moveData.getToY());

        // Проверяем сбор золота
        if (toCell.hasGold() && pirate.getGoldCarrying() == 0) {
            collectGold(pirate, toCell);
        }

        // Проверяем доставку золота на корабль
        if (pirate.getGoldCarrying() > 0 && isOnShip(pirate, player)) {
            deliverGold(pirate, player);
        }

        System.out.println("Пират " + moveData.getPirateId() + " перемещен в (" + moveData.getToX() + "," + moveData.getToY() + ")");
        return true;
    }

    private boolean isValidMove(Pirate pirate, int toX, int toY) {
        // Проверяем границы поля
        if (!gameState.getBoard().isValidPosition(toX, toY)) {
            return false;
        }

        // Проверяем, что клетка проходима
        Cell toCell = gameState.getBoard().getCell(toX, toY);
        if (toCell == null || !toCell.isPassable()) {
            return false;
        }

        // Проверяем расстояние (только соседние клетки)
        int dx = Math.abs(pirate.getX() - toX);
        int dy = Math.abs(pirate.getY() - toY);

        return (dx <= 1 && dy <= 1) && (dx + dy > 0);
    }

    private boolean isSameTeam(Pirate pirate, Player player) {
        // Находим владельца пирата
        for (Player p : gameState.getPlayers()) {
            if (p.getPirates().contains(pirate)) {
                return p.getId().equals(player.getId());
            }
        }
        return false;
    }

    private boolean handleCombat(Pirate attacker, Pirate defender, Cell combatCell) {
        System.out.println("Бой между пиратами!");

        // Простая логика боя - случайный результат
        boolean attackerWins = random.nextBoolean();

        Player attackerPlayer = findPlayerByPirate(attacker);
        Player defenderPlayer = findPlayerByPirate(defender);

        if (attackerWins) {
            // Атакующий побеждает
            returnPirateToShip(defender, defenderPlayer);
            combatCell.setPirate(attacker);
            System.out.println("Пират " + attacker.getId() + " победил в бою");
            return true;
        } else {
            // Защитник побеждает
            returnPirateToShip(attacker, attackerPlayer);
            System.out.println("Пират " + defender.getId() + " победил в бою");
            return false;
        }
    }

    private Player findPlayerByPirate(Pirate pirate) {
        return gameState.getPlayers().stream()
                .filter(p -> p.getPirates().contains(pirate))
                .findFirst()
                .orElse(null);
    }

    private void returnPirateToShip(Pirate pirate, Player player) {
        // Возвращаем пирата на корабль
        Cell currentCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        if (currentCell != null) {
            currentCell.setPirate(null);
        }

        // Теряем золото при возврате на корабль
        if (pirate.getGoldCarrying() > 0) {
            System.out.println("Пират теряет " + pirate.getGoldCarrying() + " золота при отступлении");
            pirate.setGoldCarrying(0);
        }

        // Устанавливаем позицию пирата на корабле
        pirate.setX(player.getShip().getX());
        pirate.setY(player.getShip().getY());

        // Размещаем пирата на клетке корабля
        Cell shipCell = gameState.getBoard().getCell(pirate.getX(), pirate.getY());
        if (shipCell != null) {
            shipCell.setPirate(pirate);
        }
    }

    private void collectGold(Pirate pirate, Cell cell) {
        Gold gold = cell.getGold();
        pirate.setGoldCarrying(gold.getAmount());
        cell.setGold(null);

        System.out.println("Пират " + pirate.getId() + " собрал " + gold.getAmount() + " золота");
    }

    private boolean isOnShip(Pirate pirate, Player player) {
        return pirate.getX() == player.getShip().getX() &&
                pirate.getY() == player.getShip().getY();
    }

    private void deliverGold(Pirate pirate, Player player) {
        int goldAmount = pirate.getGoldCarrying();
        player.getShip().addGold(goldAmount);
        player.addGoldToScore(goldAmount);
        pirate.setGoldCarrying(0);

        System.out.println("Игрок " + player.getName() + " доставил " + goldAmount + " золота на корабль. Всего очков: " + player.getScore());
    }

    private void checkGameEnd() {
        for (Player player : gameState.getPlayers()) {
            if (player.getScore() >= GameConfig.WINNING_SCORE) {
                gameState.setGameFinished(true);
                gameState.setWinnerPlayerId(player.getId()); // Теперь этот метод существует

                Player winner = gameState.getWinner();
                System.out.println("=== ИГРА ОКОНЧЕНА ===");
                System.out.println("Победитель: " + winner.getName() + " с " + winner.getScore() + " очками!");

                // Рассылаем специальное сообщение о победе
                broadcastGameEnd(winner);
                break;
            }
        }
    }

    private void broadcastGameEnd(Player winner) {
        GameMessage endMessage = new GameMessage();
        endMessage.setType(MessageType.GAME_END);
        endMessage.setData(MessageParser.dataToJson(new GameEndData(winner.getId(), winner.getName(), winner.getScore())));

        for (ClientHandler client : clients) {
            client.sendMessage(endMessage);
        }
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private Player getPlayer(String playerId) {
        return gameState.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private void initializePlayerPirates(Player player) {
        // Определяем стартовую позицию в зависимости от цвета команды
        int startX = 0, startY = 0;
        switch (player.getTeamColor()) {
            case "RED": startX = 0; startY = 0; break;
            case "BLUE": startX = 8; startY = 0; break;
            case "GREEN": startX = 0; startY = 8; break;
            case "YELLOW": startX = 8; startY = 8; break;
        }

        // Создаем корабль
        Ship ship = new Ship(player.getId(), startX, startY);
        player.setShip(ship);

        // Создаем двух пиратов на разных клетках рядом с кораблем
        Pirate pirate1 = new Pirate(1, startX, startY);

        // Второго пирата размещаем на соседней клетке
        int pirate2X = startX;
        int pirate2Y = startY;
        if (startX == 0) pirate2X = 1;
        else if (startX == 8) pirate2X = 7;
        if (startY == 0) pirate2Y = 1;
        else if (startY == 8) pirate2Y = 7;

        Pirate pirate2 = new Pirate(2, pirate2X, pirate2Y);

        player.addPirate(pirate1);
        player.addPirate(pirate2);

        // Размещаем пиратов на поле
        Cell shipCell = gameState.getBoard().getCell(startX, startY);
        Cell pirate2Cell = gameState.getBoard().getCell(pirate2X, pirate2Y);

        if (shipCell != null) {
            shipCell.setPirate(pirate1); // Первый пират на корабле
        }
        if (pirate2Cell != null) {
            pirate2Cell.setPirate(pirate2); // Второй пират на соседней клетке
        }

        System.out.println("Созданы пираты для игрока " + player.getName() +
                ": пират1 на (" + startX + "," + startY + "), " +
                "пират2 на (" + pirate2X + "," + pirate2Y + ")");
    }

    private void nextTurn() {
        List<Player> players = gameState.getPlayers();
        if (players.isEmpty()) return;

        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(gameState.getCurrentPlayerId())) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % players.size();
        gameState.setCurrentPlayerId(players.get(nextIndex).getId());
        gameState.setTurnNumber(gameState.getTurnNumber() + 1);

        System.out.println("Ход перешел к игроку: " + players.get(nextIndex).getName());
    }

    private void sendError(ClientHandler client, String errorMessage) {
        GameMessage errorMsg = new GameMessage();
        errorMsg.setType(MessageType.ERROR);
        errorMsg.setData(MessageParser.dataToJson(
            ErrorData.builder()
                .error("ERROR")
                .message(errorMessage)
                .build()
        ));
        client.sendMessage(errorMsg);
    }

    private void broadcastGameState() {
        GameMessage message = new GameMessage();
        message.setType(MessageType.GAME_STATE);
        message.setData(MessageParser.dataToJson(gameState));

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void broadcastMessage(GameMessage message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private boolean allPlayersReady() {
        return gameState.getPlayers().stream().allMatch(Player::isReady);
    }

    private void startGame() {
        // Начинаем игру
        gameState.setGameStarted(true);

        // Устанавливаем первого игрока
        if (!gameState.getPlayers().isEmpty()) {
            gameState.setCurrentPlayerId(gameState.getPlayers().get(0).getId());
        }

        gameState.setTurnNumber(1);
        System.out.println("=== ИГРА НАЧАЛАСЬ ===");
        System.out.println("Первый ход у: " + gameState.getPlayers().get(0).getName());

        broadcastGameState();
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getPlayerId() != null) {
            Player player = getPlayer(client.getPlayerId());
            if (player != null) {
                gameState.getPlayers().remove(player);
                System.out.println("Игрок отключен: " + player.getName());

                // Если игроков осталось меньше 2, заканчиваем игру
                if (gameState.getPlayers().size() < 2 && gameState.isGameStarted()) {
                    gameState.setGameFinished(true);
                    System.out.println("Игра прервана - недостаточно игроков");
                }
            }
        }
    }
}