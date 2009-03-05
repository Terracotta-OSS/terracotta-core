/*
 * @COPYRIGHT@
 */
package demo.chatter;

import com.tcclient.cluster.DsoNode;

import java.util.Random;

public class User {
  private final String  name;
  private final DsoNode node;

  public User(final DsoNode node) {
    this.name = generateChatname();
    this.node = node;
  }

  public String getName() {
    return name;
  }

  public DsoNode getNode() {
    return node;
  }

  private static String generateChatname() {
    return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
  }

  @Override
  public String toString() {
    return name + ", " + node;
  }

  private static final Random   random      = new Random();

  private static final String[] FIRST_NAMES = { "Miles", "Ella", "Nina", "Duke", "Charlie", "Billie", "Louis", "Fats",
      "Thelonious", "Dizzy"                };

  private static final String[] LAST_NAMES  = { "Davis", "Fitzgerald", "Simone", "Ellington", "Parker", "Holiday",
      "Armstrong", "Waller", "Monk", "Gillespie" };

}
