package com.whiteboard;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

public class WhiteboardClient {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DrawPanel drawPanel;

    public WhiteboardClient(String serverAddress, int port) throws IOException {
        // Connect to server
        Socket socket = new Socket(serverAddress, port);
        System.out.println("Connected to server: " + socket);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        
        // Ask for user name BEFORE board opens
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username == null || username.trim().isEmpty()) {
            username = "Anonymous";
        }

        // Setup GUI frame
        JFrame frame = new JFrame("Collaborative Whiteboard");

        // Panel for controls (buttons, slider)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));


        // Create draw panel (pass usernameField)
        drawPanel = new DrawPanel(out, username, Color.BLACK);

        // Color picker button
        JButton colorPicker = new JButton("Pick Color");
        colorPicker.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(frame, "Choose your drawing color", drawPanel.getCurrentColor());
            if (newColor != null) {
                drawPanel.setCurrentColor(newColor);
            }
        });
        buttonPanel.add(colorPicker);

        // Brush size slider
        JSlider sizeSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 3);
        sizeSlider.setMajorTickSpacing(5);
        sizeSlider.setMinorTickSpacing(1);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setPaintLabels(true);
        sizeSlider.addChangeListener(e -> drawPanel.setBrushSize(sizeSlider.getValue()));
        buttonPanel.add(new JLabel("Brush Size:"));
        buttonPanel.add(sizeSlider);

        // Control buttons
        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> drawPanel.undoLastLine());
        buttonPanel.add(undoButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> drawPanel.sendClearCommand());
        buttonPanel.add(clearButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> drawPanel.saveAsImage());
        buttonPanel.add(saveButton);

        JToggleButton eraserToggle = new JToggleButton("Eraser");
        eraserToggle.addActionListener(e -> drawPanel.setEraserMode(eraserToggle.isSelected()));
        buttonPanel.add(eraserToggle);

        // Create color palette panel
        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new GridLayout(0, 1));

        Color[] colors = {Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA};

        for (Color c : colors) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(c);
            colorBtn.setPreferredSize(new Dimension(30, 30));
            colorBtn.addActionListener(e -> drawPanel.setCurrentColor(c));
            colorPanel.add(colorBtn);
        }

        // Add panels to frame
        frame.getContentPane().add(colorPanel, BorderLayout.WEST);
        frame.getContentPane().add(drawPanel, BorderLayout.CENTER);
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Finalize frame
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Listen for updates from server
        new Thread(this::listenForUpdates).start();
    }

    private void listenForUpdates() {
        try {
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof LineSegment) {
                    drawPanel.addLine((LineSegment) obj);
                } else if (obj instanceof ClearCommand) {
                    drawPanel.clearLines();
                } else if (obj instanceof DeleteLineCommand) {
                    drawPanel.deleteLineById(((DeleteLineCommand) obj).lineId);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Disconnected from server.");
            System.exit(0);
        }
    }

    public static void main(String[] args) throws IOException {
        String serverAddress = JOptionPane.showInputDialog(
            "Enter server IP address:", "127.0.0.1");
        new WhiteboardClient(serverAddress, 5000);
    }
}
