/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.CheckBox;
import org.dijon.ContainerResource;
import org.dijon.TextArea;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.SearchPanel;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextArea;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterNode;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

public class ClientPanel extends XContainer implements NotificationListener, PropertyChangeListener {
  protected AdminClientContext        m_acc;
  protected ClientNode                m_clientNode;
  protected IClient                   m_client;

  protected PropertyTable             m_propertyTable;

  protected TextArea                  m_environmentTextArea;
  protected TextArea                  m_configTextArea;

  protected CheckBox                  m_classCheckBox;
  protected CheckBox                  m_locksCheckBox;
  protected CheckBox                  m_transientRootCheckBox;
  protected CheckBox                  m_rootsCheckBox;
  protected CheckBox                  m_distributedMethodsCheckBox;

  protected CheckBox                  m_nonPortableDumpCheckBox;
  protected CheckBox                  m_lockDebugCheckBox;
  protected CheckBox                  m_fieldChangeDebugCheckBox;
  protected CheckBox                  m_waitNotifyDebugCheckBox;
  protected CheckBox                  m_distributedMethodDebugCheckBox;
  protected CheckBox                  m_newObjectDebugCheckBox;

  protected CheckBox                  m_autoLockDetailsCheckBox;
  protected CheckBox                  m_callerCheckBox;
  protected CheckBox                  m_fullStackCheckBox;

  protected ActionListener            m_loggingChangeHandler;
  protected HashMap<String, CheckBox> m_loggingControlMap;

  public ClientPanel(ClientNode clientNode) {
    super();

    m_acc = AdminClient.getContext();

    load((ContainerResource) m_acc.getComponent("ClientPanel"));

    m_propertyTable = (PropertyTable) findComponent("ClientInfoTable");
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    m_propertyTable.setDefaultRenderer(Long.class, renderer);
    m_propertyTable.setDefaultRenderer(Integer.class, renderer);

    m_environmentTextArea = (XTextArea) findComponent("EnvironmentTextArea");
    ((SearchPanel) findComponent("EnvironmentSearchPanel")).setTextComponent(m_environmentTextArea);

    m_configTextArea = (XTextArea) findComponent("ConfigTextArea");
    ((SearchPanel) findComponent("ConfigSearchPanel")).setTextComponent(m_configTextArea);

    m_classCheckBox = (CheckBox) findComponent("Class1");
    m_locksCheckBox = (CheckBox) findComponent("Locks");
    m_transientRootCheckBox = (CheckBox) findComponent("TransientRoot");
    m_rootsCheckBox = (CheckBox) findComponent("Roots");
    m_distributedMethodsCheckBox = (CheckBox) findComponent("DistributedMethods");

    m_nonPortableDumpCheckBox = (CheckBox) findComponent("NonPortableDump");
    m_lockDebugCheckBox = (CheckBox) findComponent("LockDebug");
    m_fieldChangeDebugCheckBox = (CheckBox) findComponent("FieldChangeDebug");
    m_waitNotifyDebugCheckBox = (CheckBox) findComponent("WaitNotifyDebug");
    m_distributedMethodDebugCheckBox = (CheckBox) findComponent("DistributedMethodDebug");
    m_newObjectDebugCheckBox = (CheckBox) findComponent("NewObjectDebug");

    m_autoLockDetailsCheckBox = (CheckBox) findComponent("AutoLockDetails");
    m_callerCheckBox = (CheckBox) findComponent("Caller");
    m_fullStackCheckBox = (CheckBox) findComponent("FullStack");

    m_loggingControlMap = new HashMap<String, CheckBox>();

    setClient(clientNode.getClient());
  }

  public void setClient(IClient client) {
    m_client = client;

    String[] fields = { "Host", "Port", "ChannelID" };
    m_propertyTable.setModel(new PropertyTableModel(client, fields, fields));

    m_loggingChangeHandler = new LoggingChangeHandler();

    if (client.isReady()) {
      try {
        setupTunneledBeans();
      } catch (Exception e) {
        m_acc.log(e);
      }
    } else {
      m_client.addPropertyChangeListener(this);
    }
  }

  public IClient getClient() {
    return m_client;
  }

  private void setupTunneledBeans() throws Exception {
    m_environmentTextArea.setText(m_client.getEnvironment());
    m_configTextArea.setText(m_client.getConfig());

    setupInstrumentationLogging();
    setupRuntimeLogging();
    setupRuntimeOutputOptions();
  }

