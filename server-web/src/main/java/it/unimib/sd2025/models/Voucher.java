package it.unimib.sd2025.models;

import java.time.LocalDateTime;

public class Voucher {
    private int id;
    private String type;
    private float value;
    private LocalDateTime createdDateTime;
    private LocalDateTime consumedDateTime;
    private boolean consumed;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        // TODO make this value in a list of possible values
        if (type == null || type.equals("")) {
            throw new RuntimeException("Voucher type cannot be null or empty");
        }
        this.type = type;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public LocalDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(LocalDateTime createdDateTime) {
        if (createdDateTime == null) {
            throw new RuntimeException("Created date cannot be null");
        }
        this.createdDateTime = createdDateTime;
    }

    public LocalDateTime getConsumedDateTime() {
        return consumedDateTime;
    }

    public void setConsumedDateTime(LocalDateTime consumedDateTime) {
        if (consumedDateTime == null) {
            throw new RuntimeException("Consumed date cannot be null");
        }
        this.consumedDateTime = consumedDateTime;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }
}
