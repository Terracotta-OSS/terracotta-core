package com.tc.test.config.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.util.TestBaseUtil;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Ludovic Orban
 */
public class TmsManager {

  private static final Logger LOG = LoggerFactory.getLogger(TmsManager.class);

  private String warLocation;
  private int listenPort;

  private Object server;

  public TmsManager(String warLocation, int listenPort) {
    this.warLocation = warLocation;
    this.listenPort = listenPort;
  }

  public void start() throws Exception {
    LOG.info("Deploying TMS on port " + listenPort + " with war archive: " + warLocation);

    // those are the jetty-8.1.7.v20120910 dependencies
    URL[] urls = {
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("org.apache.taglibs.standard.lang.jstl.VariableResolver"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("javax.servlet.jsp.jstl.core.ConditionalTagSupport"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("com.sun.el.ExpressionFactoryImpl"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("javax.el.ELException"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("javax.servlet.jsp.JspFactory"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("org.apache.jasper.servlet.JspServlet"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("javax.servlet.ServletContext"))),
        new URL("file://" + TestBaseUtil.jarFor(Class.forName("org.eclipse.jetty.server.Server")))
    };

    ClassLoader classloader = new URLClassLoader(urls, null);
    ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classloader);

    Class<?> serverClazz = classloader.loadClass("org.eclipse.jetty.server.Server");
    Class<?> handlerClazz = classloader.loadClass("org.eclipse.jetty.server.Handler");
    Class<?> webAppContextClazz = classloader.loadClass("org.eclipse.jetty.webapp.WebAppContext");

    Object webAppContext = webAppContextClazz.newInstance();
    webAppContextClazz.getMethod("setContextPath", String.class).invoke(webAppContext, "/tmc");
    webAppContextClazz.getMethod("setWar", String.class).invoke(webAppContext, warLocation);

    server = serverClazz.getConstructor(int.class).newInstance(listenPort);
    serverClazz.getMethod("setHandler", handlerClazz).invoke(server, webAppContext);
    
    serverClazz.getMethod("start").invoke(server);
    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
  }

  public void stop() throws Exception {
    if (server != null) {
      server.getClass().getMethod("stop").invoke(server);
      server.getClass().getMethod("join").invoke(server);
      server = null;
    }
  }

}
