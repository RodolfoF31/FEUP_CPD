package client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

public class TokenDB {
    private final String directory;

    public TokenDB(String directory) {
        this.directory = directory;
    }

    public void saveToken(String username, String token, String state) {
        try {
            JSONObject tokenObject = new JSONObject();
            tokenObject.put("token", token);
            tokenObject.put("state", state);

            try (FileWriter file = new FileWriter(directory + "/token-" + username + ".json")) {
                file.write(tokenObject.toString(2)); // Pretty print with indent of 2
            }
            System.out.println("Token and state saved for user: " + username);
        } catch (IOException e) {
            System.out.println("Error saving token and state for user: " + username);
            e.printStackTrace();
        }
    }

    public String loadToken(String username) {
        try {
            File file = new File(directory + "/token-" + username + ".json");
            if (!file.exists()) {
                return null;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject tokenObject = new JSONObject(content);
            return tokenObject.getString("token");
        } catch (IOException | JSONException e) {
            System.out.println("Error loading token for user: " + username);
            e.printStackTrace();
            return null;
        }
    }

    public String loadState(String username) {
        try {
            File file = new File(directory + "/token-" + username + ".json");
            if (!file.exists()) {
                return null;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject tokenObject = new JSONObject(content);
            return tokenObject.getString("state");
        } catch (IOException | JSONException e) {
            System.out.println("Error loading state for user: " + username);
            e.printStackTrace();
            return null;
        }
    }

    public void updateState(String username, String state) {
        try {
            File file = new File(directory + "/token-" + username + ".json");
            if (!file.exists()) {
                System.out.println("User file not found for: " + username);
                return;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject tokenObject = new JSONObject(content);
            tokenObject.put("state", state);

            try (FileWriter fileWriter = new FileWriter(directory + "/token-" + username + ".json")) {
                fileWriter.write(tokenObject.toString(2)); // Pretty print with indent of 2
            }
            System.out.println("State updated for user: " + username);
        } catch (IOException | JSONException e) {
            System.out.println("Error updating state for user: " + username);
            e.printStackTrace();
        }
    }

    public void resetAllStates() {
        try {
            Files.list(Paths.get(directory))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String content = new String(Files.readAllBytes(path));
                            JSONObject tokenObject = new JSONObject(content);
                            tokenObject.put("state", "none");

                            try (FileWriter file = new FileWriter(path.toFile())) {
                                file.write(tokenObject.toString(2)); // Pretty print with indent of 2
                            }
                            System.out.println("Reset state to 'none' for file: " + path.getFileName());
                        } catch (IOException | JSONException e) {
                            System.out.println("Error resetting state for file: " + path.getFileName());
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            System.out.println("Error listing files in directory: " + directory);
            e.printStackTrace();
        }
    }
}
