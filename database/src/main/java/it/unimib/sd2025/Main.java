package it.unimib.sd2025;

import java.net.*;
import java.util.List;
import java.util.Vector;
import java.io.*;
import it.unimib.sd2025.Database;
import it.unimib.sd2025.SocketHandler;

/**
 * Classe principale in cui parte il database.
 */
public class Main {
    /**
     * Porta di ascolto.
     */
    public static final int PORT = 3030;
    /**
     * File per inizializzare il db
     */
    public static final String INITIAL_DATA_FILE = "initialData.txt";

    /**
     * Avvia il database e l'ascolto di nuove connessioni.
     */
    public static void startServer() throws IOException {
        var server = new ServerSocket(PORT);

        System.out.println("Database listening at localhost:" + PORT);

        try {
            while (true)
                new SocketHandler(server.accept()).start();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            server.close();
        }
    }

    /**
     * Metodo principale di avvio del database.
     *
     * @param args argomenti passati a riga di comando. (Vengono ignorati)
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // Inizializza il database con dati presi da un file.
        initialize();

        // Inizia l'ascolto delle connessioni.
        startServer();
    }

    /**
     * Inizializza il database con i dati iniziali.
     * Legge i dati da `initialData.txt` e li inserisce nel database.
     */
    public static void initialize() {
        // Inizializza il database.
        Database db = Database.getInstance();

        /*
         * Legge i dati da `initialData.txt` e li inserisce nel db
         * Se la riga inizia con `-` vuol dire che il dato è una stringa, nel formato
         * `-key value`
         * Se la riga inizia con `+` vuol dire che il dato è una lista, nel formato
         * `+key value1 value2 ...`
         */
        try (BufferedReader reader = new BufferedReader(new FileReader(INITIAL_DATA_FILE))) {
            System.out.println("Initializing database with data from " + INITIAL_DATA_FILE);
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // Salta le righe vuote
                }
                if (line.startsWith("-")) {
                    // Rimuove il prefisso '-' e divide la riga in chiave e valore (stringa)
                    String[] parts = line.substring(1).split(" ", 2);
                    if (parts.length == 2) {
                        db.set(parts[0].trim(), parts[1].trim());
                    } else {
                        System.err.println("Invalid format for string: " + line);
                    }
                } else if (line.startsWith("+")) {
                    // Rimuove il prefisso '+' e divide la riga in chiave e valori (lista)
                    String[] parts = line.substring(1).split(" ");
                    if (parts.length > 1) {
                        String key = parts[0].trim();
                        String[] values = new String[parts.length - 1];
                        System.arraycopy(parts, 1, values, 0, parts.length - 1);
                        db.setl(key, new Vector<>(List.of(values)));
                    } else {
                        System.err.println("Invalid format for list: " + line);
                    }
                } else {
                    System.err.println("Unknown format: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading initial data: " + e.getMessage());
        }
        System.out.println("Database initialized with initial data.");
    }
}
