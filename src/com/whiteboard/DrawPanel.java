package com.whiteboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.geom.Line2D;

public class DrawPanel extends JPanel {
    private java.util.List<LineSegment> lines = Collections.synchronizedList(new ArrayList<>());
    private int prevX, prevY;
    private ObjectOutputStream out;

    private JTextField usernameField;
    private Color color;
    private int brushSize = 3;
    private boolean eraserMode = false;

    public DrawPanel(ObjectOutputStream out, JTextField usernameField, Color color) {
        this.out = out;
        this.usernameField = usernameField;
        this.color = color;

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                prevX = e.getX();
                prevY = e.getY();

                if (eraserMode) {
                    deleteLineAtPoint(e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (eraserMode) return;

                int x = e.getX();
                int y = e.getY();
                String currentUsername = usernameField.getText().trim();
                if (currentUsername.isEmpty()) currentUsername = "Anonymous";

                LineSegment line = new LineSegment(prevX, prevY, x, y, color, currentUsername, brushSize);
                addLine(line);
                sendLine(line);
                prevX = x;
                prevY = y;
            }
        });
    }

    public void addLine(LineSegment line) {
        lines.add(line);
        repaint();
    }

    private void sendLine(LineSegment line) {
        try {
            out.writeObject(line);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        synchronized (lines) {
            for (LineSegment line : lines) {
                g2.setColor(line.color);
                g2.setStroke(new BasicStroke(line.brushSize));
                g2.drawLine(line.x1, line.y1, line.x2, line.y2);
            }
        }
    }

    public void undoLastLine() {
        synchronized (lines) {
            String currentUsername = usernameField.getText().trim();
            if (currentUsername.isEmpty()) currentUsername = "Anonymous";

            for (int i = lines.size() - 1; i >= 0; i--) {
                LineSegment line = lines.get(i);
                if (line.username.equals(currentUsername)) {
                    lines.remove(i);
                    repaint();

                    try {
                        out.writeObject(new DeleteLineCommand(line.id));
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    public void deleteLineById(String lineId) {
        synchronized (lines) {
            lines.removeIf(line -> line.id.equals(lineId));
        }
        repaint();
    }

    public void deleteLineAtPoint(int x, int y) {
        synchronized (lines) {
            for (LineSegment line : lines) {
                if (pointNearLine(x, y, line)) {
                    try {
                        out.writeObject(new DeleteLineCommand(line.id));
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private boolean pointNearLine(int x, int y, LineSegment line) {
        double dist = Line2D.ptSegDist(line.x1, line.y1, line.x2, line.y2, x, y);
        return dist <= 5.0;
    }

    public void clearLines() {
        lines.clear();
        repaint();
    }

    public void setCurrentColor(Color newColor) {
        this.color = newColor;
    }

    public Color getCurrentColor() {
        return this.color;
    }

    public void setBrushSize(int newSize) {
        this.brushSize = newSize;
    }

    public void sendClearCommand() {
        try {
            out.writeObject(new ClearCommand());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAsImage() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        paintAll(g2d);
        g2d.dispose();
        try {
            String filename = "whiteboard_" + System.currentTimeMillis() + ".png";
            ImageIO.write(image, "PNG", new File(filename));
            JOptionPane.showMessageDialog(this, "Saved as " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEraserMode(boolean eraserOn) {
        this.eraserMode = eraserOn;
    }
}
