package ru.kpfu.itis.jackal.network.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MessageParser {
    private static final Gson gson = new GsonBuilder().create();

    public static String toJson(GameMessage message) {
        return gson.toJson(message);
    }

    public static GameMessage fromJson(String json) {
        return gson.fromJson(json, GameMessage.class);
    }

    public static String dataToJson(Object data) {
        return gson.toJson(data);
    }

    public static <T> T dataFromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}