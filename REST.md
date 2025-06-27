# Progetto Sistemi Distribuiti 2024-2025 - API REST

**Attenzione**: l'unica rappresentazione ammessa è in formato JSON. Pertanto vengono assunti gli 
header `Content-Type: application/json` e `Accept: application/json`.

**Importante**: il formato di rappresentazione delle date è `GIORNO/MESE/ANNO ORE:MINUTI:SECONDI`.

## `/users`

### GET​

**Descrizione**: restituisce le rappresentazioni di tutti gli utenti registrati nel sistema.

**Parametri**: nessuno.

**Header**: nessuno.

**Risposta**: una lista di rappresentazioni di oggetti `user`; i campi di ogni oggetto sono 
`balance`, `email`, `fiscalCode`, `name`, `surname`, `vouchers` (che è una lista di `voucher`).

**Codici di stato restituiti**: `200 OK`

### POST

**Descrizione**: aggiunge un utente al sistema.

**Parametri**: nessuno.

**Header**: nessuno.

**Body richiesta**: singolo utente con i campi `name`, `surname`, `email` e `fiscalCode`.

**Risposta**: body vuoto e la risorsa creata è indicata nell'header `Location`.

**Codici di stato restituiti**:

* `201 Created`: successo.
* `400 Bad Request`: c'è un errore del client (JSON, campo mancante, codice fiscale non valido...).

## `/users/{fiscalCode}`

### ​GET

**Descrizione**: restituisce la rappresentazione di un utente del sistema.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Risposta**: un oggetto `User`, che ha come campi `name`, `surname`, `email`, `fiscalCode`, 
`balance`, e `vouchers`, ossia una lista di oggetti `voucher`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se non è presente un utente con il codice fiscale fornito.

### PUT

**Descrizione**: sostituisce le informazioni di un utente nel sistema.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Body**: una rappresentazione di `User` che contiene i nuovi dati da assegnare all'utente. 
N.B. È possibile omettere il campo `vouchers, e non è possibile modificare questo attributo 
tramite questo tipo di richiesta (verrà generato un errore 400). Per poter aggiungere/rimuovere/
modificare voucher è necessario usare l'endpoint `/users/fiscalCode/vouchers`.

**Risposta**: un oggetto `User`, che ha come campi `name`, `surname`, `email`, `fiscalCode`, 
`balance` e `vouchers`.

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

**Risposta**: una lista di oggetti `voucher`, che hanno come campi `type`, `value`, `consumed`, 
`createdDateTime`, `consumedDateTime`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se non è presente un utente con il codice fiscale fornito.

### POST

**Descrizione**: aggiunge un voucher a un utente.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Body richiesta**: rappresentazione di un oggetto `Voucher`, con i campi `type`, `value` e 
`createdDateTime`.

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

**Risposta**: la rappresentazione di un oggetto `Voucher`, che ha come campi `id`, `type`, `value`, 
`consumed`, `createdDateTime` (e `consumedDateTime`, se `consumed` è `true`).

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
`consumedDateTime`, che possono diventare da `false/null` a `true/datetime`. Per poter impostare 
questi due attributi, è necessario che entrambi siano presenti all'interno della rappresentazione 
fornita (ad esempio non è possibile impostare l'attributo `consumed` a `true` senza specificare 
`consumedDateTime`)

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