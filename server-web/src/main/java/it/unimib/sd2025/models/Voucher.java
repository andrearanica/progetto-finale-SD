package it.unimib.sd2025.models;

public class Voucher {
    private int id;
    private String type;
    private float value;
    private String createdDateTime;
    private String consumedDateTime;
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

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        if (createdDateTime == null) {
            throw new RuntimeException("Created date cannot be null");
        }
        this.createdDateTime = createdDateTime;
    }

    public String getConsumedDateTime() {
        return consumedDateTime;
    }

    public void setConsumedDateTime(String consumedDateTime) {
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

    public boolean equals(Voucher anotherVoucher) {
        boolean idEquals = (id == anotherVoucher.getId());
        boolean valueEquals = (value == anotherVoucher.getValue());
        boolean typeEquals = (type.equals(anotherVoucher.getType()));
        boolean createdDateTimeEquals = (createdDateTime.equals(anotherVoucher.getCreatedDateTime()));
        boolean consumedDateTimeEquals = (consumedDateTime == null && anotherVoucher.getConsumedDateTime() == null) || (consumedDateTime.equals(anotherVoucher.getConsumedDateTime()));
        boolean consumedEquals = (consumed == anotherVoucher.isConsumed());

        return idEquals && valueEquals && typeEquals && createdDateTimeEquals && consumedDateTimeEquals && consumedEquals;
    }
}
