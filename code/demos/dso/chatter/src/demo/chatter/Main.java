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

public class Main extends JFrame implements ActionListener, ChatterDisplay {
	/** roots * */
	private ChatManager			chatManager	= new ChatManager();

	private JTextPane			display		= new JTextPane();
	private User				user;
	private DefaultListModel	listModel	= new DefaultListModel();

	public Main() {
		try {
			String nodeId = registerForNotifications();
			user = new User(nodeId, this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		setDefaultLookAndFeelDecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container content = getContentPane();

		display.setFont(new Font("Lucida Sans Typewriter", Font.PLAIN, 9));
		display.setEditable(false);
		display.setRequestFocusEnabled(false);

		final JTextField input = new JTextField();
		input.setFont(new Font("Lucida Sans Typewriter", Font.PLAIN, 9));
		input.addActionListener(this);
		JScrollPane scroll = new JScrollPane(display);
		Random r = new Random();
		JLabel buddy = new JLabel(user.getName(),
					  new ImageIcon(getClass().getResource("/images/buddy" + r.nextInt(10) + ".gif")),
					  JLabel.LEFT);
		buddy.setFont(new Font("Lucida Sans Typewriter", Font.PLAIN, 16));
		buddy.setVerticalTextPosition(JLabel.CENTER);
		JPanel buddypanel = new JPanel();
		buddypanel.setBackground(Color.WHITE);
		buddypanel.setLayout(new BorderLayout());
		buddypanel.add(buddy, BorderLayout.CENTER);

		JPanel buddyListPanel = new JPanel();
		JList buddyList = new JList(listModel);
		buddyListPanel.add(buddyList);

		content.setLayout(new BorderLayout());
		content.add(buddypanel, BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);
		content.add(input, BorderLayout.SOUTH);
		content.add(new JScrollPane(buddyListPanel), BorderLayout.EAST);
		pack();

		setTitle("Chatter: " + user.getName());
		setSize(new Dimension(300, 400));
		setVisible(true);

		input.requestFocus();

		populateCurrentUsers();
		login();
	}

	private void populateCurrentUsers() {
		Object[] currentUsers = chatManager.getCurrentUsers();
		for (int i = 0; i < currentUsers.length; i++) {
			listModel.addElement(new String(((User)currentUsers[i]).getName()));
		}
	}

	public void actionPerformed(ActionEvent e) {
		JTextField input = (JTextField) e.getSource();
		final String message = input.getText();
		input.setText("");
		Thread sender = new Thread(new Runnable() {
			public void run() {
				chatManager.send(user, message);
			}
		});
		sender.start();
	}

	void login() {
		Message[] messages = chatManager.getMessages();
		for (int i = 0; i < messages.length; i++) {
			user.newMessage(messages[i]);
		}

		synchronized (chatManager) {
			echo("registering: " + user + ", nodeId: " + user.getNodeId());
			chatManager.registerUser(user);
		}

	}

	public static void echo(String msg) {
		System.err.println(msg);
	}

	public static void main(String[] args) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Main();
			}
		});
	}

	/**
	 * Registers this client for JMX notifications.
	 *
	 * @returns This clients Node ID
	 */
	private String registerForNotifications() throws Exception {
		java.util.List servers = MBeanServerFactory.findMBeanServer(null);
		MBeanServer server = (MBeanServer) servers.get(0);
		final ObjectName clusterBean = new ObjectName(
				"com.terracottatech:type=Terracotta Cluster,name=Terracotta Cluster Bean");
		ObjectName delegateName = ObjectName
				.getInstance("JMImplementation:type=MBeanServerDelegate");
		final java.util.List clusterBeanBag = new java.util.ArrayList();

		// listener for newly registered MBeans
		NotificationListener listener0 = new NotificationListener() {
			public void handleNotification(Notification notification,
					Object handback) {
				synchronized (clusterBeanBag) {
					clusterBeanBag.add(handback);
					clusterBeanBag.notifyAll();
				}
			}
		};

		// filter to let only clusterBean passed through
		NotificationFilter filter0 = new NotificationFilter() {
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
		java.util.Set allObjectNames = server.queryNames(null, null);

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
		NotificationListener listener1 = new NotificationListener() {
			public void handleNotification(Notification notification,
					Object handback) {
				String nodeId = notification.getMessage();
				echo("received msg: " + notification);
				if (notification.getType().endsWith("nodeDisconnected")) {
					handleDisconnectedUser(nodeId);
				}
			}
		};

		// now that we have the clusterBean, add listener for membership events
		server.addNotificationListener(clusterBean, listener1, null,
				clusterBean);
		return (server.getAttribute(clusterBean, "NodeId")).toString();
	}

	public void handleDisconnectedUser(String nodeId) {
		String username = chatManager.removeUser(nodeId);
		listModel.removeElement(username);
	}

	public void handleNewUser(String username) {
		echo("Adding user: " + username);
		listModel.addElement(username);
	}

	public void updateMessage(final String username, final String message) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					Document doc = display.getDocument();
					Style style = display.addStyle("Style", null);

					if (user.getName().equals(username)) {
						StyleConstants.setItalic(style, true);
						StyleConstants.setForeground(style, Color.LIGHT_GRAY);
						StyleConstants.setFontSize(style, 9);
					} else {
						if (user.equals(username)) {
							StyleConstants.setItalic(style, true);
							StyleConstants.setForeground(style, Color.GRAY);
						}
						StyleConstants.setBold(style, true);
						doc.insertString(doc.getLength(), username + ": ",
								style);
					}

					StyleConstants.setBold(style, false);
					doc.insertString(doc.getLength(), message, style);
					doc.insertString(doc.getLength(), "\n", style);

					display.setCaretPosition(doc.getLength());
				} catch (javax.swing.text.BadLocationException ble) {
					System.err.println(ble.getMessage());
				}
			}
		});

	}

}
