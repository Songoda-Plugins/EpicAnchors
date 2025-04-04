package com.songoda.epicanchors.files.migration;

import com.songoda.core.database.DataMigration;
import com.songoda.epicanchors.files.AnchorsDataManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _1_InitialMigration extends DataMigration {
    public _1_InitialMigration() {
        super(1);
    }

    @Override
    public void migrate(Connection connection, String tablePrefix) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE " + AnchorsDataManager.getAnchorTable(tablePrefix) +
                            "(" +
                            "id INTEGER NOT NULL PRIMARY KEY auto_increment," +
                            "world_name TEXT NOT NULL," +
                            "x INTEGER NOT NULL," +
                            "y INTEGER NOT NULL," +
                            "z INTEGER NOT NULL," +
                            "ticks_left INTEGER NOT NULL," +
                            "owner VARCHAR(36)" +
                            ");");
        }
    }
}
