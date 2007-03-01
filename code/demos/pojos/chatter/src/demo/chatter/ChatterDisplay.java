package demo.chatter;

public interface ChatterDisplay {
	public void updateMessage(String username, String message);
	public void handleNewUser(String username);
}
