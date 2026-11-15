package org.voxelware.coretuff.dashboard.players;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class PlayerDataRepository {

    private static final Logger LOG = Logger.getLogger(PlayerDataRepository.class.getName());
    private final Connection connection;

    public PlayerDataRepository(Connection connection) {
        this.connection = connection;
        createTable();
    }

    private void createTable() {
        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(64),
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL,
                    playtime BIGINT DEFAULT 0
                )
            """);
        } catch (SQLException e) {
            LOG.warning("Failed to create player_data table: " + e.getMessage());
        }
    }

    public void upsert(UUID uuid, String name, long firstSeen, long lastSeen, long playtime) {
        String sql = """
            MERGE INTO player_data (player_uuid, player_name, first_seen, last_seen, playtime)
            KEY (player_uuid) VALUES (?, ?, ?, ?, ?)
        """;
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setLong(3, firstSeen);
            stmt.setLong(4, lastSeen);
            stmt.setLong(5, playtime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Failed to upsert player data: " + e.getMessage());
        }
    }

    public void updateLastSeen(UUID uuid, long lastSeen) {
        String sql = "UPDATE player_data SET last_seen = ? WHERE player_uuid = ?";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, lastSeen);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Failed to update last seen: " + e.getMessage());
        }
    }

    public void addPlaytime(UUID uuid, long delta) {
        String sql = "UPDATE player_data SET playtime = playtime + ? WHERE player_uuid = ?";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, delta);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Failed to add playtime: " + e.getMessage());
        }
    }

    public PlayerData getByUuid(UUID uuid) {
        String sql = "SELECT * FROM player_data WHERE player_uuid = ?";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return fromResultSet(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Failed to get player data: " + e.getMessage());
        }
        return null;
    }

    public List<PlayerData> search(String query, int limit) {
        String sql = "SELECT * FROM player_data WHERE player_name ILIKE ? OR player_uuid ILIKE ? ORDER BY last_seen DESC LIMIT ?";
        List<PlayerData> results = new ArrayList<>();
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            stmt.setString(2, "%" + query + "%");
            stmt.setInt(3, limit);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.warning("Failed to search players: " + e.getMessage());
        }
        return results;
    }

    public List<PlayerData> getRecentPlayers(int limit) {
        String sql = "SELECT * FROM player_data ORDER BY last_seen DESC LIMIT ?";
        List<PlayerData> results = new ArrayList<>();
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.warning("Failed to get recent players: " + e.getMessage());
        }
        return results;
    }

    private PlayerData fromResultSet(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("player_uuid"));
        String name = rs.getString("player_name");
        long firstSeen = rs.getLong("first_seen");
        long lastSeen = rs.getLong("last_seen");
        long playtime = rs.getLong("playtime");
        return new PlayerData(uuid, name, firstSeen, lastSeen, playtime);
    }
}
