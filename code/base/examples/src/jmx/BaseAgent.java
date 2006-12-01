/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package jmx;

// RI imports
//
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import com.sun.jdmk.comm.HtmlAdaptorServer;

public class BaseAgent {
  
  public static synchronized BaseAgent getInstance() {
    if (_theInstance == null) {
      _theInstance = new BaseAgent();
    }
    return _theInstance;
  }

  private static volatile BaseAgent _theInstance = null;
  
  private MBeanServer _server = null;

  public BaseAgent() {
    _server = MBeanServerFactory.createMBeanServer();
    initHtmlAdaptor(_server);
  }

  protected void initHtmlAdaptor(MBeanServer server) {
    try {
      HtmlAdaptorServer html = new HtmlAdaptorServer();
      ObjectName html_name = new ObjectName("Adaptor:name=html,port=8082");
      server.registerMBean(html, html_name);
      html.start();
    } catch (Exception e) {
      System.out.println("\t!!! Could not create the HTML adaptor !!!");
      e.printStackTrace();
      return;
    }

  }

  public static ObjectName buildObjectName(MBeanServer server, String beanName) throws MalformedObjectNameException,
      NullPointerException {
    ObjectName rv = null;
    String domain = server.getDefaultDomain();
    rv = new ObjectName(domain + ":type=" + beanName);
    return rv;
  }

  public void registerBean(TCStandardBean bean) {
    try {
      ObjectName objName = buildObjectName(_server, bean.getClass().getName());
      _server.registerMBean(bean, objName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      BaseAgent agent = BaseAgent.getInstance();
      SimpleStandard ssb = new SimpleStandard();
      agent.registerBean(ssb);
      System.err.println("Up and Running...");
      Thread.sleep(Long.MAX_VALUE);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}