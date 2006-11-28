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
