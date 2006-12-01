/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.framework.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class RemoteContextListener implements ServletContextListener {

  private ClassPathXmlApplicationContext remoteAppCtx;

  public void contextInitialized(ServletContextEvent event) {
    WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(event.getServletContext());
    remoteAppCtx = new ClassPathXmlApplicationContext(
        new String[] { "classpath:/com/tctest/spring/spring-remoting.xml" }, wac);

  }

  public void contextDestroyed(ServletContextEvent event) {
    remoteAppCtx.close();
  }

}
