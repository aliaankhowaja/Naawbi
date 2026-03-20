package com.ibwaan.naawbi.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class DB {
    private static DB instance;
    private Connection connection;
    private final String url = "jdbc:postgresql://localhost:5432/naawbi";
    private final String user = "postgres";
    private final String password = "postgres";

    private DB() {
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static DB getInstance() {
        if (instance == null) {
            synchronized (DB.class) {
                if (instance == null) {
                    instance = new DB();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Statement statement = getConnection().createStatement();
        return statement.executeQuery(query);
    }

    public int executeUpdate(String query) throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            return statement.executeUpdate(query);
        }
    }

    public PreparedStatement prepareStatement(String query) throws SQLException {
        return getConnection().prepareStatement(query);
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void createTables() {
        String userTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id SERIAL PRIMARY KEY,"
                + "username VARCHAR(50) UNIQUE NOT NULL,"
                + "password VARCHAR(100) NOT NULL,"
                + "email VARCHAR(100) UNIQUE NOT NULL"
                + ");";

        String courseTable = "CREATE TABLE IF NOT EXISTS courses ("
                + "id SERIAL PRIMARY KEY,"
                + "course_name VARCHAR(100) NOT NULL,"
                + "course_code VARCHAR(20) UNIQUE NOT NULL,"
                + "description TEXT,"
                + "created_by INTEGER REFERENCES users(id),"
                + "is_active BOOLEAN DEFAULT FALSE"
                + ");";

        String courseEnrollmentsTable = "CREATE TABLE IF NOT EXISTS course_enrollments ("
                + "id SERIAL PRIMARY KEY,"
                + "course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,"
                + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                + "role VARCHAR(50) DEFAULT 'student',"
                + "enrolled_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE(course_id, user_id)"
                + ");";

        String announcementsTable = "CREATE TABLE IF NOT EXISTS announcements ("
                + "id SERIAL PRIMARY KEY,"
                + "course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,"
                + "created_by INTEGER NOT NULL REFERENCES users(id),"
                + "title VARCHAR(255) NOT NULL,"
                + "content TEXT NOT NULL,"
                + "content_type VARCHAR(20) DEFAULT 'html',"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");"
                + "CREATE INDEX IF NOT EXISTS idx_announcements_course_date ON announcements(course_id, created_at DESC);";

        try (Statement statement = getConnection().createStatement()) {
            statement.execute(userTable);
            statement.execute(courseTable);
            statement.execute(courseEnrollmentsTable);
            statement.execute(announcementsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
