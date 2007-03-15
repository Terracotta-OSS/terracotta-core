/*
 @COPYRIGHT@
 */
package demo.chatter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Random;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
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
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

public class Main extends JFrame implements ActionListener, ChatterDisplay, WindowListener {
	private static final String CHATTER_SYSTEM = "SYSTEM";

	private final ChatManager chatManager = new ChatManager();

	private final JTextPane display = new JTextPane();

	private User user;

	private final DefaultListModel listModel = new DefaultListModel();

	private final JList buddyList = new JList(listModel);

	private boolean isServerDown = false;

	public Main() {
		try {
			final String nodeId = registerForNotifications();
			user = new User(nodeId, this);
			populateCurrentUsers();
			login();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		addWindowListener(this);
		setDefaultLookAndFeelDecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final Container content = getContentPane();

		display.setFont(new Font("Andale Mono", Font.PLAIN, 9));
		display.setEditable(false);
		display.setRequestFocusEnabled(false);

		final JTextField input = new JTextField();
		input.setFont(new Font("Andale Mono", Font.PLAIN, 9));
		input.addActionListener(this);
		final JScrollPane scroll = new JScrollPane(display);
		final Random r = new Random();
		final JLabel avatar = new JLabel(user.getName() + " (node id: "
				+ user.getNodeId() + ")", new ImageIcon(getClass().getResource(
				"/images/buddy" + r.nextInt(10) + ".gif")), JLabel.LEFT);
		avatar.setForeground(Color.WHITE);
		avatar.setFont(new Font("Georgia", Font.PLAIN, 16));
		avatar.setVerticalTextPosition(JLabel.CENTER);
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
		content.add(new JScrollPane(buddyListPanel), BorderLayout.EAST);
		pack();

		setTitle("Chatter: " + user.getName());
		setSize(new Dimension(600, 400));
		setVisible(true);
		input.requestFocus();
	}

	private void populateCurrentUsers() {
		final Object[] currentUsers = chatManager.getCurrentUsers();
		for (int i = 0; i < currentUsers.length; i++) {
			listModel
					.addElement(new String(((User) currentUsers[i]).getName()));
		}
	}

	public void actionPerformed(final ActionEvent e) {
		final JTextField input = (JTextField) e.getSource();
		final String message = input.getText();
		input.setText("");
		(new Thread() {
			public void run() {
				chatManager.send(user, message);
			}
		}).start();

		if (isServerDown) {
			updateMessage(user.getName(), message, true);
		}
	}

	void login() {
		// ****************************************************
		// Uncomment this section if you want incoming clients
		// to see the history of messages.
		// ****************************************************
		// --- CODE BEGINS HERE ---
		//final Message[] messages = chatManager.getMessages();
		//for (int i = 0; i < messages.length; i++) {
		//	user.newMessage(messages[i]);
		//}
		// --- CODE ENDS HERE ---
		synchronized (chatManager) {
			chatManager.registerUser(user);
		}
	}

	public void handleConnectedServer() {
		isServerDown = false;
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Main.this.buddyList.setVisible(true);
				Main.this.buddyList.setEnabled(true);
			}
		});
	}

	public void handleDisconnectedServer() {
		isServerDown = true;
		updateMessage("The server is down; all of your messages will be queued until the server goes back up again.");
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Main.this.buddyList.setVisible(false);
				Main.this.buddyList.setEnabled(false);
			}
		});
	}

	public void handleDisconnectedUser(final String nodeId) {
		final String username = chatManager.removeUser(nodeId);
		listModel.removeElement(username);
	}

	public void handleNewUser(final String username) {
		listModel.addElement(username);
	}

	private void updateMessage(final String message) {
		updateMessage(CHATTER_SYSTEM, message, true);
	}

	public void updateMessage(final String username, final String message, final boolean isOwnMessage) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					final Document doc = display.getDocument();
					final Style style = display.addStyle("Style", null);

					if (isOwnMessage) {
						StyleConstants.setItalic(style, true);
						StyleConstants.setForeground(style, Color.LIGHT_GRAY);
						StyleConstants.setFontSize(style, 9);
					}

					if (username.equals(CHATTER_SYSTEM)) {
						StyleConstants.setItalic(style, true);
						StyleConstants.setForeground(style, Color.RED);
					} else {
						StyleConstants.setBold(style, true);
						doc.insertString(doc.getLength(), username + ": ",
								style);
					}

					StyleConstants.setBold(style, false);
					doc.insertString(doc.getLength(), message, style);
					doc.insertString(doc.getLength(), "\n", style);

					display.setCaretPosition(doc.getLength());
				} catch (final javax.swing.text.BadLocationException ble) {
					System.err.println(ble.getMessage());
				}
			}
		});
	}

	/**
	 * Registers this client for JMX notifications.
	 *
	 * @returns This clients Node ID
	 */
	private String registerForNotifications() throws Exception {
		final java.util.List servers = MBeanServerFactory.findMBeanServer(null);
		if (servers.size() == 0) {
		   System.err.println("WARNING: No JMX servers found, unable to register for notifications.");
		   return "0";
		}
		   
		final MBeanServer server = (MBeanServer)servers.get(0);
		final ObjectName clusterBean = new ObjectName(
				"org.terracotta:type=Terracotta Cluster,name=Terracotta Cluster Bean");
		final ObjectName delegateName = ObjectName
				.getInstance("JMImplementation:type=MBeanServerDelegate");
		final java.util.List clusterBeanBag = new java.util.ArrayList();

		// listener for newly registered MBeans
		final NotificationListener listener0 = new NotificationListener() {
			public void handleNotification(Notification notification,
					Object handback) {
				synchronized (clusterBeanBag) {
					clusterBeanBag.add(handback);
					clusterBeanBag.notifyAll();
				}
			}
		};

		// filter to let only clusterBean passed through
		final NotificationFilter filter0 = new NotificationFilter() {
			public boolean isNotificationEnabled(Notification notification) {
				if (notification.getType().equals("JMX.mbean.registered")
						&& ((MBeanServerNotification) notification)
								.getMBeanName().equals(clusterBean))
					return true;
				return false;
			}
		};

		// add our listener for clusterBean's registration
		server.addNotificationListener(delegateName, listener0, filter0,
				clusterBean);

		// because of race condition, clusterBean might already have registered
		// before we registered the listener
		final java.util.Set allObjectNames = server.queryNames(null, null);

		if (!allObjectNames.contains(clusterBean)) {
			synchronized (clusterBeanBag) {
				while (clusterBeanBag.isEmpty()) {
					clusterBeanBag.wait();
				}
			}
		}

		// clusterBean is now registered, no need to listen for it
		server.removeNotificationListener(delegateName, listener0);

		// listener for clustered bean events
		final NotificationListener listener1 = new NotificationListener() {
			public void handleNotification(Notification notification,
					Object handback) {
				String nodeId = notification.getMessage();
				if (notification.getType().endsWith("thisNodeConnected")) {
					handleConnectedServer();
				}
				if (notification.getType().endsWith("thisNodeDisconnected")) {
					handleDisconnectedServer();
				} else if (notification.getType().endsWith("nodeDisconnected")) {
					handleDisconnectedUser(nodeId);
				}
			}
		};

		// now that we have the clusterBean, add listener for membership events
		server.addNotificationListener(clusterBean, listener1, null,
				clusterBean);
		return (server.getAttribute(clusterBean, "NodeId")).toString();
	}

	public static void main(final String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Main();
			}
		});
	}

	public void windowClosing(WindowEvent e) {
		handleDisconnectedUser(user.getNodeId());
	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
	}
}
