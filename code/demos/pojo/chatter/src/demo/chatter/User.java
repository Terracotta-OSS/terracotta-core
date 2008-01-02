/*
 @COPYRIGHT@
 */
package demo.chatter;

import java.util.Random;

public class User {
	private final String name;
	private final String nodeId;

	public User(String nodeId) {
		this.name = generateChatname();
		this.nodeId = nodeId;
	}

	public String getName() {
		return name;
	}

	public String getNodeId() {
		return nodeId;
	}

	private static String generateChatname() {
		return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]
				+ LAST_NAMES[random.nextInt(LAST_NAMES.length)];
	}

	public String toString() {
		return name + ", " + nodeId;
	}

	private static final Random random = new Random();

	private static final String[] FIRST_NAMES = {"Miles", "Ella", "Nina",
			"Duke", "Charlie", "Billie", "Louis", "Fats", "Thelonious", "Dizzy"};

	private static final String[] LAST_NAMES = {"Davis", "Fitzgerald",
			"Simone", "Ellington", "Parker", "Holiday", "Armstrong", "Waller",
			"Monk", "Gillespie"};

}
