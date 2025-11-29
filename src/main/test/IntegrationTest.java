import org.junit.jupiter.api.Test;
import ru.kpfu.itis.jackal.common.GameState;
import ru.kpfu.itis.jackal.game.GameEngine;
import ru.kpfu.itis.jackal.network.protocol.*;
import ru.kpfu.itis.jackal.server.ClientHandler;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IntegrationTest {

    @Test
    void testFullGameFlow() {
        // Создаем движок игры
        GameEngine gameEngine = new GameEngine();

        // Создаем мок-клиентов
        ClientHandler client1 = mock(ClientHandler.class);
        ClientHandler client2 = mock(ClientHandler.class);

        // Подключаем двух игроков
        connectPlayer(gameEngine, client1, "player1", "Alice", "RED");
        connectPlayer(gameEngine, client2, "player2", "Bob", "BLUE");

        // Проверяем, что оба игрока подключены
        GameState gameState = getGameState(gameEngine);
        assertEquals(2, gameState.getPlayers().size());

        // Отмечаем готовность
        sendReady(gameEngine, client1, "player1");
        sendReady(gameEngine, client2, "player2");

        // Проверяем, что игра началась
        assertTrue(gameState.isGameStarted());
        assertNotNull(gameState.getCurrentPlayerId());

        System.out.println("Интеграционный тест пройден успешно!");
    }

    // Вспомогательные методы
    private void connectPlayer(GameEngine engine, ClientHandler client, String playerId, String name, String color) {
        when(client.getPlayerId()).thenReturn(playerId);

        PlayerJoinData joinData = new PlayerJoinData(name, color);
        GameMessage message = new GameMessage(MessageType.PLAYER_JOIN, playerId,
                MessageParser.dataToJson(joinData));
        engine.processMessage(message, client);
    }

    private void sendReady(GameEngine engine, ClientHandler client, String playerId) {
        GameMessage message = new GameMessage(MessageType.PLAYER_READY, playerId, "{}");
        engine.processMessage(message, client);
    }

    private GameState getGameState(GameEngine engine) {
        try {
            Field field = GameEngine.class.getDeclaredField("gameState");
            field.setAccessible(true);
            return (GameState) field.get(engine);
        } catch (Exception e) {
            fail("Не удалось получить GameState: " + e.getMessage());
            return null;
        }
    }
}