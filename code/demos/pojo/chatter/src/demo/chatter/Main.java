/*
 * @COPYRIGHT@
 */
package demo.chatter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

/**
 * Description of the Class
 *
 * @author Terracotta, Inc.
 */
public class Main extends JFrame implements ActionListener, ChatListener, NodeListProvider {

  private final User        localUser;
  private ChatManager       chatManager;
  private MessageQueue      messageQueue;
  private boolean           isServerDown = false;

  private final JTextPane   display      = new JTextPane();
  private final JList       buddyList    = new JList();
  private final JTextField  input        = new JTextField();

  private final Style       systemStyle;
  private final Style       localUserStyle;
  private final Style       remoteUserStyle;

  private final Object      lock         = new Object();
  private final MBeanServer mbeanServer;
  private final ObjectName  clusterBean;

  public Main(String nodeId, MBeanServer mbeanServer, ObjectName clusterBean) throws Exception {
    this.mbeanServer = mbeanServer;
    this.clusterBean = clusterBean;
    this.localUser = new User(nodeId);

    this.systemStyle = display.addStyle("systemStyle", null);
    this.localUserStyle = display.addStyle("localUserStyle", null);
    this.remoteUserStyle = display.addStyle("remoteUserStyle", null);
  }

  private void init() throws Exception {
    setupUI();

    synchronized (lock) {
      chatManager = new ChatManager();
      messageQueue = new MessageQueue(chatManager);
      messageQueue.start();

      chatManager.registerUser(localUser);
      chatManager.setLocalListener(this);

      registerJMXNotifications();
      retainNodes();
      populateCurrentUsers();
    }
  }

  public Collection<String> getNodeList() {
    Collection<String> allNodes = new HashSet<String>();

    try {
      String[] allNodeIDs = (String[]) mbeanServer.getAttribute(clusterBean, "NodesInCluster");
      for (int i = 0; i < allNodeIDs.length; i++) {
        allNodes.add(allNodeIDs[i]);
      }
    } catch (Exception e) {
      exit(e);
    }

    return allNodes;
  }

  private void retainNodes() {
    chatManager.retainNodes(this);
  }

