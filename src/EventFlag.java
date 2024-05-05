public enum EventFlag {
    INVALID("Invalid"),
    VALID("Valid"),
    USER_ALREADY_EXISTS("Username Already Exists"),
    USER_DOES_NOT_EXIST("Username Does Not Exist"),
    INVALID_PASSWORD("Invalid Password"),
    LOGIN(null),
    CREATE_ACCOUNT(null);

    private final String message;
    public String getMessage() {
        return message;
    }

    EventFlag(String message) {
        this.message = message;
    }
}
