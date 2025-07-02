package com.whiteboard;

import java.io.*;
import java.net.*;
import java.util.*;

public class WhiteboardServer {
    private static final int PORT = 5000;
    private static Set<ObjectOutputStream> clientOutputStreams = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Whiteboard Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            clientOutputStreams.add(out);

            Thread t = new Thread(new ClientHandler(clientSocket, out));
            t.start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;     //stream to send
        private ObjectInputStream in;       //stream to receive

        public ClientHandler(Socket socket, ObjectOutputStream out) {
            this.socket = socket;
            this.out = out;
            try {
                this.in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    broadcast(obj);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client disconnected: " + socket);
            } finally {
                try {
                    clientOutputStreams.remove(out);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcast(Object obj) {
            synchronized (clientOutputStreams) {
                for (ObjectOutputStream o : clientOutputStreams) {
                    try {
                        o.writeObject(obj);
                        o.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

