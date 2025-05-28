# Progetto Sistemi Distribuiti 2024-2025 - API REST

**Attenzione**: l'unica rappresentazione ammessa è in formato JSON. Pertanto vengono assunti gli header `Content-Type: application/json` e `Accept: application/json`.

## `/users`

### ✅​ GET​

**Descrizione**: restituisce le informazioni su tutti gli utenti registrati nel sistema.

**Parametri**: nessuno.

**Header**: nessuno.

**Risposta**: una lista contenenti oggetti che hanno come campi `name`, `surname`, `email`, `fiscalCode`, `balance` e `vouchers` (lista di `voucher`).

**Codici di stato restituiti**: `200 OK`.

### ✅​ POST

**Descrizione**: aggiunge un utente al sistema.

**Parametri**: nessuno.

**Header**: nessuno.

**Body richiesta**: singolo utente con i campi `name`, `surname`, `email` e `fiscalCode`.

**Risposta**: body vuoto e la risorsa creata è indicata nell'header `Location`.

**Codici di stato restituiti**:

* `201 Created`: successo.
* `400 Bad Request`: c'è un errore del client (JSON, campo mancante o altro).

## `/users/{fiscalCode}`

### ✅ ​GET

**Descrizione**: restituisce le informazioni su un preciso utente del sistema.

**Parametri**: fiscalCode, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Risposta**: un oggetto User, che ha come campi `name`, `surname`, `email`, `fiscalCode` e `balance`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se il codice fiscale non è registrato nel sistema.

### ✅​ PUT

**Descrizione**: sistituisce un utente nel sistema.

**Parametri**: fiscalCode, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Body**: una rappresentazione di User che contiene i nuovi dati da assegnare all'utente.

**Risposta**: un oggetto User, che ha come campi `name`, `surname`, `email`, `fiscalCode` e `balance`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se il codice fiscale non è registrato nel sistema.

## `/users/{fiscalCode}/vouchers/`

### ✅ GET

**Descrizione**: restituisce le informazioni su tutti i voucher generati da un preciso utente del sistema.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Risposta**: una lista di oggetti Voucher, che hanno come campi `type`, `value`, `consumed`, `createdDate`, `consumedDate`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se il codice fiscale non è registrato nel sistema.

### ✅ POST

**Descrizione**: genera un voucher da associare all'utente specificato.

**Parametri**: `fiscalCode`, ossia il codice fiscale dell'utente.

**Header**: nessuno.

**Body richiesta**: voucher con i campi `type` e `value`.

**Risposta**: body vuoto e la risorsa creata è indicata nell'header `Location`.

**Codici di stato restituiti**: 
* `200 OK`.
* `400 Bad request` se i dati forniti non sono validi (ad esempio se il saldo è insufficiente).
* `404 Not Found` se il codice fiscale non è registrato nel sistema.

## `/users/{fiscalCode}/vouchers/{voucherId}`

### ✅ GET

**Descrizione**: restituisce le informazioni su uno specifico voucher di un utente.

**Parametri**: 
* `fiscalCode`: il codice fiscale dell'utente.
* `voucherId`: l'id univoco del voucher

**Header**: nessuno.

**Risposta**: un oggetto Voucher, che ha come campi `type`, `value`, `consumed`, `createdDate`, `consumedDate`.

**Codici di stato restituiti**: 
* `200 OK`
* `404 Not Found` se il codice fiscale non è registrato nel sistema oppure se non esiste un voucher con l'id fornito.

### PUT

**Descrizione**: modifica un voucher associato all'utente specificato.

**Parametri**: 
* `fiscalCode`: il codice fiscale dell'utente.
* `voucherId`: l'id univoco del voucher

**Header**: nessuno.

**Body richiesta**: un oggetto `Voucher` di cui verrà preso solamente il campo `consumed`.

**Risposta**: restituisce la nuova rappresentazione della risorsa, con le modifiche richieste.

**Codici di stato restituiti**: 
* `200 OK`.
* `400 Bad request` se i dati forniti non sono validi.
* `404 Not Found` se il codice fiscale non è registrato nel sistema oppure se non esiste un voucher con l'id fornito.

### DELETE

**Descrizione**: elimina un voucher associato all'utente specificato.

**Parametri**: 
* `fiscalCode`: il codice fiscale dell'utente.
* `voucherId`: l'id univoco del voucher

**Header**: nessuno.

**Risposta**: body vuoto.

**Codici di stato restituiti**: 
* `200 OK`.
* `404 Not Found` se il codice fiscale non è registrato nel sistema oppure se non esiste un voucher con l'id fornito.