/*
 * @COPYRIGHT@
 */
package demo.chatter;

class Message {
  private final String  text;
  private final User    user;
  private final boolean alreadyDisplayedLocally;

  public Message(final User user, final String text, final boolean alreadyDisplayed) {
    this.user = user;
    this.text = text;
    this.alreadyDisplayedLocally = alreadyDisplayed;
  }

  public String getText() {
    return text;
  }

  public User getUser() {
    return user;
  }

  public boolean wasAlreadyDisplayedLocally() {
    return alreadyDisplayedLocally;
  }
}
