package it.unimib.sd2025.exceptions;

public class VoucherExceptions {
    public static class InvalidVoucherException extends Exception {
        public InvalidVoucherException(String message) {
            super(message);
        }
    }

    public static class VoucherNotFoundException extends Exception {
        public VoucherNotFoundException(String fiscalCode, int voucherId) {
            super(String.format("Cannot find voucher '%d' for user '%s'", voucherId, fiscalCode));
        }
    }
    
    public static class InvalidModifyVoucherException extends Exception {
        public InvalidModifyVoucherException(String property) {
            super(String.format("Cannot modify voucher property: %s", property));
        }
    }

    public static class InvalidDeleteVoucherException extends Exception {
        public InvalidDeleteVoucherException(String fiscalCode, int voucherId) {
            super(String.format("Cannot delete voucher '%d' from user '%s'",
                                voucherId, fiscalCode));
        }
    }
}