  private void setupInstrumentationLogging() throws Exception {
    InstrumentationLoggingMBean instrumentationLoggingBean = m_client.getInstrumentationLoggingBean();

    setupLoggingControl(m_classCheckBox, instrumentationLoggingBean);
    setupLoggingControl(m_locksCheckBox, instrumentationLoggingBean);
    setupLoggingControl(m_transientRootCheckBox, instrumentationLoggingBean);
    setupLoggingControl(m_rootsCheckBox, instrumentationLoggingBean);
    setupLoggingControl(m_distributedMethodsCheckBox, instrumentationLoggingBean);
  }

  private void setupRuntimeLogging() throws Exception {
    RuntimeLoggingMBean runtimeLoggingBean = m_client.getRuntimeLoggingBean();

    setupLoggingControl(m_nonPortableDumpCheckBox, runtimeLoggingBean);
    setupLoggingControl(m_lockDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(m_fieldChangeDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(m_waitNotifyDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(m_distributedMethodDebugCheckBox, runtimeLoggingBean);
    setupLoggingControl(m_newObjectDebugCheckBox, runtimeLoggingBean);
  }

  private void setupRuntimeOutputOptions() throws Exception {
    RuntimeOutputOptionsMBean runtimeOutputOptionsBean = m_client.getRuntimeOutputOptionsBean();

    setupLoggingControl(m_autoLockDetailsCheckBox, runtimeOutputOptionsBean);
    setupLoggingControl(m_callerCheckBox, runtimeOutputOptionsBean);
    setupLoggingControl(m_fullStackCheckBox, runtimeOutputOptionsBean);
  }

  private void setupLoggingControl(CheckBox checkBox, Object bean) {
    setLoggingControl(checkBox, bean);
    checkBox.putClientProperty(checkBox.getName(), bean);
    checkBox.addActionListener(m_loggingChangeHandler);
    m_loggingControlMap.put(checkBox.getName(), checkBox);
  }

  private void setLoggingControl(CheckBox checkBox, Object bean) {
    try {
      Class beanClass = bean.getClass();
      Method setter = beanClass.getMethod("get" + checkBox.getName(), new Class[0]);
      Boolean value = (Boolean) setter.invoke(bean, new Object[0]);
      checkBox.setSelected(value.booleanValue());
    } catch (Exception e) {
      m_acc.log(e);
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
        m_acc.log(e);
      }
    }
  }

  class LoggingChangeHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      CheckBox checkBox = (CheckBox) ae.getSource();
      Object loggingBean = checkBox.getClientProperty(checkBox.getName());
      String attrName = checkBox.getName();
      boolean enabled = checkBox.isSelected();

      m_acc.execute(new LoggingChangeWorker(loggingBean, attrName, enabled));
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (type.startsWith("tc.logging.")) {
      String name = type.substring(type.lastIndexOf('.') + 1);
      CheckBox checkBox = m_loggingControlMap.get(name);
      if (checkBox != null) {
        checkBox.setSelected(Boolean.valueOf(notification.getMessage()));
      }
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterNode.PROP_READY.equals(prop)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            setupTunneledBeans();
          } catch (Exception e) {
            m_acc.log(e);
          }
        }
      });
    } else if (prop.startsWith("tc.logging.")) {
      String name = prop.substring(prop.lastIndexOf('.') + 1);
      CheckBox checkBox = m_loggingControlMap.get(name);
      if (checkBox != null) {
        checkBox.setSelected((Boolean) evt.getNewValue());
      }
    }
  }

  public void tearDown() {
    super.tearDown();

    m_acc = null;
    m_clientNode = null;
    m_client = null;

    m_propertyTable = null;

    m_environmentTextArea = null;
    m_configTextArea = null;

    m_classCheckBox = null;
    m_locksCheckBox = null;
    m_transientRootCheckBox = null;
    m_rootsCheckBox = null;
    m_distributedMethodsCheckBox = null;
    m_nonPortableDumpCheckBox = null;
    m_lockDebugCheckBox = null;
    m_fieldChangeDebugCheckBox = null;
    m_waitNotifyDebugCheckBox = null;
    m_distributedMethodDebugCheckBox = null;
    m_newObjectDebugCheckBox = null;
    m_autoLockDetailsCheckBox = null;
    m_callerCheckBox = null;
    m_fullStackCheckBox = null;

    m_loggingChangeHandler = null;
    m_loggingControlMap.clear();
    m_loggingControlMap = null;
  }
}
