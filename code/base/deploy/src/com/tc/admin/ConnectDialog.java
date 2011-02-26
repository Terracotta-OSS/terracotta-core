/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextField;
import com.tc.admin.model.IClusterModel;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.remote.JMXConnector;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public final class ConnectDialog extends JDialog implements HierarchyListener {
  private ApplicationContext         appContext;

  private static final long          DEFAULT_CONNECT_TIMEOUT_MILLIS = 20000;
  public static final long           CONNECT_TIMEOUT_MILLIS         = Long.getLong("com.tc.admin.connect-timeout",
                                                                                   DEFAULT_CONNECT_TIMEOUT_MILLIS)
                                                                        .longValue();
  private static final int           HIDE_TIMER_DELAY_MILLIS        = 650;

  private IClusterModel              clusterModel;
  private long                       timeout;
  private ConnectionListener         listener;
  private AuthenticatingJMXConnector jmxc;
  private Future                     connectInitiator;
  private Timer                      hideTimer;
  private boolean                    isAuthenticating;
  private Exception                  error;
  private XLabel                     label;
  private XButton                    cancelButton;
  private XTextField                 usernameField;
  private JPasswordField             passwordField;
  private XButton                    okButton;
  private XButton                    authCancelButton;
  private XContainer                 authPanel;

  public ConnectDialog(ApplicationContext appContext, Frame parent, IClusterModel clusterModel,
                       ConnectionListener listener) {
    super(parent, true);

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.listener = listener;

    jmxc = new AuthenticatingJMXConnector(clusterModel);
    timeout = CONNECT_TIMEOUT_MILLIS;

    Container cp = getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(8, 8, 8, 8);

    label = new XLabel(appContext.format("connect-dialog.connecting.format", clusterModel));
    cp.add(label, gbc);
    gbc.gridy++;

    cancelButton = new XButton(appContext.getString("cancel"));
    cancelButton.addActionListener(new CancelButtonHandler());
    cp.add(cancelButton, gbc);

    gbc.gridwidth = 2;
    cp.add(authPanel = createAuthPanel(), gbc);
    authPanel.setVisible(false);

    okButton.addActionListener(new OKButtonHandler());
    authCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        cancelButton.doClick();
      }
    });

    hideTimer = new Timer(HIDE_TIMER_DELAY_MILLIS, new DialogCloserTask());
    hideTimer.setRepeats(false);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        cancelButton.doClick();
      }
    });

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setResizable(false);
    setTitle(parent.getTitle());
    pack();
  }

  private XContainer createAuthPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    XContainer buttonPanel = new XContainer(new GridBagLayout());
    buttonPanel.add(authCancelButton = new XButton(appContext.getString("cancel")), gbc);
    gbc.gridx++;
    buttonPanel.add(okButton = new XButton(appContext.getString("ok")), gbc);

    gbc.gridx = gbc.gridy = 0;
    panel.add(createCredentialsPanel(), gbc);
    gbc.gridy++;

    panel.add(buttonPanel, gbc);

    return panel;
  }

  private XContainer createCredentialsPanel() {
    XContainer credentialsPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    credentialsPanel.add(new XLabel(appContext.getString("connect-dialog.username")), gbc);
    gbc.gridx++;

    usernameField = new XTextField();
    usernameField.setColumns(16);
    credentialsPanel.add(usernameField, gbc);
    gbc.gridx--;
    gbc.gridy++;

    credentialsPanel.add(new XLabel(appContext.getString("connect-dialog.password")), gbc);
    gbc.gridx++;

    passwordField = new JPasswordField();
    passwordField.setColumns(16);
    credentialsPanel.add(passwordField, gbc);

    credentialsPanel.setBorder(BorderFactory.createTitledBorder(appContext.getString("connect-dialog.credentials")));

    return credentialsPanel;
  }

  @Override
  public void setVisible(boolean visible) {
    getContentPane().removeHierarchyListener(this);
    if (visible) {
      getContentPane().addHierarchyListener(this);
    }
    super.setVisible(visible);
  }

  class CancelButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      cancelButton.setEnabled(false);
      connectInitiator.cancel(true);
      error = new RuntimeException(appContext.getString("canceled"));
      ConnectDialog.this.setVisible(false);
      fireHandleConnect();
    }
  }

  class OKButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String username = usernameField.getText().trim();
      String password = new String(passwordField.getPassword()).trim();
      clusterModel.setConnectionCredentials(new String[] { username, password });
      disableAuthenticationDialog();
      initiateConnectAction();
    }
  }

  private void initiateConnectAction() {
    cancelButton.setEnabled(true);
    connectInitiator = appContext.submit(new ConnectInitiator());
  }

  class DialogCloserTask implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      disableAuthenticationDialog();
      setVisible(false);
      fireHandleConnect();
    }
  }

  private void disableAuthenticationDialog() {
    usernameField.setEnabled(false);
    passwordField.setEnabled(false);
    usernameField.setText("");
    passwordField.setText("");
    authPanel.setVisible(false);
    authCancelButton.setVisible(false);
    cancelButton.setVisible(true);
    pack();
    center();
  }

  public void center() {
    WindowHelper.center(this, getOwner());
  }

  private void enableAuthenticationDialog() {
    cancelButton.setVisible(false);
    authCancelButton.setVisible(true);
    usernameField.setEnabled(true);
    passwordField.setEnabled(true);
    authPanel.setVisible(true);
    if (rootPane != null) {
      rootPane.setDefaultButton(okButton);
    }
    pack();
    center();
    usernameField.grabFocus();
  }

  public void setClusterModel(IClusterModel clusterModel) {
    this.clusterModel = clusterModel;
    jmxc = new AuthenticatingJMXConnector(clusterModel);
    label.setText(appContext.format("connect-dialog.connecting.format", clusterModel));
    pack();
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public void setTimeout(long millis) {
    timeout = millis;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setConnectionListener(ConnectionListener listener) {
    this.listener = listener;
  }

  public ConnectionListener getConnectionListener() {
    return listener;
  }

  public JMXConnector getConnector() {
    return jmxc;
  }

  public Exception getError() {
    return error;
  }

  /**
   * java.awt.event.HierachyListener implementation
   */
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

  protected void fireHandleConnect() {
    if (listener != null) {
      try {
        if (error == null) {
          listener.handleConnection();
        } else {
          listener.handleException();
        }
      } catch (RuntimeException rte) {
        appContext.log(rte);
      }
    }
    isAuthenticating = false;
  }

  private class ConnectInitiator implements Runnable {
    public void run() {
      Future f = appContext.submitTask(new ConnectAction());

      error = null;
      try {
        f.get(timeout, TimeUnit.MILLISECONDS);
        hideTimer.start();
        return;
      } catch (TimeoutException te) {
        error = new TimeoutException(appContext.getString("connect-dialog.timed-out"));
        hideTimer.start();
        return;
      } catch (InterruptedException ie) {
        // interrupted by CancelButtonHandler
      } catch (Exception e) {
        Throwable cause = ExceptionHelper.getRootCause(e);
        if (!isAuthenticating && cause instanceof SecurityException) {
          isAuthenticating = true;
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              enableAuthenticationDialog();
            }
          });
        } else {
          error = e;
          hideTimer.start();
        }
      }
    }
  }

  class ConnectAction implements Callable<Void> {
    public Void call() throws Exception {
      jmxc.connect();
      return null;
    }
  }

  void tearDown() {
    Map env = clusterModel.getConnectionEnvironment();
    if (env != null) {
      env.clear();
    }

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      listener = null;
      jmxc = null;
      connectInitiator = null;
      cancelButton = null;
      hideTimer = null;
      error = null;
      label = null;
    }
  }
}
