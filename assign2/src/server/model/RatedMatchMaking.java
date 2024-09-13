package server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import server.network.PlayerHandler;

public class RatedMatchMaking implements Runnable {
    private PriorityQueue<PlayerHandler> matchMakingQueue;
    private int maxPlayers;
    private int maxRatingDifference;

    public RatedMatchMaking(int maxPlayers, int maxRatingDifference) {
        matchMakingQueue = new PriorityQueue<>((p1, p2) -> p2.getRank() - p1.getRank());
        this.maxPlayers = maxPlayers;
        this.maxRatingDifference = maxRatingDifference;
    }

    public synchronized void addPlayer(PlayerHandler player) {
        matchMakingQueue.add(player);
    }

    // Find a group of players that can be matched together
    public synchronized Lobby findPossibleMatch() {
        if (matchMakingQueue.size() >= maxPlayers) {
            List<List<PlayerHandler>> possibleGroups = getAllPossibleGroups();
            for (List<PlayerHandler> group : possibleGroups) {
                if (isGroupValid(group)) {
                    Lobby lobby = new Lobby(maxPlayers, GameMode.RANKED);
                    for (PlayerHandler player : group) {
                        lobby.enterLobby(player);
                        matchMakingQueue.remove(player);
                    }
                    System.out.println("Match found!");
                    return lobby;
                }
            }
        }
        return null;
    }

    private boolean isGroupValid(List<PlayerHandler> group) {
        for (int i = 0; i < group.size(); i++) {
            for (int j = i+1; j < group.size(); j++) {
                if (Math.abs(group.get(i).getRank() - group.get(j).getRank()) > maxRatingDifference) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<List<PlayerHandler>> getAllPossibleGroups() {
        List<List<PlayerHandler>> possibleGroups = new ArrayList<>();
        List<PlayerHandler> players = new ArrayList<>(matchMakingQueue);


        for (int i = 0; i < players.size()-maxPlayers+1; i++) {
            List<PlayerHandler> group = new ArrayList<>();
            for (int j = 0; j < maxPlayers; j++) {
                group.add(players.get(i+j));
            }
            possibleGroups.add(group);
        }

        // System.out.println("Possible groups: " + possibleGroups.size());
        return possibleGroups;
    }

    public synchronized boolean removePlayer(String playerName) {
        PlayerHandler playerToRemove = null;
        for (PlayerHandler player : matchMakingQueue) {
            if (player.getUsername().equals(playerName)) {
                playerToRemove = player;
                break;
            }
        }
        if (playerToRemove != null) {
            return matchMakingQueue.remove(playerToRemove);
        }
        return false;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Lobby lobby = findPossibleMatch();
                if (lobby != null) {
                    // Add the lobby to the lobby manager
                    LobbyManager.getInstance(null).addLobby(lobby);

                    // Reset the maximum rating difference if a match is found
                    maxRatingDifference = 100;
                }

                // Increase the maximum rating difference if the queue has enough players to form a group
                if (matchMakingQueue.size() >= maxPlayers) {
                    System.out.println("Trying to find a match with " + maxRatingDifference +  " maximum rating difference.");
                    maxRatingDifference++;
                }
                else {
                    maxRatingDifference = 100;
                }

                Thread.sleep(1000); // Sleep for 1 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
