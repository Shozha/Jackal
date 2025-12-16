package ru.kpfu.itis.jackal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.kpfu.itis.jackal.common.*;
import ru.kpfu.itis.jackal.game.GameEngine;
import ru.kpfu.itis.jackal.network.protocol.*;
import ru.kpfu.itis.jackal.server.ClientHandler;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameEngineTest {
    private GameEngine gameEngine;

    @Mock
    private ClientHandler mockClientHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gameEngine = new GameEngine();
    }

    @Test
    void testPlayerJoin() {
        // Подготовка тестовых данных
        PlayerJoinData joinData = new PlayerJoinData("TestPlayer", "RED");
        GameMessage message = new GameMessage(MessageType.PLAYER_JOIN, "test123",
                MessageParser.dataToJson(joinData));

        when(mockClientHandler.getPlayerId()).thenReturn("test123");

        // Выполнение
        gameEngine.processMessage(message, mockClientHandler);

        // Проверка
        GameState gameState = getGameState();
        assertNotNull(gameState);
        assertEquals(1, gameState.getPlayers().size());
        assertEquals("TestPlayer", gameState.getPlayers().get(0).getName());
    }

    @Test
    void testDuplicatePlayerJoin() {
        // Первое подключение
        PlayerJoinData joinData1 = new PlayerJoinData("Player1", "RED");
        GameMessage message1 = new GameMessage(MessageType.PLAYER_JOIN, "player1",
                MessageParser.dataToJson(joinData1));
        gameEngine.processMessage(message1, mockClientHandler);

        // Второе подключение с тем же ID
        PlayerJoinData joinData2 = new PlayerJoinData("Player2", "BLUE");
        GameMessage message2 = new GameMessage(MessageType.PLAYER_JOIN, "player1",
                MessageParser.dataToJson(joinData2));

        // Перехватываем отправку ошибки
        gameEngine.processMessage(message2, mockClientHandler);

        // Проверяем, что отправили сообщение об ошибке
        verify(mockClientHandler, atLeastOnce()).sendMessage(any(GameMessage.class));
    }

    @Test
    void testGameStartWithTwoPlayers() {
        // Подключаем двух игроков
        connectTestPlayer("player1", "Player1", "RED");
        connectTestPlayer("player2", "Player2", "BLUE");

        // Отмечаем готовность
        GameMessage ready1 = new GameMessage(MessageType.PLAYER_READY, "player1", "{}");
        GameMessage ready2 = new GameMessage(MessageType.PLAYER_READY, "player2", "{}");

        gameEngine.processMessage(ready1, mockClientHandler);
        gameEngine.processMessage(ready2, mockClientHandler);

        // Проверяем, что игра началась
        GameState gameState = getGameState();
        assertTrue(gameState.isGameStarted());
        assertNotNull(gameState.getCurrentPlayerId());
    }

    // Вспомогательные методы
    private void connectTestPlayer(String playerId, String playerName, String teamColor) {
        PlayerJoinData joinData = new PlayerJoinData(playerName, teamColor);
        GameMessage message = new GameMessage(MessageType.PLAYER_JOIN, playerId,
                MessageParser.dataToJson(joinData));
        when(mockClientHandler.getPlayerId()).thenReturn(playerId);
        gameEngine.processMessage(message, mockClientHandler);
    }

    private GameState getGameState() {
        try {
            Field gameStateField = GameEngine.class.getDeclaredField("gameState");
            gameStateField.setAccessible(true);
            return (GameState) gameStateField.get(gameEngine);
        } catch (Exception e) {
            fail("Не удалось получить GameState: " + e.getMessage());
            return null;
        }
    }
}