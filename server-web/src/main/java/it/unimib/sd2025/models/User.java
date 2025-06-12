package it.unimib.sd2025.models;

import java.util.List;

public class User {
    private String name;
    private String surname;
    private String email;
    private String fiscalCode;
    private float balance;
    private List<Voucher> vouchers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.equals("")) {
            throw new RuntimeException("User name cannot be null or empty");
        }
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        if (surname == null || surname.equals("")) {
            throw new RuntimeException("User surname cannot be null or empty");
        }
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        // TODO add check of email structure
        if (email == null || email == "") {
            throw new RuntimeException("User email cannot be null or empty");
        }
        this.email = email;
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public void setFiscalCode(String fiscalCode) {
        if (fiscalCode == null || fiscalCode.equals("")) {

        }
        this.fiscalCode = fiscalCode;
    }

    public float getBalance() {
        return balance;
    }
    
    public void setBalance(float balance) {
        if (balance < 0) {
            throw new RuntimeException("Balance cannot be negative");
        }
        this.balance = balance;
    }

    public List<Voucher> getVouchers() {
        return this.vouchers;
    }

    public void setVouchers(List<Voucher> vouchers) {
        if (vouchers == null) {
            throw new RuntimeException("Vouchers cannot be null");
        }
        this.vouchers = vouchers;
    }

    /**
     * Returns true if the email is valid (string + '@' + string + '.'' + string)
     * 
     * @param email
     * @return true if the email is valid
     */
    public static boolean isEmailValid(String email) {
        String emailPattern = "^[a-zA-Z0-9]+@[a-zA-Z0-9]+.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }
}
