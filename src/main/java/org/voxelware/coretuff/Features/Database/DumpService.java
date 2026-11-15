package org.voxelware.coretuff.Features.Database;

import org.voxelware.coretuff.CoreTuff;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DumpService {

    private static final String JDBC_MYSQL = "com.mysql.cj.jdbc.Driver";
    private static final String JDBC_LEGACY_MYSQL = "com.mysql.jdbc.Driver";
    private static final String JDBC_MARIADB = "org.mariadb.jdbc.Driver";
    private static final String JDBC_POSTGRESQL = "org.postgresql.Driver";
    private static final String MONGO_CLIENT = "com.mongodb.client.MongoClients";
    private static final String MONGO_DOCUMENT = "org.bson.Document";

    private final CoreTuff plugin;

    public DumpService(CoreTuff plugin) {
        this.plugin = plugin;
    }

    public DumpResult dumpAll() {
        var loader = plugin.getCoreConfigLoader();
        String method = loader.getString("storage-method", "h2").toLowerCase();

        if (method.equals("h2")) {
            return new DumpResult(false, "storage-method is set to h2. Set it to mysql, mariadb, postgresql, or mongodb to dump.");
        }

        String address = loader.getString("data.address", "localhost");
        String database = loader.getString("data.database", "minecraft");
        String username = loader.getString("data.username", "root");
        String password = loader.getString("data.password", "");

        try {
            return switch (method) {
                case "mysql" -> dumpMySql(address, database, username, password);
                case "mariadb" -> dumpMariaDb(address, database, username, password);
                case "postgresql" -> dumpPostgreSql(address, database, username, password);
                case "mongodb" -> dumpMongoDb(address, database, username, password);
                default -> new DumpResult(false, "Unsupported storage-method: " + method + ". Supported: mysql, mariadb, postgresql, mongodb");
            };
        } catch (Exception e) {
            return new DumpResult(false, "Dump failed: " + e.getMessage());
        }
    }

    private DumpResult dumpMySql(String address, String database, String username, String password) throws Exception {
        loadDriver(JDBC_MYSQL, JDBC_LEGACY_MYSQL, "MySQL");
        String url = "jdbc:mysql://" + address + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return dumpSql(conn);
        }
    }

    private DumpResult dumpMariaDb(String address, String database, String username, String password) throws Exception {
        loadDriver(JDBC_MARIADB, null, "MariaDB");
        String url = "jdbc:mariadb://" + address + "/" + database + "?useSSL=false&rewriteBatchedStatements=true";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return dumpSql(conn);
        }
    }

    private DumpResult dumpPostgreSql(String address, String database, String username, String password) throws Exception {
        loadDriver(JDBC_POSTGRESQL, null, "PostgreSQL");
        String url = "jdbc:postgresql://" + address + "/" + database;
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return dumpSql(conn);
        }
    }

    private DumpResult dumpMongoDb(String address, String database, String username, String password) throws Exception {
        Class<?> clientsClass = Class.forName(MONGO_CLIENT);
        Class<?> documentClass = Class.forName(MONGO_DOCUMENT);
        Class<?> dbClass = Class.forName("com.mongodb.client.MongoDatabase");

        String uri;
        if (username != null && !username.isEmpty()) {
            uri = "mongodb://" + username + ":" + password + "@" + address + "/" + database;
        } else {
            uri = "mongodb://" + address + "/" + database;
        }

        Method createMethod = clientsClass.getMethod("create", String.class);
        Object mongoClient = createMethod.invoke(null, uri);
        try {
            Method getDbMethod = mongoClient.getClass().getMethod("getDatabase", String.class);
            Object db = getDbMethod.invoke(mongoClient, database);

            Method getCollectionMethod = dbClass.getMethod("getCollection", String.class);
            Constructor<?> docCtor = documentClass.getConstructor();
            Method docAppend = documentClass.getMethod("append", String.class, Object.class);

            int totalRows = 0;

            totalRows += dumpCollectionMongo(db, getCollectionMethod, docCtor, docAppend, "tp_ignore", "player_uuid", "ignored_uuid");
            totalRows += dumpCollectionMongo(db, getCollectionMethod, docCtor, docAppend, "homes", "player_uuid", "home_name", "world", "x", "y", "z", "yaw", "pitch");
            totalRows += dumpCollectionMongo(db, getCollectionMethod, docCtor, docAppend, "warps", "name", "world", "x", "y", "z", "yaw", "pitch", "owner_uuid", "description", "created_at");
            totalRows += dumpCollectionMongo(db, getCollectionMethod, docCtor, docAppend, "economy", "player_uuid", "balance");
            totalRows += dumpCollectionMongo(db, getCollectionMethod, docCtor, docAppend, "economy_transactions", "id", "sender_uuid", "target_uuid", "amount", "transaction_type", "reason", "created_at");
            totalRows += dumpCollectionMongo(db, getCollectionMethod, docCtor, docAppend, "jails", "name", "world", "x", "y", "z", "yaw", "pitch");
            totalRows += dumpCollectionMongo(db, getCollectionMethod, docCtor, docAppend, "jailed_players", "player_uuid", "jail_name", "jailed_by", "reason", "jailed_at", "release_at");

            return new DumpResult(true, "Dump completed. " + totalRows + " documents exported to MongoDB.");
        } finally {
            mongoClient.getClass().getMethod("close").invoke(mongoClient);
        }
    }

    private int dumpCollectionMongo(Object db, Method getCollection, Constructor<?> docCtor, Method docAppend, String collectionName, String... fields) throws Exception {
        Object col = getCollection.invoke(db, collectionName);
        col.getClass().getMethod("drop").invoke(col);

        List<Object[]> rows = readH2Table(collectionName, fields);
        if (rows.isEmpty()) return 0;

        List<Object> documents = new ArrayList<>();
        for (Object[] row : rows) {
            Object doc = docCtor.newInstance();
            for (int i = 0; i < fields.length; i++) {
                docAppend.invoke(doc, fields[i], row[i]);
            }
            documents.add(doc);
        }

        col.getClass().getMethod("insertMany", List.class).invoke(col, documents);
        return rows.size();
    }

    private DumpResult dumpSql(Connection extConn) {
        List<String> errors = new ArrayList<>();
        int totalRows = 0;

        try (Statement stmt = extConn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            totalRows += dumpTableSql(stmt, "tp_ignore",
                    "CREATE TABLE IF NOT EXISTS tp_ignore (player_uuid VARCHAR(36), ignored_uuid VARCHAR(36), UNIQUE(player_uuid, ignored_uuid))",
                    "player_uuid", "ignored_uuid");

            totalRows += dumpTableSql(stmt, "homes",
                    "CREATE TABLE IF NOT EXISTS homes (player_uuid VARCHAR(36), home_name VARCHAR(32), world VARCHAR(100), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, PRIMARY KEY(player_uuid, home_name))",
                    "player_uuid", "home_name", "world", "x", "y", "z", "yaw", "pitch");

            totalRows += dumpTableSql(stmt, "warps",
                    "CREATE TABLE IF NOT EXISTS warps (name VARCHAR(32) PRIMARY KEY, world VARCHAR(100), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, owner_uuid VARCHAR(36), description VARCHAR(255), created_at BIGINT)",
                    "name", "world", "x", "y", "z", "yaw", "pitch", "owner_uuid", "description", "created_at");

            totalRows += dumpTableSql(stmt, "economy",
                    "CREATE TABLE IF NOT EXISTS economy (player_uuid VARCHAR(36) PRIMARY KEY, balance DOUBLE)",
                    "player_uuid", "balance");

            totalRows += dumpTableSql(stmt, "economy_transactions",
                    "CREATE TABLE IF NOT EXISTS economy_transactions (id INTEGER AUTO_INCREMENT PRIMARY KEY, sender_uuid VARCHAR(36), target_uuid VARCHAR(36), amount DOUBLE, transaction_type VARCHAR(50), reason VARCHAR(255), created_at BIGINT)",
                    "id", "sender_uuid", "target_uuid", "amount", "transaction_type", "reason", "created_at");

            totalRows += dumpTableSql(stmt, "jails",
                    "CREATE TABLE IF NOT EXISTS jails (name VARCHAR(32) PRIMARY KEY, world VARCHAR(100), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT)",
                    "name", "world", "x", "y", "z", "yaw", "pitch");

            totalRows += dumpTableSql(stmt, "jailed_players",
                    "CREATE TABLE IF NOT EXISTS jailed_players (player_uuid VARCHAR(36) PRIMARY KEY, jail_name VARCHAR(32), jailed_by VARCHAR(36), reason VARCHAR(255), jailed_at BIGINT, release_at BIGINT)",
                    "player_uuid", "jail_name", "jailed_by", "reason", "jailed_at", "release_at");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        } catch (SQLException e) {
            errors.add("Schema setup error: " + e.getMessage());
        }

        return new DumpResult(true, "Dump completed. " + totalRows + " rows exported." + (errors.isEmpty() ? "" : " Warnings: " + String.join("; ", errors)));
    }

    private int dumpTableSql(Statement extStmt, String table, String createSql, String... columns) throws SQLException {
        extStmt.execute("DROP TABLE IF EXISTS " + table);
        extStmt.execute(createSql);

        List<Object[]> rows = readH2Table(table, columns);
        if (rows.isEmpty()) return 0;

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns[i]);
        }
        sql.append(") VALUES ");
        for (int r = 0; r < rows.size(); r++) {
            if (r > 0) sql.append(", ");
            sql.append("(");
            Object[] row = rows.get(r);
            for (int c = 0; c < row.length; c++) {
                if (c > 0) sql.append(", ");
                if (row[c] == null) {
                    sql.append("NULL");
                } else if (row[c] instanceof Number || row[c] instanceof Boolean) {
                    sql.append(row[c]);
                } else {
                    sql.append("'").append(row[c].toString().replace("'", "''")).append("'");
                }
            }
            sql.append(")");
        }

        extStmt.execute(sql.toString());
        return rows.size();
    }

    private List<Object[]> readH2Table(String table, String... columns) {
        List<Object[]> result = new ArrayList<>();
        String query = "SELECT " + String.join(", ", columns) + " FROM " + table;

        try (Statement h2Stmt = plugin.databaseManager().getConnection().createStatement();
             ResultSet rs = h2Stmt.executeQuery(query)) {
            while (rs.next()) {
                Object[] row = new Object[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    row[i] = rs.getObject(columns[i]);
                }
                result.add(row);
            }
        } catch (SQLException ignored) {
        }

        return result;
    }

    private void loadDriver(String primary, String fallback, String name) throws SQLException {
        try {
            Class.forName(primary);
        } catch (ClassNotFoundException e) {
            if (fallback != null) {
                try {
                    Class.forName(fallback);
                    return;
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw new SQLException(name + " JDBC driver not found. Add the connector jar to your server's libraries.");
        }
    }

    public record DumpResult(boolean success, String message) {}
}
