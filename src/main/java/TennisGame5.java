import java.util.Map;

public class TennisGame5 implements TennisGame {

  private static final String WIN_PLAYER_1 = "Win for player1";
  private static final String WIN_PLAYER_2 = "Win for player2";
  
  // @formatter:off
  private static final String[][] SCORE_TABLE = {
      {"Love-All",     "Love-Fifteen",    "Love-Thirty",    "Love-Forty",        WIN_PLAYER_2,        null},
      {"Fifteen-Love", "Fifteen-All",     "Fifteen-Thirty", "Fifteen-Forty",     WIN_PLAYER_2,        null},
      {"Thirty-Love",  "Thirty-Fifteen",  "Thirty-All",     "Thirty-Forty",      WIN_PLAYER_2,        null},
      {"Forty-Love",   "Forty-Fifteen",   "Forty-Thirty",   "Deuce",             "Advantage player2", WIN_PLAYER_2},
      {WIN_PLAYER_1,   WIN_PLAYER_1,      WIN_PLAYER_1,     "Advantage player1", null},
      {null,           null,              null,              WIN_PLAYER_1,       null},
  };
  // @formatter:on

  private final Map<String, Integer> playerIndices;
  private final int[] points = {0, 0};


  public TennisGame5(String player1, String player2) {
    this.playerIndices = Map.of(player1, 0, player2, 1);
  }

  @Override
  public void wonPoint(String playerName) {
    if (!this.playerIndices.containsKey(playerName)) {
      throw new IllegalArgumentException("Player '" + playerName + "' does not exist");
    }

    // Increase the points
    this.points[this.playerIndices.get(playerName)]++;

    // We got another "Deuce". Reset the point to the original position
    if (this.points[0] == 4 && this.points[1] == 4) {
      this.points[0] = 3;
      this.points[1] = 3;
    }

    // Handle Illegal state: Reset points to original state and throw exception
    if (SCORE_TABLE[this.points[0]][this.points[1]] == null) {
      this.points[this.playerIndices.get(playerName)]--;
      throw new IllegalStateException("The game has already been won");
    }
  }

  @Override
  public String getScore() {
    return SCORE_TABLE[this.points[0]][this.points[1]];
  }

}
