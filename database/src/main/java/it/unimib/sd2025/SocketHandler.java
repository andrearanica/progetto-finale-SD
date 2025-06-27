package it.unimib.sd2025;

import java.net.*;
import java.util.List;
import java.util.Vector;
import java.io.*;
import it.unimib.sd2025.Database;

/**
 * Classe per gestire le connessioni socket e fare il parsing dei comandi.
 */
public class SocketHandler extends Thread {
    private Database db = Database.getInstance();
    private Socket socket;

    public SocketHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Esegue il thread per gestire la connessione socket.
     * Gestisce gli errori e passa la gesitone della connessione a handleConnection()
     */
    public void run() {
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


    /**
     * Si occupa di leggere i dati in ingresso, fare il parsing,
     * mandare i comandi al DB e ritornare i valori.
     * Legge solo una riga dall'input e poi ritorna.
     */
    private void handleConnection() throws IOException {
        System.out.println("Connection established with " + socket.getInetAddress() + ":" + socket.getPort());
        var out = new PrintWriter(socket.getOutputStream(), true);
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String inputLine; // Rappresenta una riga letta.
        inputLine = in.readLine();
        if (inputLine == null) {
            out.println("ERR No input received");
            return;
        }
        inputLine = inputLine.trim();

        if (inputLine.toLowerCase().startsWith("set ")) {
            /*
             * Comando SET key value
             */
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
            /*
             * Comando GET key
             */
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
            /*
             * Comando CLEAR key
             */
            inputLine = inputLine.substring(4);
            String key = inputLine.trim();
            // Controlla che non ci siano spazi all'interno della chiave
            if (key.contains(" ")) {
                out.println("ERR Invalid key format");
                return;
            }
            String result = db.clear(key);
            out.println(result);

        }
        /*
         * Comandi per le liste
         */
        else if (inputLine.toLowerCase().startsWith("setl ")) {
            /*
             * Comando SETL key value1 value2
             */
            inputLine = inputLine.substring(5);
            String[] parts = inputLine.split(" ", 2);
            if (parts.length < 2) {
                out.println("ERR Invalid SETL command format");
                return;
            }
            String key = parts[0].trim();
            // Crea una lista di valori splittando il messaggio da spazi
            Vector<String> value = new Vector<>(List.of(parts[1].trim().split(" ")));
            String result = db.setl(key, value);
            out.println(result);

        } else if (inputLine.toLowerCase().startsWith("getl ")) {
            /*
             * Comando GETL key
             */
            inputLine = inputLine.substring(5);
            String key = inputLine.trim();
            // Controlla che non ci siano spazi all'interno della chiave
            if (key.contains(" ")) {
                out.println("ERR Invalid key format");
                return;
            }
            String result = db.getl(key);
            out.println(result);

        } else if (inputLine.toLowerCase().startsWith("clearl ")) {
            /*
             * Comando CLEARL key
             */
            inputLine = inputLine.substring(6);
            String key = inputLine.trim();
            // Controlla che non ci siano spazi all'interno della chiave
            if (key.contains(" ")) {
                out.println("ERR Invalid key format");
                return;
            }
            String result = db.clearl(key);
            out.println(result);

        } else if (inputLine.toLowerCase().startsWith("addl ")) {
            /*
             * Comando ADDL key value3
             */
            inputLine = inputLine.substring(5);
            String[] parts = inputLine.split(" ", 2);
            if (parts.length < 2) {
                out.println("ERR Invalid ADDL command format");
                return;
            }
            String key = parts[0].trim();
            String value = parts[1].trim();
            String result = db.addl(key, value);
            out.println(result);

        } else if (inputLine.toLowerCase().startsWith("removel ")) {
            /*
             * Comando REMOVEL key value2
             */
            inputLine = inputLine.substring(8);
            String[] parts = inputLine.split(" ", 2);
            if (parts.length < 2) {
                out.println("ERR Invalid REMOVEL command format");
                return;
            }
            String key = parts[0].trim();
            String value = parts[1].trim();
            String result = db.removel(key, value);
            out.println(result);

        } else {
            /*
             * Comando sconosciuto
             */
            out.println("ERR Unknown command: " + inputLine);
        }
    }
}
