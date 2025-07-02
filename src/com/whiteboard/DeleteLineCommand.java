package com.whiteboard;

import java.io.Serializable;

public class DeleteLineCommand implements Serializable {
    public String lineId;  // ID of line to delete

    public DeleteLineCommand(String lineId) {
        this.lineId = lineId;
    }
}
