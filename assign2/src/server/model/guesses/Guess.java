package server.model.guesses;

import server.network.PlayerHandler;

public class Guess {
    private final PlayerHandler player;
    private final int guessValue;

    public Guess(PlayerHandler player, int guessValue) {
        this.player = player;
        this.guessValue = guessValue;
    }

    public PlayerHandler getPlayerHandler() {
        return player;
    }

    public int getGuess() {
        return guessValue;
    }
}
