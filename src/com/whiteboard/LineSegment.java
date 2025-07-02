package com.whiteboard;

import java.io.Serializable;
import java.util.UUID;
import java.awt.Color;

public class LineSegment implements Serializable {
    public String id;
    public int x1, y1, x2, y2;
    public Color color;
    public String username;
    public int brushSize;

    public LineSegment(int x1, int y1, int x2, int y2, Color color, String username, int brushSize) {
        this.id = UUID.randomUUID().toString();
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
        this.color = color;
        this.username = username;
        this.brushSize = brushSize;
    }
}
