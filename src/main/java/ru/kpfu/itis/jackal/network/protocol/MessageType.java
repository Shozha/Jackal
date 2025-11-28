package ru.kpfu.itis.jackal.network.protocol;

public enum MessageType {
    PLAYER_JOIN,      // Игрок присоединился
    GAME_STATE,       // Обновление состояния игры
    PLAYER_ACTION,    // Действие игрока (ход)
    CHAT_MESSAGE,     // Сообщение в чат
    PLAYER_READY,     // Игрок готов
    GAME_START,       // Начало игры
    GAME_END,
    COMBAT_RESULT,    // Результат боя
    GOLD_UPDATE,       // Изменение золота
    ERROR,
    GOLD_COLLECTED,     // Добавлено для сбора золота
    GOLD_DELIVERED      // Добавлено для доставки золота
}
