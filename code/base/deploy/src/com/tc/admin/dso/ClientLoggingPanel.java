/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModelElement;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;

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

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;

public class ClientLoggingPanel extends XContainer implements NotificationListener, PropertyChangeListener {
  protected ApplicationContext         appContext;
  protected IClient                    client;

  protected XCheckBox                  classCheckBox;
  protected XCheckBox                  locksCheckBox;
  protected XCheckBox                  transientRootCheckBox;
  protected XCheckBox                  rootsCheckBox;
  protected XCheckBox                  distributedMethodsCheckBox;

  protected XCheckBox                  nonPortableDumpCheckBox;
  protected XCheckBox                  lockDebugCheckBox;
  protected XCheckBox                  fieldChangeDebugCheckBox;
  protected XCheckBox                  waitNotifyDebugCheckBox;
  protected XCheckBox                  distributedMethodDebugCheckBox;
  protected XCheckBox                  newObjectDebugCheckBox;
  protected XCheckBox                  flushDebugCheckBox;
  protected XCheckBox                  faultDebugCheckBox;

  protected XCheckBox                  autoLockDetailsCheckBox;
  protected XCheckBox                  callerCheckBox;
  protected XCheckBox                  fullStackCheckBox;

  protected ActionListener             loggingChangeHandler;
  protected HashMap<String, XCheckBox> loggingControlMap;

  public ClientLoggingPanel(ApplicationContext appContext, IClient client) {
    super(new GridBagLayout());

    this.appContext = appContext;

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.NORTH;

    add(createInstrumentationLoggingPanel(), gbc);
    gbc.gridx++;

    add(createRuntimeLoggingPanel(), gbc);
    gbc.gridx++;

    add(createObjectManagerOptionsPanel(), gbc);
    gbc.gridx++;

    loggingControlMap = new HashMap<String, XCheckBox>();
    loggingChangeHandler = new LoggingChangeHandler();

    setClient(client);
  }

  private XContainer createInstrumentationLoggingPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    panel.add(classCheckBox = new XCheckBox("Class"), gbc);
    classCheckBox.setName("Class1");
    gbc.gridy++;

    panel.add(locksCheckBox = new XCheckBox("Locks"), gbc);
    locksCheckBox.setName("Locks");
    gbc.gridy++;

    panel.add(transientRootCheckBox = new XCheckBox("TransientRoot"), gbc);
    transientRootCheckBox.setName("TransientRoot");
    gbc.gridy++;

    panel.add(rootsCheckBox = new XCheckBox("Roots"), gbc);
    rootsCheckBox.setName("Roots");
    gbc.gridy++;

    panel.add(distributedMethodsCheckBox = new XCheckBox("DistributedMethods"), gbc);
    distributedMethodsCheckBox.setName("DistributedMethods");
    gbc.gridy++;

    panel.setBorder(BorderFactory.createTitledBorder("Instrumentation"));

