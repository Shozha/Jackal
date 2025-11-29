public class SimpleClientTest {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Использование: SimpleClientTest <playerId> <playerName> <teamColor>");
            System.out.println("Пример: player1 Сергей RED");
            return;
        }

        String playerId = args[0];
        String playerName = args[1];
        String teamColor = args[2];

        TestClient client = new TestClient(playerId, playerName, teamColor);
        client.connect("localhost", 8888);
    }
}