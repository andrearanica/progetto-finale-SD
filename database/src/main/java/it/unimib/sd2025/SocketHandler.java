package it.unimib.sd2025;

import java.net.*;
import java.io.*;
import it.unimib.sd2025.Database;

/*
 * Classe per gestire le connessioni socket e fare il parsing dei comandi.
 */
public class SocketHandler extends Thread {
    private Database db = Database.getInstance();
    private Socket socket;

    public SocketHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Gestisce la connessione socket e il parsing dei comandi.
     */
    public void run() {

        // Implementazione della gestione della connessione e del parsing dei comandi.
        // Questo metodo dovrebbe leggere i dati dalla socket, fare il parsing dei
        // comandi
        // e interagire con il database per eseguire le operazioni richieste.

        try {
            handleConnection();
        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void handleConnection() throws IOException {
        System.out.println("Connection established with " + socket.getInetAddress() + ":" + socket.getPort());
        var out = new PrintWriter(socket.getOutputStream(), true);
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String inputLine;
        inputLine = in.readLine();
        if (inputLine == null) {
            out.println("ERR No input received");
            return;
        }

        inputLine = inputLine.trim();
        // Prende il comando cercando la stringa " " (spazio) e lo divide in due parti:

        if (inputLine.toLowerCase().startsWith("set ")) {
            inputLine = inputLine.substring(4);
            String[] parts = inputLine.split(" ", 2);
            if (parts.length < 2) {
                out.println("ERR Invalid SET command format");
                return;
            }
            String key = parts[0].trim();
            String value = parts[1].trim();
            String result = db.set(key, value);
            out.println(result);
        } else if (inputLine.toLowerCase().startsWith("get ")) {
            inputLine = inputLine.substring(4);
            String key = inputLine.trim();
            // Controlla che non ci siano spazi all'interno della chiave
            if (key.contains(" ")) {
                out.println("ERR Invalid key format");
                return;
            }
            String result = db.get(key);
            out.println(result);
        } else if (inputLine.toLowerCase().startsWith("clear ")) {
            inputLine = inputLine.substring(4);
            String key = inputLine.trim();
            // Controlla che non ci siano spazi all'interno della chiave
            if (key.contains(" ")) {
                out.println("ERR Invalid key format");
                return;
            }
            String result = db.clear(key);
            out.println(result);
        } else {
            out.println("ERR Unknown command: " + inputLine);
        }
    }
}
