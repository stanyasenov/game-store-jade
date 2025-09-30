package com.gamestore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.gamestore.model.Game;

public class RelationalDBConnector {
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:gamestop.db";
    
    public RelationalDBConnector() {
        try {
            // Create connection
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite database successfully");
            
            // Initialize the database if it doesn't exist
            initializeDatabase();
            
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeDatabase() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create games table if it doesn't exist
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS games (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "genre TEXT NOT NULL," +
                "price REAL NOT NULL," +
                "stock INTEGER NOT NULL," +
                "release_date TEXT," +
                "esrb_rating TEXT," +
                "description TEXT," +
                "publisher TEXT," +
                "developer TEXT" +
                ")"
            );
            
            // Create platforms table if it doesn't exist
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS game_platforms (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "game_id INTEGER NOT NULL," +
                "platform_name TEXT NOT NULL," +
                "FOREIGN KEY (game_id) REFERENCES games (id)" +
                ")"
            );
            
            // Create features table if it doesn't exist
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS game_features (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "game_id INTEGER NOT NULL," +
                "feature_name TEXT NOT NULL," +
                "FOREIGN KEY (game_id) REFERENCES games (id)" +
                ")"
            );
            
            // Check if we have any sample data, if not, insert some
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM games");
            if (rs.next() && rs.getInt(1) == 0) {
                insertSampleData();
            }
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void insertSampleData() {
        try {
            // Insert sample games
            PreparedStatement insertGame = connection.prepareStatement(
                "INSERT INTO games (title, genre, price, stock, release_date, esrb_rating, description, publisher, developer) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            
            // Elden Ring( Also in Ontology)
            insertGame.setString(1, "Elden Ring");
            insertGame.setString(2, "RolePlaying");
            insertGame.setDouble(3, 59.99);
            insertGame.setInt(4, 10);
            insertGame.setString(5, "2022-02-25");
            insertGame.setString(6, "M");
            insertGame.setString(7, "An open-world action RPG created in collaboration with George R.R. Martin");
            insertGame.setString(8, "FromSoftware");
            insertGame.setString(9, "FromSoftware");
            insertGame.executeUpdate();
            
            ResultSet rs = insertGame.getGeneratedKeys();
            if (rs.next()) {
                int eldenRingId = rs.getInt(1);
                
                // Add platforms for Elden Ring
                addPlatform(eldenRingId, "PlayStation 5");
                addPlatform(eldenRingId, "Xbox Series X");
                addPlatform(eldenRingId, "Windows PC");
                
                // Add features for Elden Ring
                addFeature(eldenRingId, "Online Multiplayer");
            }
            
            // Add more games that aren't in the ontology
            insertGame.setString(1, "Call of Duty: Modern Warfare");
            insertGame.setString(2, "Action");
            insertGame.setDouble(3, 69.99);
            insertGame.setInt(4, 15);
            insertGame.setString(5, "2019-10-25");
            insertGame.setString(6, "M");
            insertGame.setString(7, "First-person shooter developed by Infinity Ward");
            insertGame.setString(8, "Activision");
            insertGame.setString(9, "Infinity Ward");
            insertGame.executeUpdate();
            
            rs = insertGame.getGeneratedKeys();
            if (rs.next()) {
                int codId = rs.getInt(1);
                
                // Add platforms for COD
                addPlatform(codId, "PlayStation 5");
                addPlatform(codId, "Xbox Series X");
                addPlatform(codId, "Windows PC");
                
                // Add features for COD
                addFeature(codId, "Online Multiplayer");
                addFeature(codId, "Campaign Mode");
            }
            
            insertGame.setString(1, "Animal Crossing: New Horizons");
            insertGame.setString(2, "Simulation");
            insertGame.setDouble(3, 49.99);
            insertGame.setInt(4, 20);
            insertGame.setString(5, "2020-03-20");
            insertGame.setString(6, "E");
            insertGame.setString(7, "Life simulation game developed by Nintendo");
            insertGame.setString(8, "Nintendo");
            insertGame.setString(9, "Nintendo");
            insertGame.executeUpdate();
            
            rs = insertGame.getGeneratedKeys();
            if (rs.next()) {
                int acId = rs.getInt(1);
                
                // Add platforms for Animal Crossing
                addPlatform(acId, "Nintendo Switch");
                
                // Add features for Animal Crossing
                addFeature(acId, "Online Multiplayer");
                addFeature(acId, "Seasonal Events");
            }
            
            System.out.println("Sample data inserted successfully");
            
        } catch (SQLException e) {
            System.err.println("Error inserting sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addPlatform(int gameId, String platformName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
            "INSERT INTO game_platforms (game_id, platform_name) VALUES (?, ?)"
        );
        stmt.setInt(1, gameId);
        stmt.setString(2, platformName);
        stmt.executeUpdate();
        stmt.close();
    }
    
    private void addFeature(int gameId, String featureName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
            "INSERT INTO game_features (game_id, feature_name) VALUES (?, ?)"
        );
        stmt.setInt(1, gameId);
        stmt.setString(2, featureName);
        stmt.executeUpdate();
        stmt.close();
    }
    
    public Game findGame(String title) {
        try {
            String cleanTitle = title.replace("\"", "");
            
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM games WHERE title LIKE ?"
            );
            stmt.setString(1, "%" + cleanTitle + "%");
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Game game = new Game(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("genre"),
                    rs.getDouble("price"),
                    rs.getInt("stock")
                );
                
                game.setSource("SQLite Database");
                
                if (rs.getString("release_date") != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        game.setReleaseDate(sdf.parse(rs.getString("release_date")));
                    } catch (ParseException e) {
                        System.err.println("Error parsing date: " + e.getMessage());
                    }
                }
                if (rs.getString("esrb_rating") != null) {
                    game.setEsrbRating(rs.getString("esrb_rating"));
                }
                if (rs.getString("description") != null) {
                    game.setDescription(rs.getString("description"));
                }
                if (rs.getString("publisher") != null) {
                    game.setPublisher(rs.getString("publisher"));
                }
                if (rs.getString("developer") != null) {
                    game.setDeveloper(rs.getString("developer"));
                }
                
                PreparedStatement platformStmt = connection.prepareStatement(
                    "SELECT platform_name FROM game_platforms WHERE game_id = ?"
                );
                platformStmt.setInt(1, game.getId());
                ResultSet platformRs = platformStmt.executeQuery();
                
                while (platformRs.next()) {
                    game.addPlatform(platformRs.getString("platform_name"));
                }
                platformStmt.close();
                
                PreparedStatement featureStmt = connection.prepareStatement(
                    "SELECT feature_name FROM game_features WHERE game_id = ?"
                );
                featureStmt.setInt(1, game.getId());
                ResultSet featureRs = featureStmt.executeQuery();
                
                while (featureRs.next()) {
                    game.addFeature(featureRs.getString("feature_name"));
                }
                featureStmt.close();
                
                stmt.close();
                return game;
            }
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error querying database: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public List<Game> findGamesByGenre(String genre) {
        List<Game> games = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM games WHERE genre LIKE ?"
            );
            stmt.setString(1, "%" + genre + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Game game = new Game(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("genre"),
                    rs.getDouble("price"),
                    rs.getInt("stock")
                );
                games.add(game);
            }
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error querying database: " + e.getMessage());
            e.printStackTrace();
        }
        return games;
    }
    public List<Game> getAllGames() {
        List<Game> games = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM games ORDER BY title");
            
            while (rs.next()) {
                Game game = new Game(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("genre"),
                    rs.getDouble("price"),
                    rs.getInt("stock")
                );
                
                game.setSource("SQLite Database");
                
                if (rs.getString("release_date") != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        game.setReleaseDate(sdf.parse(rs.getString("release_date")));
                    } catch (ParseException e) {
                        System.err.println("Error parsing date: " + e.getMessage());
                    }
                }
                if (rs.getString("esrb_rating") != null) {
                    game.setEsrbRating(rs.getString("esrb_rating"));
                }
                if (rs.getString("description") != null) {
                    game.setDescription(rs.getString("description"));
                }
                if (rs.getString("publisher") != null) {
                    game.setPublisher(rs.getString("publisher"));
                }
                if (rs.getString("developer") != null) {
                    game.setDeveloper(rs.getString("developer"));
                }
                
                PreparedStatement platformStmt = connection.prepareStatement(
                    "SELECT platform_name FROM game_platforms WHERE game_id = ?");
                platformStmt.setInt(1, game.getId());
                ResultSet platformRs = platformStmt.executeQuery();
                
                while (platformRs.next()) {
                    game.addPlatform(platformRs.getString("platform_name"));
                }
                platformStmt.close();
                
                PreparedStatement featureStmt = connection.prepareStatement(
                    "SELECT feature_name FROM game_features WHERE game_id = ?");
                featureStmt.setInt(1, game.getId());
                ResultSet featureRs = featureStmt.executeQuery();
                
                while (featureRs.next()) {
                    game.addFeature(featureRs.getString("feature_name"));
                }
                featureStmt.close();
                
                games.add(game);
            }
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error querying all games: " + e.getMessage());
            e.printStackTrace();
        }
        return games;
    }
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}