/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IServer;
import com.tc.management.beans.TCServerInfoMBean;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;

public class ServerLoggingPanel extends XContainer implements NotificationListener, PropertyChangeListener {
  protected ApplicationContext         appContext;
  protected IServer                    server;

  protected XCheckBox                  faultDebugCheckBox;
  protected XCheckBox                  requestDebugCheckBox;
  protected XCheckBox                  flushDebugCheckBox;
  protected XCheckBox                  broadcastDebugCheckBox;
  protected XCheckBox                  commitDebugCheckBox;

  protected XCheckBox                  verboseGCCheckBox;
  protected XButton                    gcButton;

  protected ActionListener             loggingChangeHandler;
  protected HashMap<String, XCheckBox> loggingControlMap;

  public ServerLoggingPanel(ApplicationContext appContext, IServer server) {
    super(new GridBagLayout());

    this.appContext = appContext;
    this.server = server;

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.NORTH;

    add(createDSOPanel(), gbc);
    gbc.gridx++;

    add(createGCPanel(), gbc);
    gbc.gridx++;

    // filler
    gbc.weightx = gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    add(new XLabel(), gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    add(new XLabel(), gbc);

    loggingControlMap = new HashMap<String, XCheckBox>();
    loggingChangeHandler = new LoggingChangeHandler();

    server.addPropertyChangeListener(this);
  }

  private XContainer createDSOPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.weightx = gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.WEST;

    panel.add(faultDebugCheckBox = new XCheckBox("FaultDebug"), gbc);
    faultDebugCheckBox.setName("FaultDebug");
    faultDebugCheckBox.setToolTipText("Fault from disk");
    gbc.gridy++;

    panel.add(requestDebugCheckBox = new XCheckBox("RequestDebug"), gbc);
    requestDebugCheckBox.setName("RequestDebug");
    requestDebugCheckBox.setToolTipText("Object request");
    gbc.gridy++;

    panel.add(flushDebugCheckBox = new XCheckBox("FlushDebug"), gbc);
    flushDebugCheckBox.setName("FlushDebug");
    flushDebugCheckBox.setToolTipText("Flush to disk");
    gbc.gridy++;

    panel.add(broadcastDebugCheckBox = new XCheckBox("BroadcastDebug"), gbc);
    broadcastDebugCheckBox.setName("BroadcastDebug");
    broadcastDebugCheckBox.setToolTipText("Change broadcast");
    gbc.gridy++;

    panel.add(commitDebugCheckBox = new XCheckBox("CommitDebug"), gbc);
    commitDebugCheckBox.setName("CommitDebug");
    commitDebugCheckBox.setToolTipText("Commit to database");

    panel.setBorder(BorderFactory.createTitledBorder("DSO"));

    return panel;
  }

  private XContainer createGCPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    panel.add(verboseGCCheckBox = new XCheckBox("Verbose"), gbc);
    verboseGCCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        server.setVerboseGC(verboseGCCheckBox.isSelected());
      }
    });
    gbc.gridy++;

    panel.add(gcButton = new XButton("Request GC"), gbc);
    gcButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        server.gc();
      }
    });
    gbc.gridy++;

    panel.setBorder(BorderFactory.createTitledBorder("Java GC"));

    return panel;
  }

  synchronized IServer getServer() {
    return server;
  }

  synchronized ApplicationContext getApplicationContext() {
    return appContext;
  }

  public void setupLoggingControls() {
    IServer theServer = getServer();
    if (theServer != null && theServer.isReady()) {
      setupLoggingControl(faultDebugCheckBox, theServer);
      setupLoggingControl(requestDebugCheckBox, theServer);
      setupLoggingControl(flushDebugCheckBox, theServer);
      setupLoggingControl(broadcastDebugCheckBox, theServer);
      setupLoggingControl(commitDebugCheckBox, theServer);

      verboseGCCheckBox.setSelected(server.isVerboseGC());
    }
  }

  private void setupLoggingControl(XCheckBox checkBox, Object bean) {
    setLoggingControl(checkBox, bean);
    checkBox.putClientProperty(checkBox.getName(), bean);
    checkBox.addActionListener(loggingChangeHandler);
    loggingControlMap.put(checkBox.getName(), checkBox);
  }

  private void setLoggingControl(XCheckBox checkBox, Object bean) {
    ApplicationContext theAppContext = getApplicationContext();
    if (theAppContext == null) return;
    try {
      Class beanClass = bean.getClass();
      Method setter = beanClass.getMethod("get" + checkBox.getName(), new Class[0]);
      Boolean value = (Boolean) setter.invoke(bean, new Object[0]);
      checkBox.setSelected(value.booleanValue());
    } catch (Exception e) {
      theAppContext.log(e);
    }
  }

  private class LoggingChangeWorker extends BasicWorker<Void> {
    private LoggingChangeWorker(final Object loggingBean, final String attrName, final boolean enabled) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          ApplicationContext theAppContext = getApplicationContext();
          if (theAppContext == null) return null;
          Class beanClass = loggingBean.getClass();
          Method setter = beanClass.getMethod("set" + attrName, new Class[] { Boolean.TYPE });
          setter.invoke(loggingBean, new Object[] { Boolean.valueOf(enabled) });
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext == null) return;
      Exception e = getException();
      if (e != null) {
        theAppContext.log(e);
      }
    }
  }

  class LoggingChangeHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      XCheckBox checkBox = (XCheckBox) ae.getSource();
      Object loggingBean = checkBox.getClientProperty(checkBox.getName());
      String attrName = checkBox.getName();
      boolean enabled = checkBox.isSelected();

      appContext.execute(new LoggingChangeWorker(loggingBean, attrName, enabled));
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (type.startsWith("tc.logging.")) {
      String name = type.substring(type.lastIndexOf('.') + 1);
      XCheckBox checkBox = loggingControlMap.get(name);
      if (checkBox != null) {
        checkBox.setSelected(Boolean.valueOf(notification.getMessage()));
      }
    } else if (type.equals(TCServerInfoMBean.VERBOSE_GC)) {
      Boolean value = (Boolean) ((AttributeChangeNotification) notification).getNewValue();
      verboseGCCheckBox.setSelected(value.booleanValue());
    }
  }

  public void propertyChange(final PropertyChangeEvent evt) {
    final String prop = evt.getPropertyName();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (prop.startsWith("tc.logging.")) {
          String name = prop.substring(prop.lastIndexOf('.') + 1);
          XCheckBox checkBox = loggingControlMap.get(name);
          if (checkBox != null) {
            checkBox.setSelected((Boolean) evt.getNewValue());
          }
        } else if (prop.equals("VerboseGC")) {
          Boolean value = (Boolean) evt.getNewValue();
          verboseGCCheckBox.setSelected(value.booleanValue());
        }
      }
    });
  }

  @Override
  public void tearDown() {
    server.removePropertyChangeListener(this);

    synchronized (this) {
      appContext = null;
      server = null;

      faultDebugCheckBox = null;
      requestDebugCheckBox = null;
      flushDebugCheckBox = null;
      broadcastDebugCheckBox = null;
      commitDebugCheckBox = null;
      verboseGCCheckBox = null;

      loggingChangeHandler = null;
      loggingControlMap.clear();
      loggingControlMap = null;
    }

    super.tearDown();
  }
}
