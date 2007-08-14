package demo.chatter;

import java.util.Random;

public class User implements ChatListener {
	private String name;
	private String nodeId;
	private transient ChatterDisplay display;

	public User(String nodeId, ChatterDisplay display) {
		this.name = generateChatname();
		this.nodeId = nodeId;
		this.display = display;
	}

	public String getName() {
		return name;
	}

	public String getNodeId() {
		return nodeId;
	}

	private String generateChatname() {
		Random r = new Random();

		String[] cool = { "Miles", "Ella", "Nina", "Duke", "Charlie", "Billie",
				"Louis", "Fats", "Thelonious", "Dizzy", "Davis", "Fitzgerald",
				"Simone", "Ellington", "Parker", "Holiday", "Armstrong",
				"Waller", "Monk", "Gillespie" };
		return cool[r.nextInt(10)] + cool[r.nextInt(10) + 10];
	}

	public String toString() {
		return name + ", " + nodeId;
	}

	public void newMessage(Message message) {
		display.updateMessage(message.getUser().getName(),
				message.getMessage(), this.equals(message.getUser()));
	}

	public void newUser(String username) {
		display.handleNewUser(username);
	}
}
