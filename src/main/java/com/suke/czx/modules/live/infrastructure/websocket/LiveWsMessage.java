package com.suke.czx.modules.live.infrastructure.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveWsMessage<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String taskId;
    private T data;

    public static <T> LiveWsMessage<T> of(String type, String taskId, T data) {
        return new LiveWsMessage<>(type, taskId, data);
    }
}
