/*
 * @COPYRIGHT@
 */
package demo.chatter;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
public class Main extends JFrame implements ActionListener, ChatListener, DsoClusterListener {

  private DsoCluster       cluster;

  private final User       localUser;
  private ChatManager      chatManager;
  private MessageQueue     messageQueue;

  private final JTextPane  display   = new JTextPane();
  private final JList      buddyList = new JList();
  private final JTextField input     = new JTextField();

  private final Style      systemStyle;
  private final Style      localUserStyle;
  private final Style      remoteUserStyle;

  private final Object     lock      = new Object();

  public Main() throws Exception {
    this.chatManager = new ChatManager();
    this.localUser = new User(cluster.getCurrentNode());

    this.systemStyle = display.addStyle("systemStyle", null);
    this.localUserStyle = display.addStyle("localUserStyle", null);
    this.remoteUserStyle = display.addStyle("remoteUserStyle", null);
  }

  private void init() throws Exception {
    setupUI();

    synchronized (lock) {

      messageQueue = new MessageQueue(chatManager);
      messageQueue.start();

      chatManager.registerUser(localUser);
      chatManager.setLocalListener(this);

      cluster.addClusterListener(this);
      populateCurrentUsers();
    }
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
    final JLabel avatar = new JLabel(localUser.getName() + " (node id: " + localUser.getNode() + ")",
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

  public void nodeJoined(final DsoClusterEvent event) {
    // unused
  }

  public void nodeLeft(final DsoClusterEvent event) {
    synchronized (lock) {
      chatManager.removeUser(event.getNode());
      populateCurrentUsers();
    }
  }

  public void operationsEnabled(final DsoClusterEvent event) {
    chatManager.retainNodes(cluster.getClusterTopology());

    synchronized (lock) {
      systemMessage("The server is up.");

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          toggleList(true);
        }
      });
    }
  }

  public void operationsDisabled(final DsoClusterEvent event) {
    synchronized (lock) {
      systemMessage("The server is down; all of your messages will be queued until the server comes back up again.");
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          toggleList(false);
        }
      });
    }
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
      Message msg = new Message(localUser, message, !cluster.areOperationsEnabled());

      messageQueue.enqueue(msg);

      if (!cluster.areOperationsEnabled()) {
        displayMessage(message, localUserStyle);
      }
    }
  }

  private void toggleList(final boolean on) {
    this.buddyList.setVisible(on);
    this.buddyList.setEnabled(on);
  }

  private void handleNewUser(final String username) {
    synchronized (lock) {
      populateCurrentUsers();
    }
  }

  public void newMessage(final Message message) {
    User source = message.getUser();
    boolean local = source == localUser;

    if (local && message.wasAlreadyDisplayedLocally()) { return; }

    String displayMessage = (local ? "" : source.getName() + ": ") + message.getText();
    displayMessage(displayMessage, local ? localUserStyle : remoteUserStyle);
  }

  public void newUser(final String username) {
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
    for (User currentUser : currentUsers) {
      list.addElement(currentUser.getName());
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

  private void systemMessage(final String message) {
    displayMessage(message, systemStyle);
  }

  private static void exit(final Throwable t) {
    t.printStackTrace();
    System.exit(1);
  }

  public static void main(final String[] args) throws Exception {
    final Main main = new Main();
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

    MessageQueue(final ChatManager chatManager) {
      this.chatManager = chatManager;
      setDaemon(true);
      setName("Offline Message Queue");
    }

    void enqueue(final Message msg) {
      try {
        msgQueue.put(msg);
      } catch (InterruptedException e) {
        exit(e);
      }
    }

    @Override
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
