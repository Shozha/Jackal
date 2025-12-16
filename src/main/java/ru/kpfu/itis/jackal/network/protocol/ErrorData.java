package ru.kpfu.itis.jackal.network.protocol;

import lombok.*;

@Setter
@Getter
@Builder
public class ErrorData {
    private String message;
    private String error;
}
