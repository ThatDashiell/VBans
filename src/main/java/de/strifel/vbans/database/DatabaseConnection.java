package de.strifel.vbans.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {

    private final HikariDataSource dataSource;

    public static final String BANNED_CRITERIA = "purged IS NULL and ((reducedUntil is NULL and (until = -1 or until > ?)) or (reducedUntil = -1 or reducedUntil > ?))";

    private static final String INSERT_BAN = "INSERT INTO ban_bans (user, until, bannedBy, reason, issuedAt) VALUES (?, ?, ?, ?, ?)";
    private static final String GET_BAN = "SELECT id, reason, until, bannedBy, reducedUntil, issuedAt FROM ban_bans WHERE " + BANNED_CRITERIA + " and user = ? LIMIT 1";
    private static final String GET_BAN_HISTORY = "SELECT id, reason, until, bannedBy, reducedUntil, issuedAt, purged, reducedBy  FROM ban_bans WHERE user = ?";
    private static final String SET_USERNAME = "INSERT INTO ban_nameCache (user, username) VALUES (?, ?)";
    private static final String UPDATE_USERNAME = "UPDATE ban_nameCache SET username=? WHERE user=?";
    private static final String GET_USERNAME = "SELECT username FROM ban_nameCache WHERE user=? LIMIT 1";
    private static final String GET_UUID = "SELECT user FROM ban_nameCache WHERE username=? LIMIT 1";
    private static final String PURGE_BANS = "UPDATE ban_bans SET purged=? WHERE " + BANNED_CRITERIA + " and user = ?";
    private static final String PURGE_BAN = "UPDATE ban_bans SET purged=? WHERE user = ? AND id=?";
    private static final String REDUCE_BANS = "UPDATE ban_bans SET reducedUntil=?, reducedBy=?, reducedAt=? WHERE " + BANNED_CRITERIA + " AND user=?";
    private static final String GET_USERNAMES_BASE = "SELECT username FROM ban_bans INNER JOIN ban_nameCache ON ban_bans.user = ban_nameCache.user WHERE GROUP BY username";
    private static final String GET_BAN_COUNT = "SELECT count(*) FROM ban_bans WHERE " + BANNED_CRITERIA;

    public DatabaseConnection(String server, int port, String username, String password, String database) throws ClassNotFoundException, SQLException {
        synchronized (this) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            HikariConfig config = new HikariConfig();
            String url = String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", server, port, database, username, password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(url);
            config.setMaximumPoolSize(10);
            dataSource = new HikariDataSource(config);
            createDefaultTable();
        }
    }

    public boolean addBan(String banned, long until, String by, String reason) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(INSERT_BAN)) {
            statement.setString(1, banned);
            statement.setLong(2, until);
            statement.setString(3, by);
            statement.setString(4, reason);
            statement.setLong(5, System.currentTimeMillis() / 1000);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Ban getBan(String userUUID) {
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(GET_BAN)) {
            statement.setLong(1, System.currentTimeMillis() / 1000);
            statement.setLong(2, System.currentTimeMillis() / 1000);
            statement.setString(3, userUUID);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return new Ban(result.getLong("id"), userUUID, result.getString("bannedBy"), result.getString("reason"), result.getLong("until"), result.getLong("issuedAt"), result.getLong("reducedUntil"));
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<HistoryBan> getBanHistory(String userUUID, boolean includePurged) {
        ArrayList<HistoryBan> bans = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(GET_BAN_HISTORY + (includePurged ? "" : " AND PURGED IS NULL"))) {
            statement.setString(1, userUUID);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                do {
                    bans.add(new HistoryBan(
                            result.getLong("id"),
                            userUUID, result.getString("bannedBy"),
                            result.getString("reason"),
                            result.getLong("until"),
                            result.getLong("issuedAt"),
                            result.getLong("reducedUntil"),
                            result.getString("purged"),
                            result.getString("reducedBy")));
                } while (result.next());
            }
            return bans;
        } catch (SQLException e) {
            e.printStackTrace();
            return bans;
        }
    }

    String getUsername(String userUUID) {
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(GET_USERNAME)) {
            statement.setString(1, userUUID);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("username");
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public String getUUID(String username) {
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(GET_UUID)) {
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("user");
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setUsername(String userUUID, String username) {
        String inDatabase = getUsername(userUUID);
        if (inDatabase == null) {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(SET_USERNAME)) {
                statement.setString(1, userUUID);
                statement.setString(2, username);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (!inDatabase.equals(username)) {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(UPDATE_USERNAME)) {
                statement.setString(1, username);
                statement.setString(2, userUUID);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void createDefaultTable() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ban_bans (id int(12) NOT NULL AUTO_INCREMENT, user text(36) NOT NULL, bannedBy text(36) NOT NULL, until int(64), issuedAt int(64), reducedUntil int(64), reducedBy text(36), reducedAt int(64), reason text(512), purged text(36), primary key (id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ban_nameCache (user varchar(36) NOT NULL PRIMARY KEY, username text(16))");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public void purgeActiveBans(String userUUID, String purgerUUID) {
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(PURGE_BANS)) {
            statement.setString(1, purgerUUID);
            statement.setLong(2, System.currentTimeMillis() / 1000);
            statement.setLong(3, System.currentTimeMillis() / 1000);
            statement.setString(4, userUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void purgeBanById(String userUUID, String purgerUUID, int id) {
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(PURGE_BAN)) {
            statement.setString(1, purgerUUID);
            statement.setString(2, userUUID);
            statement.setInt(3, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void reduceBanTo(String userUUID, String reducerUUID, long reduceTo) {
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(REDUCE_BANS)) {
            statement.setLong(1, reduceTo);
            statement.setString(2, reducerUUID);
            statement.setLong(3, System.currentTimeMillis() / 1000);
            statement.setLong(4, System.currentTimeMillis() / 1000);
            statement.setLong(5, System.currentTimeMillis() / 1000);
            statement.setString(6, userUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getUsernamesByQuery(String query) {
        List<String> usernames = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet results = statement.executeQuery(GET_USERNAMES_BASE.replace("WHERE", query));
            if (results.next()) {
                do {
                    usernames.add(results.getString("username"));
                } while (results.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usernames;
    }

    public int getBannedCount() {
        try (Connection connection = dataSource.getConnection();PreparedStatement statement = connection.prepareStatement(GET_BAN_COUNT)) {
            statement.setLong(1, System.currentTimeMillis() / 1000);
            statement.setLong(2, System.currentTimeMillis() / 1000);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(0);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
