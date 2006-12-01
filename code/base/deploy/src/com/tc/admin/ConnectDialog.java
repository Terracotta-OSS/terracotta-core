/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.Dialog;
import org.dijon.DialogResource;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.Timer;

public class ConnectDialog extends Dialog {
  private JMXServiceURL      m_url;
  private Map                m_env;
  private long               m_timeout;
  private ConnectionListener m_listener;
  private JMXConnector       m_jmxc;
  private Thread             m_mainThread;
  private Thread             m_connectThread;
  private Button             m_cancelButton;
  private Timer              m_timer;
  private Exception          m_error;

  public ConnectDialog(
    JMXServiceURL      url,
    Map                env,
    long               timeout,
    ConnectionListener listener)
  {
    super();

    m_url      = url;
    m_env      = env;
    m_timeout  = timeout;
    m_listener = listener;

    AdminClientContext acc = AdminClient.getContext();
    load((DialogResource)acc.topRes.child("ConnectDialog"));

    m_cancelButton = (Button)findComponent("CancelButton");
    m_cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_cancelButton.setEnabled(false);
        m_mainThread.interrupt();
        m_jmxc = null;
        m_error = new InterruptedIOException("Interrupted");
        ConnectDialog.this.setVisible(false);
      }
    });
    getContentPane().addHierarchyListener(new HL());

    int delay = 1000;
    ActionListener taskPerformer = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        setVisible(false);
      }
    };
    m_timer = new Timer(delay, taskPerformer);
    m_timer.setRepeats(false);
  }

  public void setServiceURL(JMXServiceURL url) {
    m_url = url;
  }

  public JMXServiceURL getServiceURL() {
    return m_url;
  }

  public void setEnvironment(Map env) {
    m_env = env;
  }

  public Map getEnvironment() {
    return m_env;
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

  class HL implements HierarchyListener {
    public void hierarchyChanged(HierarchyEvent e) {
      long flags = e.getChangeFlags();

      if((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
        if(isShowing()) {
          m_cancelButton.setEnabled(true);
          m_mainThread = new MainThread();
          m_mainThread.start();
        }
        else {
          fireHandleConnect();
        }
      }
    }
  }

  protected void fireHandleConnect() {
    if(m_listener != null) {
      try {
        if(m_error == null) {
          m_listener.handleConnection();
        }
        else {
          m_listener.handleException();
        }
      } catch(RuntimeException rte) {
        rte.printStackTrace();
      }
    }
  }

  class MainThread extends Thread {
    public void run() {
      m_connectThread = new ConnectThread();

      try {
        m_error = null;
        m_jmxc  = JMXConnectorFactory.newJMXConnector(m_url, m_env);

        if(m_jmxc != null && m_error == null) {
          m_connectThread.start();
          m_connectThread.join(m_timeout);
        }
      } catch(IOException e) {
        m_error = e;
      } catch(InterruptedException e) {
        m_error = new InterruptedIOException("Interrupted");
      }

      if(m_error != null && m_connectThread.isAlive()) {
        m_connectThread.interrupt();
        m_error = new InterruptedIOException("Connection timed out");
      }

      m_timer.start();
    }
  }

  class ConnectThread extends Thread {
    public void run() {
      try {
        m_jmxc.connect(m_env);
      } catch(IOException e) {
        m_error = e;
      } catch(RuntimeException e) {
        m_error = e;
      }
    }
  }
  
  void tearDown() {
    if(m_env != null) {
      m_env.clear();
    }
    
    m_url           = null;
    m_env           = null;
    m_listener      = null;
    m_jmxc          = null;
    m_mainThread    = null;
    m_connectThread = null;
    m_cancelButton  = null;
    m_timer         = null;
    m_error         = null;
  } 
}
