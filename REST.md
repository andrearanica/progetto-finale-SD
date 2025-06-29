# Progetto Sistemi Distribuiti 2024-2025 - API REST

**Attenzione**: 
- L'unica rappresentazione ammessa è in formato JSON. Pertanto vengono assunti gli 
header `Content-Type: application/json` e `Accept: application/json`
- Il formato di rappresentazione delle date è `GIORNO/MESE/ANNO ORE:MINUTI:SECONDI`
- In caso di errore lato client (4xx) o lato server (5xx), il body della risposta conterrà un
attributo "error" con una stringa che spiega l'errore che si è verificato a seguito della richiesta.
Eviteremo di specificare questo dettaglio nella risposte illustrate di seguito

**Risorse e attributi**
- `User`: risorsa che rappresenta un utente all'interno del sistema. I suoi attributi sono:
    - `name` (string)
    - `surname` (string)
    - `email` (string): stringa che segue la struttura standard delle mail (user@domain)
    - `fiscalCode` (string): stringa che segue la struttura dei codici fiscali italiani (6 lettere, 2 numeri, 1 lettera, 2 numeri, 1 lettera, 3 numeri, 1 lettera)
    - `vouchers` (list): voucher generati da un utente
    - `balance` (float): credito residuo a disposizione di un utente
- `Voucher`: risorsa che rappresenta un buono creato da un utente. I suoi attributi sono:
    - `id` (int): viene ignorato se presente nel body delle richieste relative ai voucher, dato che
    viene generato automaticamente dal server
    - `type` (string): categoria di cui il voucher fa parte
    - `value` (float)
    - `consumed` (boolean)
    - `createdDateTime` (string)
    - `consumedDateTime` (string): se il voucher è stato consumato, indica la data in cui è stato 
    consumato

## `/users`

### GET​

**Descrizione**: restituisce le rappresentazioni di tutti gli utenti registrati nel sistema.

**Parametri**: nessuno.

**Header**: nessuno.

**Risposta**: una lista di rappresentazioni di oggetti `user`.

**Codici di stato restituiti**: `200 OK`

### POST

**Descrizione**: aggiunge un utente al sistema.

**Parametri**: nessuno.

**Header**: nessuno.

**Body richiesta**: rappresentazione di un utente; il campo `vouchers` deve essere una lista vuota, 
dato che per poter aggiungere/modificare/eliminare i voucher associati ad un utente è necessario 
usare gli endpoint specifici per i voucher (altrimenti viene generato un errore). Il campo `balance`,
se presente, verrà ignorato e impostato di default a 500.

**Risposta**: body vuoto e la risorsa creata è indicata nell'header `Location`.

**Codici di stato restituiti**:

* `201 Created`: successo.
* `400 Bad Request`: c'è un errore del client (JSON, campo mancante, codice fiscale non valido...).

## `/users/{fiscalCode}`

### ​GET

**Descrizione**: restituisce la rappresentazione di un utente del sistema.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Risposta**: una rappresentazione di un oggetto `User`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se non è presente un utente con il codice fiscale fornito.

### PUT

**Descrizione**: sostituisce le informazioni di un utente nel sistema.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Body**: una rappresentazione di `User` che contiene i nuovi dati da assegnare all'utente. 
N.B. È possibile omettere il campo `vouchers`, e non è possibile modificare questo attributo 
tramite questo tipo di richiesta (verrà generato un errore 400). Per poter aggiungere/rimuovere/
modificare voucher è necessario usare l'endpoint `/users/fiscalCode/vouchers`.

**Risposta**: una rappresentazione di un oggetto `User` con le modifiche applicate.

**Codici di stato restituiti**: 
* `200 OK`
* `400 Bad Request` se i dati forniti non sono validi
* `404 Not Found` se non è presente un utente con il codice fiscale fornito

## `/users/{fiscalCode}/vouchers/`

### GET

**Descrizione**: restituisce le rappresentazioni di tutti i voucher generati da un preciso utente 
del sistema.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Risposta**: una lista di rappresentazioni di oggetti `voucher`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se non è presente un utente con il codice fiscale fornito.

### POST

**Descrizione**: aggiunge un voucher a un utente.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Body richiesta**: rappresentazione di un oggetto `Voucher`. 
N.B. Il campo `consumedDateTime` deve essere `null` e `consumed` deve valere `false`, altrimenti 
verrà generato un errore; non è quindi possibile crere un voucher che sia già stato consumato.
Inoltre il valore del voucher deve essere minore o uguale al credito residuo dell'utente.

**Risposta**: body vuoto e la risorsa creata è indicata nell'header `Location`.

**Codici di stato restituiti**: 
* `200 OK`.
* `400 Bad request` se i dati forniti non sono validi (ad esempio se il saldo dell'utente è 
                                                        insufficiente o se mancano informazioni).
* `404 Not Found` se non è presente un utente con il codice fiscale fornito.

## `/users/{fiscalCode}/vouchers/{voucherId}`

### GET

**Descrizione**: restituisce la rappresentazione di un voucher di un utente.

**Parametri**: 
* `fiscalCode`: il codice fiscale dell'utente
* `voucherId`: l'id del voucher

**Header**: nessuno.

**Risposta**: la rappresentazione di un oggetto `Voucher`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se non è presente un utente con il codice fiscale fornito oppure se non esiste un 
                  voucher con l'id fornito.

### PUT

**Descrizione**: modifica un voucher di un utente.

**Parametri**: 
* `fiscalCode`: il codice fiscale dell'utente
* `voucherId`: l'id univoco del voucher

**Header**: nessuno.

**Body richiesta**: un oggetto `Voucher`. N.B. Non sarà possibile modificare i valori degli 
attributi `type`, `value`, `createdDateTime`: gli unici attributi modificabili saranno `consumed` e
`consumedDateTime`, che possono diventare rispettivamente da `false/null` a `true/datetime`. Per 
poter impostare questi due attributi, è necessario che entrambi siano presenti all'interno della 
rappresentazione fornita (ad esempio non è possibile impostare l'attributo `consumed` a `true` senza
 specificare `consumedDateTime` e viceversa). In tutti questi casi, viene generato un errore.

**Risposta**: restituisce la nuova rappresentazione della risorsa, con le modifiche richieste.

**Codici di stato restituiti**: 
* `200 OK`.
* `400 Bad request` se i dati forniti non sono validi o mancanti
* `404 Not Found` se non è presente un utente con il codice fiscale fornito oppure se non esiste un 
                  voucher con l'id fornito

### DELETE

**Descrizione**: elimina un voucher associato all'utente specificato.

**Parametri**: 
* `fiscalCode`: il codice fiscale dell'utente.
* `voucherId`: l'id univoco del voucher

**Header**: nessuno.

**Risposta**: body vuoto.

**Codici di stato restituiti**: 
* `200 OK`.
* `404 Not Found` se non è presente un utente con il codice fiscale fornito oppure se non esiste un 
                  voucher con l'id fornito.
