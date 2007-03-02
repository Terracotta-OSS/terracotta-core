package demo.chatter;

public interface ChatterDisplay {
	public void updateMessage(String username, String message, boolean isOwnMessage);
	public void handleNewUser(String username);
}
