/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.Label;
import org.dijon.TextField;

import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.remote.JMXConnector;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public final class ConnectDialog extends Dialog {
  private static final long          DEFAULT_CONNECT_TIMEOUT_MILLIS = 8000;
  public static final long           CONNECT_TIMEOUT_MILLIS         = Long.getLong("com.tc.admin.connect-timeout",
                                                                                   DEFAULT_CONNECT_TIMEOUT_MILLIS)
                                                                        .longValue();

  private AdminClientContext         m_acc;
  private IServer                    m_server;
  private long                       m_timeout;
  private ConnectionListener         m_listener;
  private AuthenticatingJMXConnector m_jmxc;
  private Future                     m_connectInitiator;
  private Timer                      m_hideTimer;
  private boolean                    m_isAuthenticating;
  private Exception                  m_error;
  private Label                      m_label;
  private Button                     m_cancelButton;
  private final JTextField           m_usernameField;
  private final JPasswordField       m_passwordField;
  private final Button               m_okButton;
  private final Button               m_authCancelButton;
  private final Container            m_emptyPanel;
  private final Container            m_authPanel;

  public ConnectDialog(Frame parent, IServer server, ConnectionListener listener) {
    super(parent, true);

    m_acc = AdminClient.getContext();
    m_server = server;
    m_jmxc = new AuthenticatingJMXConnector(m_server);
    m_timeout = CONNECT_TIMEOUT_MILLIS;
    m_listener = listener;

    load((DialogResource) m_acc.childResource("ConnectDialog"));
    ((JComponent) getContentPane()).setBorder(UIManager.getBorder("InternalFrame.border"));
    m_label = (Label) findComponent("ConnectLabel");
    m_label.setText("Connecting to " + server + ". Please wait...");
    pack();

    m_cancelButton = (Button) findComponent("CancelButton");
    m_cancelButton.addActionListener(new CancelButtonHandler());
    getContentPane().addHierarchyListener(new ShowingChangeListener());

    m_emptyPanel = (Container) findComponent("EmptyPanel");
    m_emptyPanel.setLayout(new BorderLayout());

    m_authPanel = (Container) AdminClient.getContext().resolveResource("AuthPanel");

    Container credentialsPanel = (Container) m_authPanel.findComponent("CredentialsPanel");
    m_authPanel.setVisible(false);
    this.m_usernameField = (JTextField) credentialsPanel.findComponent("UsernameField");
    this.m_okButton = (Button) m_authPanel.findComponent("OKButton");
    this.m_authCancelButton = (Button) m_authPanel.findComponent("CancelButton");

    // must be found last because JPasswordField is not a Dijon Component
    TextField passwordField = (TextField) credentialsPanel.findComponent("PasswordField");
    Container passwdHolder = new Container();
    passwdHolder.setLayout(new BorderLayout());
    passwdHolder.add(m_passwordField = new JPasswordField());
    credentialsPanel.replaceChild(passwordField, passwdHolder);

    m_okButton.addActionListener(new OKButtonHandler());
    m_authCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_cancelButton.doClick();
      }
    });

    m_hideTimer = new Timer(100, new DialogCloserTask());
    m_hideTimer.setRepeats(false);
  }

  class CancelButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      m_cancelButton.setEnabled(false);
      m_connectInitiator.cancel(true);
      m_error = new RuntimeException("Canceled");
      ConnectDialog.this.setVisible(false);
    }
  }

  class OKButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String username = m_usernameField.getText().trim();
      String password = new String(m_passwordField.getPassword()).trim();
      m_server.setConnectionCredentials(new String[] { username, password });
      disableAuthenticationDialog();
      initiateConnectAction();
    }
  }

  private void initiateConnectAction() {
    m_cancelButton.setEnabled(true);
    m_connectInitiator = m_acc.submit(new ConnectInitiator());
  }

  class DialogCloserTask implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      disableAuthenticationDialog();
      setVisible(false);
    }
  }

  private void disableAuthenticationDialog() {
    m_usernameField.setEnabled(false);
    m_passwordField.setEnabled(false);
    m_emptyPanel.removeAll();
    m_usernameField.setText("");
    m_passwordField.setText("");
    m_authPanel.setVisible(false);
    m_authCancelButton.setVisible(false);
    m_cancelButton.setVisible(true);
    pack();
    center(getOwner());
  }

  private void enableAuthenticationDialog() {
    m_emptyPanel.add(m_authPanel);
    m_cancelButton.setVisible(false);
    m_authCancelButton.setVisible(true);
    m_usernameField.setEnabled(true);
    m_passwordField.setEnabled(true);
    m_authPanel.setVisible(true);
    m_authPanel.getRootPane().setDefaultButton(m_okButton);
    pack();
    center(getOwner());
    m_usernameField.grabFocus();
  }

  public void setServer(IServer server) {
    m_server = server;
    m_jmxc = new AuthenticatingJMXConnector(m_server);
    m_label.setText("Connecting to " + server + ". Please wait...");
    pack();
  }

  public IServer getServer() {
    return m_server;
  }

  public void setTimeout(long millis) {
    m_timeout = millis;
  }

  public long getTimeout() {
    return m_timeout;
  }

  public void setConnectionListener(ConnectionListener listener) {
    m_listener = listener;
  }

  public ConnectionListener getConnectionListener() {
    return m_listener;
  }

  public JMXConnector getConnector() {
    return m_jmxc;
  }

  public Exception getError() {
    return m_error;
  }

  private class ShowingChangeListener implements HierarchyListener {
    public void hierarchyChanged(HierarchyEvent e) {
      long flags = e.getChangeFlags();

      if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
        if (isShowing()) {
          initiateConnectAction();
        } else {
          fireHandleConnect();
        }
      }
    }
  }

  protected void fireHandleConnect() {
    if (m_listener != null) {
      try {
        if (m_error == null) {
          m_listener.handleConnection();
        } else {
          m_listener.handleException();
        }
      } catch (RuntimeException rte) {
        rte.printStackTrace();
      }
    }
    m_isAuthenticating = false;
  }

  private class ConnectInitiator implements Runnable {
    public void run() {
      Future f = m_acc.submitTask(new ConnectAction());

      m_error = null;
      try {
        f.get(m_timeout, TimeUnit.MILLISECONDS);
        m_hideTimer.start();
        return;
      } catch (TimeoutException te) {
        m_hideTimer.start();
        return;
      } catch (Exception e) {
        Throwable cause = e.getCause();
        if (!m_isAuthenticating && cause instanceof SecurityException) {
          m_isAuthenticating = true;
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              enableAuthenticationDialog();
            }
          });
        } else {
          m_error = e;
          m_hideTimer.start();
        }
      }
    }
  }

  class ConnectAction implements Callable<Void> {
    public Void call() throws Exception {
      m_jmxc.connect();
      return null;
    }
  }

  void tearDown() {
    Map env = m_server.getConnectionEnvironment();
    if (env != null) {
      env.clear();
    }

    m_acc = null;
    m_server = null;
    m_listener = null;
    m_jmxc = null;
    m_connectInitiator = null;
    m_cancelButton = null;
    m_hideTimer = null;
    m_error = null;
  }
}
