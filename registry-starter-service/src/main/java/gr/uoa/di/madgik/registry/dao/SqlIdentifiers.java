package gr.uoa.di.madgik.registry.dao;

public final class SqlIdentifiers {

    private SqlIdentifiers() {
    }

    /**
     * Quotes a column name so reserved words (e.g. {@code order}) are valid identifiers. The name is
     * lowercased because unquoted identifiers are folded to lowercase by the database.
     */
    public static String quoteColumn(String name) {
        return "\"" + name.toLowerCase().replace("\"", "\"\"") + "\"";
    }
}
