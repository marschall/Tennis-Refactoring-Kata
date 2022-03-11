import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class GameStateMatchine {

    private static final FlipableGameState ADVANTAGE_SERVER = new AdvantageServer();

    private static final FlipableGameState ADVANTAGE_RECIVER = new AdvantageReceiver();

    private static final FlipableGameState WIN_SERVER = new WinServer();

    private static final FlipableGameState WIN_RECIEVER = new WinReceiver();

    private static final FlipableGameState DEUCE = new Deuce();

    private static final GameState LOVE_ALL = computeInitialState();

    private interface FlipableGameState extends GameState {

        FlipableGameState flipPlayerScores();

    }

    private static GameState computeInitialState() {

        Map<GameStateLookupKey, FlipableGameState> computed = new HashMap<>();

        // initialize the map with well known, special states
        computed.put(new GameStateLookupKey(3, 3), DEUCE);
        computed.put(new GameStateLookupKey(4, 4), DEUCE);

        computed.put(new GameStateLookupKey(4, 3), ADVANTAGE_SERVER);
        computed.put(new GameStateLookupKey(3, 4), ADVANTAGE_RECIVER);

        // all wins that weren't proceeded with "Advantage player X"
        for (int i = 0; i <= 2; i++) {
            GameStateLookupKey key = new GameStateLookupKey(4, i);
            computed.put(key, WIN_SERVER);
            computed.put(key.flipPlayerScores(), WIN_RECIEVER);
        }

        // Compute a game state by looking up the two possible following ones
        // (either the server or the receiver wins).
        // Do it in reverse order as the terminal have already been added to the map
        // above.
        //
        // No need to start with 4, because all 4 based states are already in the map.
        // They are either wins or "Advantage player X".
        for (int i = 3; i > 0; i--) {
            for (int j = i; j >= 0; j--) {
                GameStateLookupKey key = new GameStateLookupKey(i, j);
                // ignore the terminal and special cases added above
                if (!computed.containsKey(key)) {
                    FlipableGameState serverWins = computed.get(key.incrementServer());
                    FlipableGameState receiverWins = computed.get(key.incrementReceiver());
                    if (i == j) {
                        FlipableGameState state = new All(i, serverWins, receiverWins);
                        computed.put(key, state);
                    } else {
                        FlipableGameState state = new GenericGameState(i, j, serverWins, receiverWins);
                        computed.put(key, state);
                        // if i-j wasn't in the map then j-i will also not be in the map, no need to check
                        computed.put(key.flipPlayerScores(), state.flipPlayerScores());
                    }

                }
            }

        }
        return new All(0, computed.get(new GameStateLookupKey(1, 0)), computed.get(new GameStateLookupKey(0, 1)));
    }

    static GameState getInitialState() {
        return LOVE_ALL;
    }

    /**
     * Look up a {@link GameState} by the score of each player.
     */
    static final class GameStateLookupKey {
        // REVIEW no need to use 2 ints, fits into 1 byte
        // can save 8 bytes, see JDK-8237767

        private final int serverScore;

        private final int receiverScore;

        GameStateLookupKey(int serverScore, int receiverScore) {
            if (serverScore > 4 || serverScore < 0) {
                throw new IllegalArgumentException("invalid serverScore");
            }
            if (receiverScore > 4 || receiverScore < 0) {
                throw new IllegalArgumentException("invalid receiverScore");
            }
            this.serverScore = serverScore;
            this.receiverScore = receiverScore;
        }

        GameStateLookupKey incrementServer() {
            return new GameStateLookupKey(this.serverScore + 1, this.receiverScore);
        }

        GameStateLookupKey incrementReceiver() {
            return new GameStateLookupKey(this.serverScore, this.receiverScore + 1);
        }

        GameStateLookupKey flipPlayerScores() {
            return new GameStateLookupKey(this.receiverScore, this.serverScore);
        }

        @Override
        public int hashCode() {
            // serverScore and receiverScore don't exceed 4
            // this gives us a unique hashCode for all used values
            // by only using the lower 6 bits
            return Integer.rotateLeft(serverScore, 3)  ^  receiverScore;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GameStateLookupKey)) {
                return false;
            }
            GameStateLookupKey other = (GameStateLookupKey) obj;
            return serverScore == other.serverScore
                    && receiverScore == other.receiverScore;
        }

        @Override
        public String toString() {
            return "(" + this.serverScore + ", " + this.receiverScore + ")";
        }

    }

    private static String translate(int score) {
        switch (score) {
            case 0:
                return "Love";
            case 1:
                return "Fifteen";
            case 2:
                return "Thirty";
            case 3:
                return "Forty";
            default:
                throw new IllegalArgumentException("unsupported score: " + score);
        }
    }

    private static final class GenericGameState implements FlipableGameState {
        // REVIEW no need to use 2 ints, fits into 1 byte
        // can save 8 bytes, see JDK-8237767

        private final int serverScore;

        private final int receiverScore;

        private final FlipableGameState serverWins;

        private final FlipableGameState receiverWins;

        GenericGameState(int serverScore, int receiverScore, FlipableGameState serverWins, FlipableGameState receiverWins) {
            if (serverScore >= 4) {
                throw new IllegalArgumentException(AdvantageServer.class + " shold be used");
            }
            if (serverScore < 0) {
                throw new IllegalArgumentException("negative score for server");
            }
            if (receiverScore >= 4) {
                throw new IllegalArgumentException(AdvantageReceiver.class + " shold be used");
            }
            if (receiverScore < 0) {
                throw new IllegalArgumentException("negative score for receiver");
            }
            this.serverScore = serverScore;
            this.receiverScore = receiverScore;
            Objects.requireNonNull(serverWins, "serverWins");
            Objects.requireNonNull(receiverWins, "receiverWins");
            this.serverWins = serverWins;
            this.receiverWins = receiverWins;
        }

        @Override
        public GameState serverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return this.serverWins;
        }
        @Override
        public GameState receiverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return this.receiverWins;
        }

        @Override
        public String getScore(GameState.GameDisplayContext context) {
            return getScore();
        }

        private String getScore() {
            return translate(this.serverScore) + "-" + translate(this.receiverScore);
        }

        @Override
        public String toString() {
            return this.getScore();
        }

        @Override
        public FlipableGameState flipPlayerScores() {
            // 3-0 (serverWins, (3,1)) -> 0-3((1-3), receiverWins)
            // REVIEW, recursive, ends up creating equal copies
            return new GenericGameState(this.receiverScore, this.serverScore, this.receiverWins.flipPlayerScores(), this.serverWins.flipPlayerScores());
        }

    }

    /**
     * Represents a state when both players have equal points but we haven't reached deuce yet.
     */
    private static final class All implements FlipableGameState {

        private final GameState serverWins;

        private final GameState receiverWins;

        private int score;

        All(int score, GameState serverWins, GameState receiverWins) {
            if (score > 2) {
                throw new IllegalArgumentException("deuce should be used");
            }
            if (score < 0) {
                throw new IllegalArgumentException("negative score not allowed");
            }
            Objects.requireNonNull(serverWins, "serverWins");
            Objects.requireNonNull(receiverWins, "receiverWins");
            this.serverWins = serverWins;
            this.receiverWins = receiverWins;
            this.score = score;
        }

        @Override
        public GameState serverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return this.serverWins;
        }

        @Override
        public GameState receiverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return this.receiverWins;
        }

        @Override
        public String getScore(GameState.GameDisplayContext context) {
            return getScore();
        }

        private String getScore() {
            return translate(this.score) + "-All";
        }

        @Override
        public String toString() {
            return this.getScore();
        }

        @Override
        public FlipableGameState flipPlayerScores() {
            return this;
        }

    }

    private static final class Deuce implements FlipableGameState {

        @Override
        public GameState serverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return ADVANTAGE_SERVER;
        }

        @Override
        public GameState receiverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return ADVANTAGE_RECIVER;
        }

        @Override
        public String getScore(GameState.GameDisplayContext context) {
            return this.getScore();
        }

        private String getScore() {
            return "Deuce";
        }

        @Override
        public FlipableGameState flipPlayerScores() {
            return this;
        }

        @Override
        public String toString() {
            return this.getScore();
        }

    }

    private static abstract class Advantage implements FlipableGameState {

        @Override
        public String getScore(GameState.GameDisplayContext context) {
            return "Advantage " + getPlayer(context);
        }

        protected abstract String getPlayer(GameState.GameDisplayContext context);

    }

    private static final class AdvantageServer extends Advantage {

        @Override
        public GameState serverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return WIN_SERVER;
        }

        @Override
        public GameState receiverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return DEUCE;
        }

        @Override
        protected String getPlayer(GameState.GameDisplayContext context) {
            return context.getServer();
        }

        @Override
        public FlipableGameState flipPlayerScores() {
            return ADVANTAGE_RECIVER;
        }

        @Override
        public String toString() {
            return "Advantage server";
        }

    }

    private static final class AdvantageReceiver extends Advantage {

        @Override
        public GameState serverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return DEUCE;
        }

        @Override
        public GameState receiverWonPoint(GameState.GameDisplayContext context) {
            Objects.requireNonNull(context);
            return WIN_RECIEVER;
        }

        @Override
        protected String getPlayer(GameState.GameDisplayContext context) {
            return context.getReceiver();
        }

        @Override
        public FlipableGameState flipPlayerScores() {
            return ADVANTAGE_SERVER;
        }

        @Override
        public String toString() {
            return "Advantage receiver";
        }

    }

    private static abstract class WonGame implements FlipableGameState {

        @Override
        public String getScore(GameState.GameDisplayContext context) {
            return "Win for " + getPlayer(context);
        }

        @Override
        public GameState serverWonPoint(GameDisplayContext context) {
            throw alreadyWon(context);
        }

        private IllegalStateException alreadyWon(GameDisplayContext context) {
            return new IllegalStateException("game is already won by " + getPlayer(context));
        }

        @Override
        public GameState receiverWonPoint(GameDisplayContext context) {
            throw alreadyWon(context);
        }

        protected abstract String getPlayer(GameState.GameDisplayContext context);

    }

    private static final class WinServer extends WonGame {

        @Override
        protected String getPlayer(GameState.GameDisplayContext context) {
            return context.getServer();
        }

        @Override
        public FlipableGameState flipPlayerScores() {
            return WIN_RECIEVER;
        }

        @Override
        public String toString() {
            return "Win for server";
        }

    }

    private static final class WinReceiver extends WonGame {

        @Override
        protected String getPlayer(GameState.GameDisplayContext context) {
            return context.getReceiver();
        }

        @Override
        public FlipableGameState flipPlayerScores() {
            return WIN_SERVER;
        }

        @Override
        public String toString() {
            return "Win for receiver";
        }

    }

}
