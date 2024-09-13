package server.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.json.*;

public class Database {
    private JSONObject database;
    private final String filename;

    public Database(String filename) throws IOException, JSONException {
        this.filename = filename;
        File file = new File(filename);

        if (file.exists() && !file.isDirectory()) {
            // Load the existing database
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            database = new JSONObject(content);
        } else {
            // Create a new database
            database = new JSONObject();
            database.put("users", new JSONArray());
            backup(); // Save the empty database
        }
    }

    public synchronized void backup() throws IOException {
        try (FileWriter file = new FileWriter(filename)) {
            file.write(database.toString(1)); // Pretty print with indent of 2
        }
    }

    // Get the last activity time of a user
    public synchronized long getLastActiveTime(String username) {
        try {
            JSONArray users = database.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                if (user.getString("username").equals(username)) {
                    return user.getLong("lastActiveTime");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return 15; // User not found or last activity time not set
    }

    public synchronized boolean authUserPass(String username, String password) {
        try {
            JSONArray users = database.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                if (user.getString("username").equals(username)) {
                    // Compare the hashed input password with the stored hash
                    String storedPasswordHash = user.getString("passwordHash");
                    String hashedInputPassword = hashPassword(password);
                    return storedPasswordHash.equals(hashedInputPassword);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Register a new user
    public synchronized boolean register(String username, String password) throws IOException {
        try {
            JSONArray users = database.getJSONArray("users");

            // Check if username already exists
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                if (user.getString("username").equals(username)) {
                    return false; // Username already exists
                }
            }

            // Hash the password
            String passwordHash = hashPassword(password);

            // Create a new user object
            JSONObject newUser = new JSONObject();
            newUser.put("username", username);
            newUser.put("passwordHash", passwordHash);
            newUser.put("rank", 1000);
            newUser.put("token", ""); // Empty token for now

            users.put(newUser);
            backup();
            return true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update the user's rank
    public synchronized void updateSetRankPlayer(String username, int newRank) throws IOException {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("username").equals(username)) {
                user.put("rank", newRank);
                backup();
                break;
            }
        }
    }

    public synchronized int getRank(String username) {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("username").equals(username)) {
                return user.getInt("rank");
            }
        }
        return -1;
    }

    public synchronized String generateToken(String username) throws IOException {
        String token = UUID.randomUUID().toString();
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("username").equals(username)) {
                user.put("token", token);
                backup();
                break;
            }
        }
        return token;
    }

    // Update the makeTokenInval method to return a boolean
    public synchronized boolean makeTokenInval(String token) throws IOException {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (token.equals(user.getString("token"))) {
                user.put("token", "");
                backup();
                return true;
            }
        }
        return false;
    }

    // Find a user by token
    public synchronized String findUserByToken(String token) {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("token").equals(token)) {
                return user.getString("username");
            }
        }
        return null;
    }

    // Find a user by token
    public synchronized String getTokenByUsername(String username) {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("username").equals(username)) {
                return user.getString("token");
            }
        }
        return null;
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    // Call this method after successful authentication
    public synchronized boolean loginUser(String username) throws IOException {
        String token = generateToken(username);
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("username").equals(username)) {
                user.put("token", token);
                user.put("isLoggedIn", true);
                user.put("lastActiveTime", System.currentTimeMillis());
                backup();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean loginWithToken(String username, String token) throws IOException {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("username").equals(username) && user.getString("token").equals(token)) {
                user.put("isLoggedIn", true);
                user.put("lastActiveTime", System.currentTimeMillis());
                backup();
                return true;
            }
        }
        return false;
    }

    // Call this method to log out a user
    public synchronized boolean logoutUser(String username) throws IOException {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("username").equals(username) && user.getBoolean("isLoggedIn")) {
                user.put("token", "");
                user.put("isLoggedIn", false);
                backup();
                return true;
            }
        }
        return false;
    }

    // Call this method to check if the session is still valid
    public synchronized boolean isSessionActive(String token) {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("token").equals(token)) {
                long lastActiveTime = user.getLong("lastActiveTime");
                return System.currentTimeMillis() - lastActiveTime < getSessionTimeout();
            }
        }
        return false;
    }

    // Implement a timeout for the session
    private long getSessionTimeout() {
        return 10 * 60 * 1000; // 2 minutes
    }

    // Update the user's last active time and set session expiry
    public synchronized boolean updateSessionActivity(String token) throws IOException {
        JSONArray users = database.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.getString("token").equals(token)) {
                long currentTime = System.currentTimeMillis();
                user.put("lastActiveTime", currentTime);
                user.put("sessionExpiry", currentTime + getSessionTimeout()); // Extend session expiry
                backup();
                return true;
            }
        }
        return false;
    }

}