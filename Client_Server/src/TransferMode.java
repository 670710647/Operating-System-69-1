public enum TransferMode {
    TRADITIONAL,
    NIO;

    public static TransferMode fromString(String value) {
        if ("traditional".equalsIgnoreCase(value)) {
            return TRADITIONAL;
        }

        if ("nio".equalsIgnoreCase(value)) {
            return NIO;
        }

        throw new IllegalArgumentException(
            "โหมดจะต้องเป็น traditional หรือ nio"
        );
    }
}
