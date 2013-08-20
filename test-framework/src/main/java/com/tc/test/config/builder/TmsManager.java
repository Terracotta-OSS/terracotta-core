package com.tc.test.config.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.util.TestBaseUtil;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Ludovic Orban
 */
public class TmsManager {

  private static final Logger LOG = LoggerFactory.getLogger(TmsManager.class);

  private final String warLocation;
  private final int listenPort;
  private final String keystorePath;
  private final String keystorePassword;
  private final boolean ssl;

  private Object server;

  public TmsManager(String warLocation, int listenPort) {
    this(warLocation, listenPort, false, null, null);
  }

  public TmsManager(String warLocation, int listenPort, boolean ssl, String keystorePath, String keystorePassword) {
    this.warLocation = warLocation;
    this.listenPort = listenPort;
    this.keystorePath = keystorePath;
    this.keystorePassword = keystorePassword;
    this.ssl = ssl;
  }

  public void start() throws Exception {
    LOG.info("Deploying TMS on port " + listenPort + " with war archive: " + warLocation);

    // those are the jetty-8.1.7.v20120910 dependencies
    URL[] urls = {
        new File(TestBaseUtil.jarFor(Class.forName("org.apache.taglibs.standard.lang.jstl.VariableResolver"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("javax.servlet.jsp.jstl.core.ConditionalTagSupport"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("com.sun.el.ExpressionFactoryImpl"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("javax.el.ELException"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("javax.servlet.ServletContext"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("javax.servlet.jsp.JspApplicationContext"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("org.apache.jasper.servlet.JspServlet"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("javax.servlet.ServletContext"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("org.eclipse.jetty.server.ssl.SslSocketConnector"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("org.eclipse.jetty.webapp.WebAppContext"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("org.eclipse.jetty.util.Attributes"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("org.objectweb.asm.ClassVisitor"))).toURI().toURL(),
        new File(TestBaseUtil.jarFor(Class.forName("org.eclipse.jetty.server.Server"))).toURI().toURL()
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

    if (!ssl) {
      server = serverClazz.getConstructor(int.class).newInstance(listenPort);
    } else {
      server = serverClazz.getConstructor().newInstance();

      Class<?> sslContextFactoryClazz = classloader.loadClass("org.eclipse.jetty.util.ssl.SslContextFactory");
      Class<?> sslSocketConnectorClazz = classloader.loadClass("org.eclipse.jetty.server.ssl.SslSocketConnector");
      Class<?> connectorClazz = classloader.loadClass("org.eclipse.jetty.server.Connector");

      Object sslContextFactory = sslContextFactoryClazz.getConstructor().newInstance();
      sslContextFactoryClazz.getMethod("setKeyStorePath", String.class).invoke(sslContextFactory, keystorePath);
      sslContextFactoryClazz.getMethod("setKeyStorePassword", String.class).invoke(sslContextFactory, keystorePassword);
      sslContextFactoryClazz.getMethod("setCertAlias", String.class).invoke(sslContextFactory, "l2");

      Object sslSocketConnector = sslSocketConnectorClazz.getConstructor(sslContextFactoryClazz).newInstance(sslContextFactory);
      sslSocketConnectorClazz.getMethod("setPort", int.class).invoke(sslSocketConnector, listenPort);

      serverClazz.getMethod("addConnector", connectorClazz).invoke(server, sslSocketConnector);
    }

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
