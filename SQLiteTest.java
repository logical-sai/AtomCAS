package com.AtomCAS;

import java.sql.Connection;
import java.sql.DriverManager;

public class SQLiteTest {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");

        try (Connection connection =
                     DriverManager.getConnection(
                             "jdbc:sqlite:factor_cache.sqlite"
                     )) {
            System.out.println("SQLite connected successfully");
        }
    }
}