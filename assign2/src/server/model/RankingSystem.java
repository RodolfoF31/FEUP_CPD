package server.model;

public class RankingSystem {

    public static int initialRank = 1000;

    public static int calculateNewRank(int currentRank, int opponentRank, boolean isWinner) {
        int rankDifference = opponentRank - currentRank;
        int kFactor = 32;

        double expectedScore = 1 / (1.0 + Math.pow(10, rankDifference / 400.0));
        int score = isWinner ? 1 : 0;

        int newRank = (int) (currentRank + kFactor * (score - expectedScore));
        return Math.max(newRank, 0);
    }

}
