### Формат сообщений (JSON) примерный
```json
{
  "type": "PLAYER_JOIN",
  "playerId": "player1",
  "playerName": "Сергей",
  "timestamp": 1635789200000
}
```

```
Jackal/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ru/kpfu/itis/
│   │   │       └── jackal/
│   │   │           ├── server/                       # ТВОЯ ПАПКА
│   │   │           │   ├── GameServer.java           # Главный класс сервера
│   │   │           │   ├── ClientHandler.java        # Обработчик клиента
│   │   │           │   └── ServerController.java     # Контроллер сервера (опционально)
│   │   │           ├── game/                         # ТВОЯ ПАПКА
│   │   │           │   ├── GameEngine.java           # Движок игры
│   │   │           │   ├── rules/
│   │   │           │   │   ├── MovementRules.java    # Правила движения
│   │   │           │   │   ├── CombatRules.java      # Правила боя
│   │   │           │   │   └── GoldRules.java        # Правила золота
│   │   │           │   └── actions/
│   │   │           │       ├── MoveAction.java
│   │   │           │       ├── CombatAction.java
│   │   │           │       └── GoldAction.java
│   │   │           ├── common/                       # ОБЩИЕ КЛАССЫ
│   │   │           │   ├── GameState.java
│   │   │           │   ├── Player.java
│   │   │           │   ├── Board.java
│   │   │           │   ├── Cell.java
│   │   │           │   ├── Pirate.java
│   │   │           │   └── Gold.java
│   │   │           └── network/
│   │   │               └── protocol/
│   │   │                   ├── MessageType.java
│   │   │                   ├── GameMessage.java
│   │   │                   └── MessageParser.java
│   │   └── resources/...
│   └── test/...
└── pom.xml
```