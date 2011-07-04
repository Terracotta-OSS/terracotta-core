/*
 * @COPYRIGHT@
 */
package demo.chatter;

interface ChatListener {
  public void newUser(String username);

  public void newMessage(Message message);
}
