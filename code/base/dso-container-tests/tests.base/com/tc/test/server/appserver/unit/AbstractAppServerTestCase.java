/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.unit;

import org.apache.commons.lang.ClassUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.Lock;
import com.tc.object.config.schema.Root;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.test.server.Server;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerResult;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.war.AbstractDescriptorXml;
import com.tc.test.server.appserver.war.War;
import com.tc.test.server.dsoserver.DsoServer;
import com.tc.test.server.dsoserver.StandardDsoServer;
import com.tc.test.server.dsoserver.StandardDsoServerParameters;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;
import com.tc.test.server.tcconfig.TerracottaServerConfigGenerator;
import com.tc.test.server.util.AppServerUtil;
import com.tc.util.Assert;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.ThreadDump;
import com.terracotta.session.util.ConfigProperties;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public abstract class AbstractAppServerTestCase extends TCTestCase {

  private static final TCLogger             logger             = TCLogging.getLogger(AbstractAppServerTestCase.class);

  private static final SynchronizedInt      nodeCounter        = new SynchronizedInt(-1);
  private static final String               NODE               = "node-";
  private static final String               DOMAIN             = "localhost";

  protected final List                      appservers         = new ArrayList();
  private final Object                      workingDirLock     = new Object();
  private final List                        dsoServerJvmArgs   = new ArrayList();
  private final List                        roots              = new ArrayList();
  private final List                        locks              = new ArrayList();
  private final List                        includes           = new ArrayList();
  private final TestConfigObject            config             = TestConfigObject.getInstance();

  private File                              serverInstallDir;
  private File                              sandbox;
  private File                              tempDir;
  private File                              bootJar;
  private NewAppServerFactory               appServerFactory;
  private AppServerInstallation             installation;
  private File                              warFile;
  private DsoServer                         dsoServer;
  private TerracottaServerConfigGenerator   configGen;
  private StandardTerracottaAppServerConfig configBuilder;

  private List                              filterList         = new ArrayList();
  private List                              listenerList       = new ArrayList();
  private List                              servletList        = new ArrayList();

  private boolean                           isSynchronousWrite = false;

  public AbstractAppServerTestCase() {
    // keep the regular thread dump behavior for windows and macs
    setDumpThreadsOnTimeout(Os.isWindows() || Os.isMac());

    String appserver = config.appserverFactoryName();
    // XXX: Only non-session container tests work in glassfish and jetty at the moment
    if (isSessionTest()
        && (NewAppServerFactory.GLASSFISH.equals(appserver) || NewAppServerFactory.JETTY.equals(appserver))) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }

    initTestEnvironment();
  }

  private void initTestEnvironment() {
    try {
      tempDir = getTempDirectory();
      serverInstallDir = config.appserverServerInstallDir();
      bootJar = new File(config.normalBootJar());
      appServerFactory = NewAppServerFactory.createFactoryFromProperties(config);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    sandbox = AppServerUtil.createSandbox(tempDir);
    installation = AppServerUtil.createAppServerInstallation(appServerFactory, serverInstallDir, sandbox);
    configBuilder = appServerFactory.createTcConfig(installation.dataDirectory());
  }

  /**
   * If overridden <tt>super.tearDown()</tt> must be called to ensure that servers are all shutdown properly
   * 
   * @throws Exception
   */
  protected void tearDown() throws Exception {
    logger.info("tearDown() called, stopping servers and archiving sandbox");
    for (Iterator iter = appservers.iterator(); iter.hasNext();) {
      Server server = (Server) iter.next();
      server.stop();
    }
    if (dsoServer != null && dsoServer.isRunning()) dsoServer.stop();

    AppServerUtil.shutdownAndArchive(sandbox, new File(tempDir, getName()));
    super.tearDown();
  }

  protected int getJMXPort() {
    if (configGen == null) { throw new AssertionError("DSO server is not running"); }
    return configGen.getConfig().getJmxPort();
  }

  protected final boolean cleanTempDir() {
    return false;
  }

  protected void beforeTimeout() throws Throwable {
    logger
        .warn("beforeTimeout() called: a timeout has occurred.  Calling threadDumpGroup() and archiving sandbox logs",
              new Exception("Stack trace of timeout timer"));
    threadDumpGroup();

    // make an archive of the workingDir since it will not be renamed when test times out
    archiveSandboxLogs();
  }

  private void archiveSandboxLogs() {
    synchronized (workingDirLock) {
      if (installation != null) {
        String src = installation.sandboxDirectory().getAbsolutePath();
        String dest = new File(tempDir, "archive-logs-" + System.currentTimeMillis() + ".zip").getAbsolutePath();

        StringBuffer msg = new StringBuffer("\n");
        msg.append("*****************************\n");
        msg.append("* Archiving logs in [").append(src).append("] to ").append(dest).append("\n");
        msg.append("*****************************\n");
        logger.info(msg.toString());

        Zip zip = new Zip();
        zip.setProject(new Project());
        zip.setDestFile(new File(dest));
        zip.setBasedir(new File(src));
        zip.setIncludes("**/*.log");
        zip.setIncludes("**/java_pid*.hprof"); // OOME heap dumps
        zip.setUpdate(false);
        zip.execute();
      }
    }
  }

  /**
   * Starts a DSO server using a generated tc-config.xml
   */
  protected final void startDsoServer() throws Exception {
    Assert.assertNull(dsoServer);
    dsoServer = new StandardDsoServer();

    if (dsoServerJvmArgs != null && !dsoServerJvmArgs.isEmpty()) {
      dsoServer.addJvmArgs(dsoServerJvmArgs);
    }

    TerracottaServerConfigGenerator generator = configGen();

    File dsoWorkingDir = installation.dataDirectory();
    File outputFile = new File(dsoWorkingDir, "dso-server.log");

    StandardDsoServerParameters params = new StandardDsoServerParameters(generator, dsoWorkingDir, outputFile,
                                                                         generator.getConfig().getDsoPort(), generator
                                                                             .getConfig().getJmxPort());

    dsoServer.start(params);
  }

  /*
   * This method should be called before DSO server is started.
   */
  protected final void addDsoServerJvmArgs(List jvmArgs) {
    dsoServerJvmArgs.addAll(jvmArgs);
  }

  /*
   * This method should be called before DSO server is started.
   */
  protected final void setSynchronousWrite(boolean value) {
    isSynchronousWrite = value;
  }

  /*
   * This method should be called before DSO server is started.
   */
  protected final void addRoot(Root dsoRoot) {
    roots.add(dsoRoot);
  }

  /*
   * This method should be called before DSO server is started.
   */
  protected final void addLock(Lock lock) {
    locks.add(lock);
  }

  protected final void addInclude(String expression) {
    includes.add(expression);
  }

  protected final void addConfigModule(String name, String version) {
    configBuilder.addModule(name, version);
  }

  /**
   * Starts an instance of the assigned default application server listed in testconfig.properties. Servlets and the WAR
   * are dynamically generated using the convention listed in the header of this document.
   * 
   * @param dsoEnabled - enable or disable dso for this instance
   * @return AppServerResult - series of return values including the server port assigned to this instance
   */
  protected final AppServerResult startAppServer(boolean dsoEnabled) throws Exception {
    return startAppServer(dsoEnabled, new Properties());
  }

  /**
   * @see startAppServer(boolean dsoEnabled)
   * @param props - <tt>Properties</tt> available as <tt>System.properties</tt> to servlets delopyed in this
   *        container
   */
  protected final AppServerResult startAppServer(boolean dsoEnabled, Properties props) throws Exception {
    return startAppServer(dsoEnabled, props, null);
  }

  /**
   * @see startAppServer(boolean dsoEnabled, Properties props)
   * @param jvmargs - Array of additional jvm arguments to use when starting the app server
   */
  protected final AppServerResult startAppServer(boolean dsoEnabled, Properties props, String[] jvmargs)
      throws Exception {
    int nodeNumber = nodeCounter.increment();
    try {
      StandardAppServerParameters params;
      params = (StandardAppServerParameters) appServerFactory.createParameters(NODE + nodeNumber, props);
      AppServer appServer = appServerFactory.createAppServer(installation);

      if (dsoEnabled) {
        params.enableDSO(configGen(), bootJar);
      }
      if (jvmargs != null) {
        for (int i = 0; i < jvmargs.length; i++) {
          params.appendJvmArgs(jvmargs[i]);
        }
      }

      addAppServerSpecificJvmArg(NewAppServerFactory.TOMCAT, params, "-Dcatalina.jvmroute=" + NODE + nodeNumber);

      params.appendJvmArgs("-DNODE=" + NODE + nodeNumber);
      params.appendJvmArgs("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX + ConfigProperties.REQUEST_BENCHES + "=true");

      params.addWar(warFile());
      AppServerResult r = (AppServerResult) appServer.start(params);

      // only add to this collection if start() actually returned w/o an exception
      appservers.add(appServer);
      return r;
    } catch (Exception e) {
      threadDumpGroup();
      throw e;
    }
  }

  private void addAppServerSpecificJvmArg(String appserverName, StandardAppServerParameters params, String arg) {
    if (appserverName.equals(config.appserverFactoryName())) {
      params.appendJvmArgs(arg);
    }
  }

  private void threadDumpGroup() {
    // this is disabled on mac since something in the process group really dislikes kill -3. Eric says every process on
    // his machine gets killed (terminated) when this goes off
    if (Os.isUnix() && !Os.isMac()) {
      ThreadDump.dumpProcessGroup();
    }
  }

  /**
   * @return URL - the correct URL refering to the provied servlet class for the appserver running on the given port
   */
  public URL createUrl(int port, Class servletClass, String query) throws MalformedURLException {
    if (query != null && query.length() > 0) {
      query = "?" + query;
    }

    String[] parts = servletClass.getName().split("\\.");
    String servletUrl = AbstractDescriptorXml.translateUrl(parts[parts.length - 1]);
    return new URL("http://" + DOMAIN + ":" + port + "/" + testName() + "/" + servletUrl + query);
  }

  public URL createUrl(int port, Class servletClass) throws MalformedURLException {
    return createUrl(port, servletClass, "");
  }

  private synchronized File warFile() throws Exception {
    if (warFile != null) return warFile;
    War war = appServerFactory.createWar(testName());

    // add registered session filters
    for (Iterator it = filterList.iterator(); it.hasNext();) {
      Class filter = (Class) it.next();
      TCServletFilter filterInstance = (TCServletFilter) filter.newInstance();
      war.addFilter(filter, filterInstance.getPattern(), filterInstance.getInitParams());
    }

    // add registered attribute listeners
    for (Iterator it = listenerList.iterator(); it.hasNext();) {
      war.addListener((Class) it.next());
    }

    // add registered servlets
    for (Iterator it = servletList.iterator(); it.hasNext();) {
      war.addServlet((Class) it.next());
    }

    File resourceDir = installation.dataDirectory();
    warFile = new File(resourceDir + File.separator + war.writeWarFileToDirectory(resourceDir));
    return warFile;
  }

  protected final void registerSessionFilter(Class filterClazz) {
    Assert.assertTrue("Class " + filterClazz + " is not a session filter", isFilter(filterClazz));
    filterList.add(filterClazz);
  }

  protected final void registerListener(Class listener) {
    Assert.assertTrue("Class " + listener + " is not any kind of javax.servlet listener", isListener(listener));
    listenerList.add(listener);
  }

  protected final void registerServlet(Class servlet) {
    Assert.assertTrue("Class " + servlet + " is not a servlet", isServlet(servlet));
    servletList.add(servlet);
  }

  private static boolean isServlet(Class clazz) {
    return clazz.getSuperclass().equals(HttpServlet.class) && clazz.getName().toLowerCase().endsWith("servlet")
           && clazz.getName().indexOf("$") == -1;
  }

  private static boolean isFilter(Class clazz) {
    return TCServletFilter.class.isAssignableFrom(clazz);
  }

  private static boolean isListener(Class clazz) {
    return HttpSessionActivationListener.class.isAssignableFrom(clazz)
           || HttpSessionAttributeListener.class.isAssignableFrom(clazz)
           || HttpSessionListener.class.isAssignableFrom(clazz);
  }

  private String testName() {
    return ClassUtils.getShortClassName(getClass());
  }

  private synchronized TerracottaServerConfigGenerator configGen() throws Exception {
    if (configGen != null) { return configGen; }

    if (isSessionTest()) {
      if (isSynchronousWrite) {
        configBuilder.addWebApplication(testName(), isSynchronousWrite);
      } else {
        configBuilder.addWebApplication(testName());
      }
    }

    // add modules that needed for certain app server here
    if (NewAppServerFactory.JETTY.equals(config.appserverFactoryName())) {
      addConfigModule("clustered-jetty-6.1", "1.0.0");
    } else if (NewAppServerFactory.WEBSPHERE.equals(config.appserverFactoryName())) {
      addConfigModule("clustered-websphere-6.1.0.7", "1.0.0");
    }

    configBuilder.addInclude("com.tctest..*");
    configBuilder.addInclude("com.tctest..*$*");

    for (Iterator iter = includes.iterator(); iter.hasNext();) {
      configBuilder.addInclude((String) iter.next());
    }

    for (Iterator iter = roots.iterator(); iter.hasNext();) {
      Root root = (Root) iter.next();
      configBuilder.addRoot(root.fieldName(), root.rootName());
    }

    for (Iterator iter = locks.iterator(); iter.hasNext();) {
      Lock lock = (Lock) iter.next();
      String lockName = null;
      if (!lock.isAutoLock()) {
        lockName = lock.lockName();
      }
      configBuilder.addLock(lock.isAutoLock(), lock.methodExpression(), lock.lockLevel().toString(), lockName);
    }

    return configGen = new TerracottaServerConfigGenerator(installation.dataDirectory(), configBuilder);
  }

  protected boolean isSessionTest() {
    return true;
  }

}