  private void setupUI() {
    setDefaultLookAndFeelDecorated(true);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    final Container content = getContentPane();

    display.setFont(new Font("Andale Mono", Font.PLAIN, 9));
    display.setEditable(false);
    display.setRequestFocusEnabled(false);

    StyleConstants.setItalic(localUserStyle, true);
    StyleConstants.setForeground(localUserStyle, Color.LIGHT_GRAY);
    StyleConstants.setFontSize(localUserStyle, 9);

    StyleConstants.setItalic(systemStyle, true);
    StyleConstants.setForeground(systemStyle, Color.RED);

    input.setFont(new Font("Andale Mono", Font.PLAIN, 9));
    input.addActionListener(this);
    final JScrollPane scroll = new JScrollPane(display);
    final Random r = new Random();
    final JLabel avatar = new JLabel(localUser.getName() + " (node id: " + localUser.getNodeId() + ")",
                                     new ImageIcon(getClass().getResource("/images/buddy" + r.nextInt(10) + ".gif")),
                                     SwingConstants.LEFT);
    avatar.setForeground(Color.WHITE);
    avatar.setFont(new Font("Georgia", Font.PLAIN, 16));
    avatar.setVerticalTextPosition(SwingConstants.CENTER);
    final JPanel buddypanel = new JPanel();
    buddypanel.setBackground(Color.DARK_GRAY);
    buddypanel.setLayout(new BorderLayout());
    buddypanel.add(avatar, BorderLayout.CENTER);

    final JPanel buddyListPanel = new JPanel();
    buddyListPanel.setBackground(Color.WHITE);
    buddyListPanel.add(buddyList);
    buddyList.setFont(new Font("Andale Mono", Font.BOLD, 9));

    content.setLayout(new BorderLayout());
    content.add(buddypanel, BorderLayout.NORTH);
    content.add(scroll, BorderLayout.CENTER);
    content.add(input, BorderLayout.SOUTH);
    JScrollPane scrollPane = new JScrollPane(buddyListPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    content.add(scrollPane, BorderLayout.EAST);
    pack();

    setTitle("Chatter: " + localUser.getName());
    setSize(new Dimension(600, 400));
  }

  private void registerJMXNotifications() throws InstanceNotFoundException {
    // listener for clustered bean events
    final NotificationListener clusterEventsListener = new NotificationListener() {
      public void handleNotification(Notification notification, Object handback) {
        String nodeId = notification.getMessage();
        if (notification.getType().endsWith("thisNodeConnected")) {
          handleConnectedServer();
        } else if (notification.getType().endsWith("thisNodeDisconnected")) {
          handleDisconnectedServer();
        } else if (notification.getType().endsWith("nodeDisconnected")) {
          handleDisconnectedUser(nodeId);
        }
      }
    };

    // add listener for membership events
    mbeanServer.addNotificationListener(clusterBean, clusterEventsListener, null, null);
  }

  private void startup() {
    setVisible(true);
    input.requestFocus();
  }

  public void actionPerformed(final ActionEvent e) {
    final JTextField source = (JTextField) e.getSource();
    final String message = source.getText();
    source.setText("");

    synchronized (lock) {
      Message msg = new Message(localUser, message, isServerDown);

      messageQueue.enqueue(msg);

      if (isServerDown) {
        displayMessage(message, localUserStyle);
      }
    }
  }

  private void toggleList(boolean on) {
    this.buddyList.setVisible(on);
    this.buddyList.setEnabled(on);
  }

  private void handleConnectedServer() {
    synchronized (lock) {
      isServerDown = false;
      systemMessage("The server is back up.");

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          retainNodes();
          toggleList(true);
        }
      });
    }

  }

  private void handleDisconnectedServer() {
    synchronized (lock) {
      isServerDown = true;
      systemMessage("The server is down; all of your messages will be queued until the server comes back up again.");
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          toggleList(false);
        }
      });
    }
  }

  private void handleDisconnectedUser(final String nodeId) {
    synchronized (lock) {
      chatManager.removeUser(nodeId);
      populateCurrentUsers();
    }
  }

  private void handleNewUser(final String username) {
    synchronized (lock) {
      populateCurrentUsers();
    }
  }

  public void newMessage(Message message) {
    User source = message.getUser();
    boolean local = source == localUser;

    if (local && message.wasAlreadyDisplayedLocally()) { return; }

    String displayMessage = (local ? "" : source.getName() + ": ") + message.getText();
    displayMessage(displayMessage, local ? localUserStyle : remoteUserStyle);
  }

  public void newUser(String username) {
    handleNewUser(username);
  }

  private void displayMessage(final String message, final Style style) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Document doc = display.getDocument();
        try {
          doc.insertString(doc.getLength(), message + "\n", style);
        } catch (BadLocationException ble) {
          exit(ble);
        }
        display.setCaretPosition(doc.getLength());
      }
    });
  }

  private void populateCurrentUsers() {
    final DefaultListModel list = new DefaultListModel();
    User[] currentUsers = chatManager.getCurrentUsers();
    for (int i = 0; i < currentUsers.length; i++) {
      list.addElement(currentUsers[i].getName());
    }

    Runnable setList = new Runnable() {
      public void run() {
        buddyList.setModel(list);
        buddyList.invalidate();
        buddyList.repaint();
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      setList.run();
    } else {
      SwingUtilities.invokeLater(setList);
    }
  }

  private void systemMessage(String message) {
    displayMessage(message, systemStyle);
  }

  /**
   * Ensures (waiting if needed) that the terracotta cluster bean is available
   *
   * @param mbeanServer
   * @return The ObjectName of the terracotta cluster bean
   */
  private static ObjectName getClusterBean(MBeanServer server) throws Exception {
    final ObjectName clusterBean = new ObjectName("org.terracotta:type=Terracotta Cluster,name=Terracotta Cluster Bean");
    final ObjectName delegateName = ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");
    final List<Object> clusterBeanBag = new ArrayList<Object>();

    // listener for newly registered MBeans
    final NotificationListener clusterBeanListener = new NotificationListener() {
      public void handleNotification(Notification notification, Object handback) {
        synchronized (clusterBeanBag) {
          clusterBeanBag.add(handback);
          clusterBeanBag.notifyAll();
        }
      }
    };

    // filter to let only clusterBean passed through
    final NotificationFilter cluserBeanFilter = new NotificationFilter() {
      public boolean isNotificationEnabled(Notification notification) {
        return (notification.getType().equals("JMX.mbean.registered") && ((MBeanServerNotification) notification)
            .getMBeanName().equals(clusterBean));

      }
    };

    // add our listener for clusterBean's registration
    server.addNotificationListener(delegateName, clusterBeanListener, cluserBeanFilter, clusterBean);

    // because of race condition, clusterBean might already have registered
    // before we registered the listener
    final Set<ObjectName> allObjectNames = server.queryNames(null, null);

    if (!allObjectNames.contains(clusterBean)) {
      synchronized (clusterBeanBag) {
        while (clusterBeanBag.isEmpty()) {
          clusterBeanBag.wait();
        }
      }
    }

    // clusterBean is now registered, no need to listen for it
    server.removeNotificationListener(delegateName, clusterBeanListener);

    return clusterBean;
  }

  private static void exit(Throwable t) {
    t.printStackTrace();
    System.exit(1);
  }

  public static void main(final String[] args) throws Exception {
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName clusterBean = getClusterBean(mbeanServer);
    String nodeId = mbeanServer.getAttribute(clusterBean, "NodeId").toString();

    final Main main = new Main(nodeId, mbeanServer, clusterBean);
    main.init();

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        main.startup();
      }
    });
  }

  private static class MessageQueue extends Thread {
    private final BlockingQueue<Message> msgQueue = new LinkedBlockingQueue<Message>(Integer.MAX_VALUE);
    private final ChatManager            chatManager;

    MessageQueue(ChatManager chatManager) {
      this.chatManager = chatManager;
      setDaemon(true);
      setName("Offline Message Queue");
    }

    void enqueue(Message msg) {
      try {
        msgQueue.put(msg);
      } catch (InterruptedException e) {
        exit(e);
      }
    }

    public void run() {
      while (true) {
        try {
          Message msg = msgQueue.take();
          chatManager.send(msg);
        } catch (InterruptedException e) {
          exit(e);
        }
      }
    }
  }

}
