package com.AtomCAS.utilities;
import java.sql.*;
final class FactorCacheDatabase implements AutoCloseable {
    private static final String URL = "jdbc:sqlite:factor_cache.sqlite";

    private final Connection connection;
    private final PreparedStatement findFactorStatement;
    private final PreparedStatement insertFactorStatement;

    private FactorCacheDatabase() {
        try {
            connection = DriverManager.getConnection(URL);
            configureConnection();
            initializeSchema();

            findFactorStatement = connection.prepareStatement("""
                    SELECT factor
                    FROM factor_cache
                    WHERE n = ?
                    LIMIT 1
                    """);
            insertFactorStatement = connection.prepareStatement("""
                    INSERT OR IGNORE INTO factor_cache
                        (n, factor, method, found_at)
                    VALUES (?, ?, ?, ?)
                    """);
            Runtime.getRuntime().addShutdownHook(new Thread(this::closeQuietly, "factor-cache-shutdown"));

        } catch (SQLException e) {throw new ExceptionInInitializerError(e);}
    }
    /*
     * Initialization-on-demand holder idiom:
     * constructed only when instance() is first called.
     */
    private static final class Holder {
        private static final FactorCacheDatabase INSTANCE = new FactorCacheDatabase();}
    static FactorCacheDatabase instance() {return Holder.INSTANCE;}
    private void configureConnection() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS factor_cache (
                        n        TEXT NOT NULL,
                        factor   TEXT NOT NULL,
                        method   TEXT NOT NULL,
                        found_at INTEGER NOT NULL,
                        PRIMARY KEY (n, factor)
                    )
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS factor_cache_n_index
                    ON factor_cache(n)
                    """);
        }
    }
    synchronized LargeInteger findFactor(LargeInteger n) {
        try {
            findFactorStatement.clearParameters();
            findFactorStatement.setString(1, n.toString());

            try (ResultSet result = findFactorStatement.executeQuery()) {
                while (result.next()) {
                    LargeInteger factor = new LargeInteger(result.getString("factor"));
                    // Persisted values should still be validated.
                    if (factor.greaterThan(LargeInteger.ONE) && factor.lessThan(n)
                            && n.remainder(factor).equals(LargeInteger.ZERO))
                        return factor;
                }
            }
            return null;
        } catch (SQLException e) {throw new IllegalStateException("Could not query factor cache", e);}
    }
    synchronized void save(LargeInteger n, LargeInteger factor, LargeInteger.AlgorithmChoice method) {
        if (factor.lesserThanOrEqualTo(LargeInteger.ONE) || factor.greaterThanOrEqualTo(n)
                || !n.remainder(factor).isZero())
            throw new IllegalArgumentException("Attempted to cache an invalid factor");
        LargeInteger cofactor = n.divide(factor);
        // Store one canonical side of the split.
        if (cofactor.lessThan(factor))
            factor = cofactor;
        try {
            insertFactorStatement.clearParameters();
            insertFactorStatement.setString(1, n.toString());
            insertFactorStatement.setString(2, factor.toString());
            insertFactorStatement.setString(3, method.name());
            insertFactorStatement.setLong(4, System.currentTimeMillis());
            insertFactorStatement.executeUpdate();

        } catch (SQLException e) {throw new IllegalStateException("Could not update factor cache", e);}
    }
    @Override
    public synchronized void close() throws SQLException {
        findFactorStatement.close();
        insertFactorStatement.close();
        connection.close();
    }
    private void closeQuietly() {try {close();} catch (SQLException _) {}}
}