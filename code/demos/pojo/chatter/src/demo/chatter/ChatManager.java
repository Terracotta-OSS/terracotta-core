/*
 * @COPYRIGHT@
 */
package demo.chatter;

import com.tc.cluster.DsoClusterTopology;
import com.tcclient.cluster.DsoNode;

import java.util.Map;
import java.util.TreeMap;

/**
 * Description of the Class
 *
 * @author Terracotta, Inc.
 */
class ChatManager {

  private final Map<DsoNode, User>        users;
  private volatile transient ChatListener listener;

  public ChatManager() {
    this.users = new TreeMap<DsoNode, User>();
    init();
  }

  private void init() {
    this.listener = new NullChatListener();
  }

  public User[] getCurrentUsers() {
    synchronized (users) {
      return users.values().toArray(new User[] {});
    }
  }

  public void send(final Message msg) {
    sendNewMessageEvent(msg);
  }

  public void registerUser(final User user) {
    synchronized (users) {
      users.put(user.getNode(), user);
    }
    sendNewUserEvent(user.getName());
  }

  public void removeUser(final DsoNode node) {
    synchronized (users) {
      users.remove(node);
    }
  }

  /**
   * Normally the user list is maintained via cluster events received in each node. This method will ensure that the
   * list is consistent even if all clients crash simultaneously
   */
  public void retainNodes(final DsoClusterTopology topology) {
    synchronized (users) {
      users.keySet().retainAll(topology.getNodes());
    }
  }

  private void sendNewUserEvent(final String username) {
    this.listener.newUser(username);
  }

  private void sendNewMessageEvent(final Message message) {
    this.listener.newMessage(message);
  }

  public void setLocalListener(final ChatListener listener) {
    this.listener = listener;
  }

  private static class NullChatListener implements ChatListener {
    public void newMessage(final Message message) {
      //
    }

    public void newUser(final String username) {
      //
    }
  }

}
