# TODO

- [ ] Capire se bene quali sono i comandi necessari per gestire le liste

# Progetto Sistemi Distribuiti 2024-2025 - TCP

Il protocollo implementato dal DB è di tipo testuale.
Dunque non si fa differenza tra tipi di dato.
Esistono solo due tipi di dati:
- string
- list

Questi tipi vengono memorizzati in hash map completamenti separati,
quindi è possibile utilizzari chiavi uguali per memorizzare sia stringhe che list.
I comandi che operano su liste hanno il suffisso `l` (es. `set` e `setl`).

Tutta la comunicazione avviene in *ASCII*, non sono ammessi caratteri al di fuori dello standard.
Ogni messaggio è composto da un comando, opzionalmente un valore, e un delimitatore `\n`.
Per immagazzinare un `\n` all'interno di una stringa deve può essere sostituito da `\\n`.
La sostituzione (`\n` -> `\\n` e `\\n` -> `\n`) deve essere effettuata dal client.
Il comando e gli argomenti devono essere separate da spazi.
Non sono ammessi spazi all'interno di chiavi e di valori.

```
COMMAND ARG\n
```

```
COMMAND ARG ARG\n
```

## Comandi

- Gli identificativi `key` non possono contenere spazi e indicano la chiave da ricercare nel db.
- Gli identificativi `index` indicano gli indici della lista.
- Gli identificativi `value` sono i valori da immagazzinare nel db.

### GET

Ritorna l'elemento (stringa) con la chiave.

```
GET key
```

#### Esempio

```
>> GET key
<< OK value
```

### GETL

Ritorna la lista con la chiave.

```
GETL key
```

```
GETL key index
```

#### Esempio

```
>> GETL key
<< OK value1 value2 value3 value4
```

```
>> GETL key 3
<< OK value3
```

### SET

Salva il valore con la chiave.

```
SET key value
```

#### Esempio

```
>> SET key value
<< OK
```

### SETL

Salva la lista con la chiave.

```
SETL key value1 value2 value3
```

#### Esempio

```
>> SET key value1 value2 value3
<< OK
```

### ADDL

Aggiunge un elemento alla lista.

```
ADDL key value
```

#### Esempio

```
>> SETL key value1 value2
<< OK
---
>> ADDL key value3
<< OK
---
>> GETL key
<< OK value1 value2 value3
```

### REMOVEL

Rimuove l'elemento dalla lista.

```
REMOVEL key
```

#### Esempio

```
>> SETL key value1 value2 value3
<< OK
---
>> REMOVEL key value3
<< OK
---
>> GETL key
<< OK value1 value2
```

### CLEAR

Rimuove l'elemento (stringa) dal database.

```
CLEAR key
```

#### Esempio

```
>> CLEAR key
<< OK
```

### CLEARL

Rimuove l'elemento (lista) dal database.

```
CLEARL key
```

#### Esempio

```
>> CLEARL key
<< OK
```

## Risposte

Le risposte sono di due tipi

- `OK` / `OK value`: in caso di successo dell'operazione
- `ERR error`: in caso di errore
