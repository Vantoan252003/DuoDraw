package com.codraw.CoDraw.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrokeMessage {
    private String type = "STROKE";
    private String id;
    private List<PointMessage> points;
    private String colorHex;
    private float strokeWidth;
    @JsonAlias({"isEraser", "eraser"})
    @JsonProperty("isEraser")
    private boolean eraser;
    private boolean preview;
    private int playerId;

    @Data
    public static class PointMessage {
        private float x;
        private float y;
    }
}
