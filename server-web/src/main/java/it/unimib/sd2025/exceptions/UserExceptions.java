package it.unimib.sd2025.exceptions;

public class UserExceptions {
    public static class InvalidUserException extends Exception {
        public InvalidUserException(String message) {
            super(message);
        }
    }

    public static class UserNotFoundException extends Exception {
        public UserNotFoundException(String fiscalCode) {
            super(String.format("Cannot find user with fiscal code '%s'", fiscalCode));
        }
    }

    public static class InvalidModifyUserException extends Exception {
        public InvalidModifyUserException(String property) {
            super(String.format("Cannot modify user property '%s'", property));
        }
    }
}
