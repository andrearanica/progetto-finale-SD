package it.unimib.sd2025;

import java.util.*;
import java.util.concurrent.*;

/**
 * Classe principale del database.
 * Questa classe contiene le funzionalità principali del database.
 * Implementa un dizionario per avere un'archiviazione dei dati in stile
 * chiave-valore.
 * La classe è progettata per poter gestire la concorrenza delle richieste del
 * client.
 * Utilizza ConcurrentHashMap per gestire la concorrenza.
 * Le operazioni possibili sono descritte nel file TCP.md
 * È un singleton, quindi non è necessario creare più istanze.
 */
public class Database {
    private static Database instance;
    private ConcurrentHashMap<String, String> archivio;

    // Crea anche l'archivio per le liste, utilizzando Vector perché è
    // sincronizzato.
    private ConcurrentHashMap<String, Vector<String>> archivioListe;

    /**
     * Restituisce l'istanza del database.
     * 
     * @return L'istanza del database.
     */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    // Altre funzionalità del database possono essere aggiunte qui.

    private Database() {
        // Inizializzazione del database.
        // Inizializza l'hashmap
        // L'inizializzazione è lazy. Avviene alla prima getInstance().
        this.archivio = new ConcurrentHashMap<>();
        this.archivioListe = new ConcurrentHashMap<>();
    }

    /**
     * Esegue una SET sul database.
     * 
     * @param key   Chiave.
     * @param value Valore.
     * @return Il risultato dell'operazione.
     */
    public synchronized String set(String key, String value) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            // Controlla se il valore è valido.
            if (value == null) {
                return "ERR Invalid value";
            }

            // Aggiunge o aggiorna la chiave con il valore specificato.
            archivio.put(key, value);
            return "OK";

        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    /**
     * Esegue una GET sul database.
     * 
     * @param key Chiave da cercare.
     * @return Il valore associato alla chiave.
     */
    public synchronized String get(String key) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            // Recupera il valore associato alla chiave.
            String value = archivio.get(key);
            if (value == null) {
                // return "ERR Key not found";
                return "OK ";
                // TODO: Decidere se restituire un errore o un valore vuoto.
            }
            return "OK " + value;

        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    /**
     * Esegue una CLEAR sul database.
     * 
     * @param key Chiave da cancellare.
     * @return Il risultato dell'operazione.
     */
    public synchronized String clear(String key) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            archivio.remove(key);
            return "OK";

        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    /*
     * Funzioni per le liste
     */

    /**
     * Esegue una SETL sul database.
     * @param key   Chiave della lista.
     * @param value Valore da associare alla chiave nella lista.
     * @return Il risultato dell'operazione.
     */
    public synchronized String setl(String key, Vector<String> value) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            // Controlla se il valore è valido.
            if (value == null) {
                return "ERR Invalid value";
            }

            // Aggiunge o aggiorna la chiave con il valore specificato nella lista.
            archivioListe.put(key, value);
            return "OK";

        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    /**
     * Esegue una GETL sul database.
     * 
     * @param key Chiave della lista da cercare.
     * @return Il valore associato alla chiave nella lista.
     */
    public synchronized String getl(String key) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            // Recupera il valore associato alla chiave nella lista.
            Vector<String> value = archivioListe.get(key);
            if (value == null) {
                // return "ERR Key not found";
                return "OK ";
            } else {
                return "OK " + String.join(" ", value);
            }
        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    /**
     * Esegue una CLEARL sul database.
     * 
     * @param key Chiave della lista da cancellare.
     * @return Il risultato dell'operazione.
     */
    public synchronized String clearl(String key) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            archivioListe.remove(key);
            return "OK";

        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    /**
     * Aggiunge un valore alla lista associata alla chiave.
     * 
     * @param key   Chiave della lista.
     * @param value Valore da aggiungere alla lista.
     * @return Il risultato dell'operazione.
     */
    public synchronized String addl(String key, String value) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            // Controlla se il valore è valido.
            if (value == null) {
                return "ERR Invalid value";
            }

            // Aggiunge il valore alla lista associata alla chiave.
            Vector<String> list = archivioListe.getOrDefault(key, new Vector<>());
            list.add(value);
            archivioListe.put(key, list);
            return "OK";

        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    /**
     * Rimuove un valore dalla lista associata alla chiave.
     * 
     * @param key   Chiave della lista.
     * @param value Valore da rimuovere dalla lista.
     * @return Il risultato dell'operazione.
     */
    public synchronized String removel(String key, String value) {
        try {
            // Controlla se la chiave è valida.
            if (key == null || key.isEmpty()) {
                return "ERR Invalid key";
            }

            // Controlla se il valore è valido.
            if (value == null) {
                return "ERR Invalid value";
            }

            // Rimuove il valore dalla lista associata alla chiave.
            Vector<String> list = archivioListe.get(key);
            if (list != null && list.remove(value)) {
                return "OK";
            } else {
                return "ERR Value not found in list";
            }

        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

}
