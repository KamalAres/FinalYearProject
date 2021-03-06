package finalyear.project.cse.database;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import static finalyear.project.cse.database.SQLLoader.INSERT_IGNORE_CHANNELS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChannelsTable extends TableHelper<YouTubeChannel> {

    private final Cache<String, YouTubeChannel> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    public ChannelsTable(final Connection connection) {
        super(connection);
    }

    @Override
    public YouTubeChannel to(ResultSet resultSet) throws SQLException {
        return new YouTubeChannel(resultSet.getString("channel_id"),
                resultSet.getString("channel_name"),
                resultSet.getString("channel_profile_url"));
    }

    public void check(String id) {
        getOrNull(id);
    }

    public YouTubeChannel getOrNull(String id) {
        try {
            return get(id);
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public YouTubeChannel get(String id) throws SQLException {
        YouTubeChannel channel = cache.getIfPresent(id);
        if (channel != null) {
            return channel;
        }

        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM channels WHERE channel_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    channel = to(rs);
                    cache.put(channel.getId(), channel);
                    return channel;
                }
            }
        }

        return null;
    }

    @Override
    public List<YouTubeChannel> getAll() throws SQLException {
        List<YouTubeChannel> results = new ArrayList<>();

        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM channels")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(to(rs));
                }
            }
        }

        return results;
    }

    @Override
    public boolean exists(String id) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "SELECT channel_id FROM channels WHERE channel_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void insertAll(List<YouTubeChannel> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(INSERT_IGNORE_CHANNELS.toString())) {
            for (YouTubeChannel c : objects) {
                ps.setString(1, c.getId());
                ps.setString(2, c.getTitle());
                ps.setString(3, c.getThumbUrl());
                ps.setBoolean(4, false);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public void deleteAll(List<YouTubeChannel> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "DELETE FROM channels WHERE channel_id = ?")) {
            for (YouTubeChannel channel : objects) {
                ps.setString(1, channel.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    @Override
    public void updateAll(List<YouTubeChannel> objects) throws SQLException {
        // not updating, ignoring if exists
    }

}
