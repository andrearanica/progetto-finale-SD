# Progetto Sistemi Distribuiti 2024-2025 - Finite State Automaton

Progetto finale di Sistemi Distribuiti: applicazione web che simula un portale per la gestione del bonus cultura, composta da un database, un server web e un client web.  
Il database usa un protocollo testuale basato su TCP, il web server è realizzato in Java e il client web in JavaScript.

## Componenti del gruppo

* Federico Zotti (914252) <f.zotti@campus.unimib.it>
* Andrea Ranica (909424) <a.ranica@campus.unimib.it>
* Sara Trabattoni (914295) <s.trabattoni@campus.unimib.it>

## Compilazione ed esecuzione

Sia il server Web sia il database sono applicazioni Java gestire con Maven. All'interno delle rispettive cartelle si può trovare il file `pom.xml` in cui è presenta la configurazione di Maven per il progetto. Si presuppone l'utilizzo della macchina virtuale di laboratorio, per cui nel `pom.xml` è specificato l'uso di Java 21.

Il server Web e il database sono dei progetti Java che utilizano Maven per gestire le dipendenze, la compilazione e l'esecuzione.

### Client Web

Per avviare il client Web è necessario utilizzare l'estensione "Live Preview" su Visual Studio Code, come mostrato durante il laboratorio. Tale estensione espone un server locale con i file contenuti nella cartella `client-web`.

**Attenzione**: è necessario configurare CORS in Google Chrome come mostrato nel laboratorio.

### Server Web

Il server Web utilizza Jetty e Jersey. Si può avviare eseguendo `mvn jetty:run` all'interno della cartella `server-web`. Espone le API REST all'indirizzo `localhost` alla porta `8080`.

### Database

Il db carica dei dati all'avvio dal file `database/initialData.txt`.
Si presuppone che il db venga eseguito dalla cartella `database/`.
Il formato di quel file è il seguente:
- `-key value` per i valori di tipo stringa
- `+key value1 value2 value3 ...` per i valori di tipo lista


Il database è una semplice applicazione Java. Si possono utilizzare i seguenti comandi Maven:

* `mvn clean`: per ripulire la cartella dai file temporanei,
* `mvn compile`: per compilare l'applicazione,
* `mvn exec:java`: per avviare l'applicazione (presuppone che la classe principale sia `Main.java`). Si pone in ascolto all'indirizzo `localhost` alla porta `3030`.
