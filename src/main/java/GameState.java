/**
 * Represents the state of a game.
 * <p>
 * Offers methods for transitioning to a next state ({@link #serverWonPoint(GameContext)}
 * and {@link #receiverWonPoint(GameContext)}) as well as querying the current state
 * ({@link #getScore(GameContext)}).
 * <p>
 * Instances are stateless due to {@link GameContext} and therefore thread safe.
 * <p>
 * Instances may implement {@link #toString()} for debug purposes.
 */
interface GameState {

    /**
     * Returns the next state when the server (player1) won a point.
     * 
     * @param context the context, used for generating exception messages,
     *                not {@code null}
     * @return the next valid game state
     * @throws IllegalStateException if the game is already won by a player
     */
    GameState serverWonPoint(GameDisplayContext context);

    /**
     * Returns the next state when the receiver (player2) won a point.
     * 
     * @param context the context, used for generating exception messages,
     *                not {@code null}
     * @return the next valid game state
     * @throws IllegalStateException if the game is already won by a player
     */
    GameState receiverWonPoint(GameDisplayContext context);
    String getScore(GameDisplayContext context);

    /**
     * Accessor for game-specific display-only information to keep the instances
     * of {@link GameState} stateless so they can be shared.
     */
    interface GameDisplayContext {

        /**
         * Returns the name of the server (player1).
         * 
         * @return the name of the server, not {@code null}
         */
        String getServer();

        /**
         * Returns the name of the receiver (player2).
         * 
         * @return the name of the receiver, not {@code null}
         */
        String getReceiver();

    }

}
