package server.model.guesses;

import server.network.PlayerHandler;

import java.util.LinkedList;
import java.util.Queue;

public class GuessQueue {
    private final Queue<Guess> queue = new LinkedList<>();

    public synchronized void addGuess(Guess guess) {
        queue.add(guess);
        notifyAll(); // Notify any waiting threads that a new guess is available
    }

    public synchronized Guess getNextGuess() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // Wait until a guess is available
        }
        return queue.poll(); // Retrieve and remove the next guess
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
