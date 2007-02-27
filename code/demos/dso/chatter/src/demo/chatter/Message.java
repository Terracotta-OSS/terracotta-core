/*
 @COPYRIGHT@
 */
package demo.chatter;

class Message {
	private String	message;
	private User	user;

	public Message(User user, String message) {
		this.user = user;
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public User getUser() {
		return user;
	}
}