    return panel;
  }

  private XContainer createRuntimeLoggingPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    panel.add(nonPortableDumpCheckBox = new XCheckBox("NonPortableDump"), gbc);
    nonPortableDumpCheckBox.setName("NonPortableDump");
    gbc.gridy++;

    panel.add(lockDebugCheckBox = new XCheckBox("LockDebug"), gbc);
    lockDebugCheckBox.setName("LockDebug");
    gbc.gridy++;

    panel.add(fieldChangeDebugCheckBox = new XCheckBox("FieldChangeDebug"), gbc);
    fieldChangeDebugCheckBox.setName("FieldChangeDebug");
    gbc.gridy++;

    panel.add(waitNotifyDebugCheckBox = new XCheckBox("WaitNotifyDebug"), gbc);
    waitNotifyDebugCheckBox.setName("WaitNotifyDebug");
    gbc.gridy++;

    panel.add(distributedMethodDebugCheckBox = new XCheckBox("DistributedMethodDebug"), gbc);
    distributedMethodDebugCheckBox.setName("DistributedMethodDebug");
    gbc.gridy++;

    panel.add(newObjectDebugCheckBox = new XCheckBox("NewObjectDebug"), gbc);
    newObjectDebugCheckBox.setName("NewObjectDebug");
    gbc.gridy++;

    gbc.anchor = GridBagConstraints.CENTER;
    panel.add(createOutputOptionsPanel(), gbc);

    panel.setBorder(BorderFactory.createTitledBorder("Runtime"));

    return panel;
  }

  private XContainer createOutputOptionsPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    panel.add(autoLockDetailsCheckBox = new XCheckBox("AutoLockDetails"), gbc);
    autoLockDetailsCheckBox.setName("AutoLockDetails");
    gbc.gridy++;

    panel.add(callerCheckBox = new XCheckBox("Caller"), gbc);
    callerCheckBox.setName("Caller");
    gbc.gridy++;

    panel.add(fullStackCheckBox = new XCheckBox("FullStack"), gbc);
    fullStackCheckBox.setName("FullStack");
    gbc.gridy++;

    panel.setBorder(BorderFactory.createTitledBorder("Output Options"));

    return panel;
  }

  private XContainer createObjectManagerOptionsPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    panel.add(flushDebugCheckBox = new XCheckBox("FlushDebug"), gbc);
    flushDebugCheckBox.setName("FlushDebug");
    gbc.gridy++;

    panel.add(faultDebugCheckBox = new XCheckBox("FaultDebug"), gbc);
    faultDebugCheckBox.setName("FaultDebug");
    gbc.gridy++;

    panel.setBorder(BorderFactory.createTitledBorder("ObjectManager"));

    return panel;
  }

  public void setClient(IClient client) {
    this.client = client;

    if (client.isReady()) {
      try {
        setupTunneledBeans();
      } catch (Exception e) {
        appContext.log(e);
      }
    } else {
      client.addPropertyChangeListener(this);
    }
  }

  public IClient getClient() {
    return client;
  }

  private void setupTunneledBeans() throws Exception {
    setupInstrumentationLogging();
    setupRuntimeLogging();
    setupRuntimeOutputOptions();
  }

  private void setupInstrumentationLogging() throws Exception {
    InstrumentationLoggingMBean instrumentationLoggingBean = client.getInstrumentationLoggingBean();

    setupLoggingControl(classCheckBox, instrumentationLoggingBean);
    setupLoggingControl(locksCheckBox, instrumentationLoggingBean);
    setupLoggingControl(transientRootCheckBox, instrumentationLoggingBean);
    setupLoggingControl(rootsCheckBox, instrumentationLoggingBean);
    setupLoggingControl(distributedMethodsCheckBox, instrumentationLoggingBean);
  }

  private void setupRuntimeLogging() throws Exception {
    RuntimeLoggingMBean runtimeLoggingBean = client.getRuntimeLoggingBean();

    setupLoggingControl(nonPortableDumpCheckBox, runtimeLoggingBean);
    setupLoggingControl(lockDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(fieldChangeDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(waitNotifyDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(distributedMethodDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(newObjectDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(flushDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(faultDebugCheckBox, runtimeLoggingBean);
  }

  private void setupRuntimeOutputOptions() throws Exception {
    RuntimeOutputOptionsMBean runtimeOutputOptionsBean = client.getRuntimeOutputOptionsBean();

    setupLoggingControl(autoLockDetailsCheckBox, runtimeOutputOptionsBean);
    setupLoggingControl(callerCheckBox, runtimeOutputOptionsBean);
    setupLoggingControl(fullStackCheckBox, runtimeOutputOptionsBean);
  }

  private void setupLoggingControl(XCheckBox checkBox, Object bean) {
    setLoggingControl(checkBox, bean);
    checkBox.putClientProperty(checkBox.getName(), bean);
    checkBox.addActionListener(loggingChangeHandler);
    loggingControlMap.put(checkBox.getName(), checkBox);
  }

  private void setLoggingControl(XCheckBox checkBox, Object bean) {
    try {
      Class beanClass = bean.getClass();
      Method setter = beanClass.getMethod("get" + checkBox.getName(), new Class[0]);
      Boolean value = (Boolean) setter.invoke(bean, new Object[0]);
      checkBox.setSelected(value.booleanValue());
    } catch (Exception e) {
      appContext.log(e);
    }
  }

  private class LoggingChangeWorker extends BasicWorker<Void> {
    private LoggingChangeWorker(final Object loggingBean, final String attrName, final boolean enabled) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          Class beanClass = loggingBean.getClass();
          Method setter = beanClass.getMethod("set" + attrName, new Class[] { Boolean.TYPE });
          setter.invoke(loggingBean, new Object[] { Boolean.valueOf(enabled) });
          return null;
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
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
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModelElement.PROP_READY.equals(prop)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            setupTunneledBeans();
          } catch (Exception e) {
            appContext.log(e);
          }
        }
      });
    } else if (prop.startsWith("tc.logging.")) {
      String name = prop.substring(prop.lastIndexOf('.') + 1);
      XCheckBox checkBox = loggingControlMap.get(name);
      if (checkBox != null) {
        checkBox.setSelected((Boolean) evt.getNewValue());
      }
    }
  }

  public void tearDown() {
    super.tearDown();

    appContext = null;
    client = null;

    classCheckBox = null;
    locksCheckBox = null;
    transientRootCheckBox = null;
    rootsCheckBox = null;
    distributedMethodsCheckBox = null;
    nonPortableDumpCheckBox = null;
    lockDebugCheckBox = null;
    fieldChangeDebugCheckBox = null;
    waitNotifyDebugCheckBox = null;
    distributedMethodDebugCheckBox = null;
    newObjectDebugCheckBox = null;
    flushDebugCheckBox = null;
    faultDebugCheckBox = null;
    autoLockDetailsCheckBox = null;
    callerCheckBox = null;
    fullStackCheckBox = null;

    loggingChangeHandler = null;
    loggingControlMap.clear();
    loggingControlMap = null;
  }
}
