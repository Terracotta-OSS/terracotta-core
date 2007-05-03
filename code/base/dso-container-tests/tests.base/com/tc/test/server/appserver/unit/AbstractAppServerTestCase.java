/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.unit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.schema.Lock;
import com.tc.object.config.schema.Root;
import com.tc.process.HeartBeatService;
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
import com.tc.test.server.util.HttpUtil;
import com.tc.test.server.util.VmStat;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.ThreadDump;
import com.terracotta.session.util.ConfigProperties;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

/**
 * Please read this doc in it's entirety before attempting to utilize the Terracotta Appserver Testing Framework and
 * it's constituent components. The comments below describe the abstract test case itself and explain how it is intended
 * to be extended, debugged, and automated.
 * <p>
 * This class serves as a layer of indirection between unit tests and the terracotta framework for server installation
 * and invocation. The initialization and execution process is as follows:
 * <p>
 * A factory is used to create supporting appserver classes for a given platform. A list of servers is kept for use by
 * the <tt>tearDown()</tt> method. It is generally advised that <tt>startDsoServer()</tt> be called first followed
 * by n calls to <tt>startAppServer()</tt> before {@link HttpUtil} is used to page the running servers. This practice
 * may vary for certain tests that may be expected to fail or timeout. The method <tt>createUrl()</tt> should be
 * called to obtain a reference to a servlet running in a container. The server port is provided as a field of
 * {@link AppServerResult} which should be captured and passed to <tt>createUrl()</tt> for that server.
 * <p>
 * The framework uses reflection to locate <tt>public static</tt> inner classes that extend {@link HttpServlet} and
 * end with "servlet" (not case sensitive). These servlets are automatically deployed in a WAR (web application
 * resource) with the correct servlet mappings and appserver specific descriptors; which are referenced using the
 * <tt>createUrl()</tt> method. The tc-config.xml file is also auto generated and deployed to each server running dso.
 * <p>
 * As a recommended coding practice the inner class servlets should handle the majority of the testing infrastructure;
 * where possible returning a simple boolean "true" or "false". A mechanism is provided to share properties between the
 * outer class (unit test) and it's associated servlets in the form of an overloaded <tt>startAppServer()</tt> method
 * that takes a {@link Properties} file. These properties are available to servlets in that container as system
 * properties. By default every servlet under this convention is provided a system property for the instance name and
 * http port. See {@link AppServerConstants} for exact property names.
 * <p>
 * The coding guidelines for writting unit tests that interact with inner class servlets are as follows:
 * <ul>
 * <li>Servlets are packaged and deployed to the appserver's JVM therefore they know nothing about the outer class or
 * the unit test which is being run.
 * <li>A servlet should not utilize any imports unless the imported files are also included in the associated WAR as
 * libraries.
 * <li>The servlet should not reference any parent class methods (the outer-class superclass is not even deployed to
 * the appserver)
 * </ul>
 * <p>
 * 
 * <pre>
 *                            outer class:
 *                            ...
 *                            int port0 = startAppServer(false).serverPort();
 *                            boolean[] values = HttpUtil.getBooleanValues(createUrl(port0, SimpleDsoSessionsTest.DsoPingPongServlet.class));
 *                            assertTrue(values[0]);
 *                            assertFalse(values[1]);
 *                            ...
 *                            inner class servlet:
 *                            ...
 *                            response.setContentType(&quot;text/html&quot;);
 *                            PrintWriter out = response.getWriter();
 *                            out.println(&quot;true&quot;);
 *                            out.println(&quot;false&quot;);
 *                            ...
 * </pre>
 * 
 * <p>
 * <h3>Debugging Information:</h3>
 * There are a number of locations and files to consider when debugging appserver unit tests. Below is a list followed
 * by a more formal description.
 * <ul>
 * <li>The console - Provides output for the unit test parent process which includes the DSO server (in red) and the
 * output known to the parent about the child process appserver startup.
 * <li>appserver-name.log - located in the sandbox directory of the working instance location which includes all server
 * output for out and err.
 * <li>data directory - contains the WAR and tc-config.
 * </ul>
 * In general if the console does not sufficiently describe a failure you should open the working instance directory
 * (see comments below for more info) to confirm that the .log was created for a particular instance. The .log is
 * created before the appserver is started in the child JVM process. If that does not exist there is something wrong;
 * probably in the classpath that starts the appserver. If the log does exist it will probably describe the failure or
 * exception. If no exception is provided try wrapping your servlet content with a try catch block to the extent of
 * e.printStackTrace().
 * <p>
 * <h3>Working Instance Directory</h3>
 * This directory contains the subdirectories of the sandbox and data. The data directory contains files shared by all
 * instances of an appserver but is specific to a unit test. These files may include the tc-config.xml file,
 * testname.war and DSO logs etc. The sandbox directory contains the actual running server instances. Each server is
 * given a name. By default the convention "node-i" is used. In this location you will find a folder (node-i), a log
 * (node-i.log), and a properties file (node-i.properties) for each allocated server instance. By default the working
 * instance is created in the unit test temp directory referred to by {@link TCTestCase.getTempDirectory()} (usually
 * under build/test/fullclassname). When the monkeys complete a batch run of the unit tests they take the contents of
 * the temp directory and package them into a jar so that the data is available for review. There is an option to
 * override the working instance directory in the appserver properties file (generated by the build process). If this
 * feature is exercised the working instance will run in the specified location. Once complete for that test the
 * contents will be moved to their proper location in the temp folder. You will only need to override the working
 * instance location if the path becomes too long on windows systems.
 * <p>
 * As a final note: the <tt>UttpUtil</tt> class should be used (and added to as needed) to page servlets and validate
 * assertions.
 * 
 * @author eellis
 */
public abstract class AbstractAppServerTestCase extends TCTestCase {

  private static final SynchronizedInt    nodeCounter        = new SynchronizedInt(-1);
  private static final String             NODE               = "node-";
  private static final String             DOMAIN             = "localhost";

  protected final List                    appservers         = new ArrayList();
  private final Object                    workingDirLock     = new Object();
  private final List                      dsoServerJvmArgs   = new ArrayList();
  private final List                      roots              = new ArrayList();
  private final List                      locks              = new ArrayList();
  private final List                      includes           = new ArrayList();
  private final TestConfigObject          config;

  private File                            serverInstallDir;
  private File                            workingDir;
  private File                            tempDir;
  private File                            bootJar;
  private NewAppServerFactory             appServerFactory;
  private AppServerInstallation           installation;
  private File                            warFile;
  private DsoServer                       dsoServer;
  private TerracottaServerConfigGenerator configGen;

  private boolean                         isSynchronousWrite = false;

  public AbstractAppServerTestCase() {
    // keep the regular thread dump behavior for windows and macs
    setDumpThreadsOnTimeout(Os.isWindows() || Os.isMac());

    try {
      config = TestConfigObject.getInstance();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String appserver = config.appserverFactoryName();
    // XXX: Only non-session container tests work in glassfish and jetty at the moment
    if (isSessionTest()
        && (NewAppServerFactory.GLASSFISH.equals(appserver) || NewAppServerFactory.JETTY.equals(appserver))) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  protected int getJMXPort() {
    if (configGen == null) { throw new AssertionError(
                                                      "DSO server is not running so JMX port has not been assigned yet."); }
    return configGen.getConfig().getJmxPort();
  }

  protected void setUp() throws Exception {
    tempDir = getTempDirectory();
    serverInstallDir = makeDir(config.appserverServerInstallDir());
    File workDir = new File(config.appserverWorkingDir());

    try {
      if (workDir.exists()) {
        if (workDir.isDirectory()) {
          FileUtils.cleanDirectory(workDir);
        } else {
          throw new RuntimeException(workDir + " exists, but is not a directory");
        }
      }
    } catch (IOException e) {
      File prev = workDir;
      workDir = new File(config.appserverWorkingDir() + "-"
                         + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
      Banner.warnBanner("Caught IOException setting up workDir as " + prev + ", using " + workDir + " instead");
    }

    workingDir = makeDir(workDir.getAbsolutePath());
    bootJar = new File(config.normalBootJar());
    appServerFactory = NewAppServerFactory.createFactoryFromProperties(config);

    String appserverURLBase = config.appserverURLBase();
    String appserverHome = config.appserverHome();

    if (appserverHome != null && !appserverHome.trim().equals("")) {
      File home = new File(appserverHome);
      installation = appServerFactory.createInstallation(home, workingDir);

    } else if (appserverURLBase != null && !appserverURLBase.trim().equals("")) {
      URL host = new URL(appserverURLBase);
      installation = appServerFactory.createInstallation(host, serverInstallDir, workingDir);

    } else {
      throw new AssertionError(
                               "No container installation available. You must define one of the following config properties:\n"
                                   + TestConfigObject.APP_SERVER_HOME + "\nor\n"
                                   + TestConfigObject.APP_SERVER_REPOSITORY_URL_BASE);
    }
  }

  protected final boolean cleanTempDir() {
    return false;
  }

  protected void beforeTimeout() throws Throwable {
    threadDumpGroup();

    // make an archive of the workingDir since it will not be renamed when test times out
    archiveSandboxLogs();
  }

  private void archiveSandboxLogs() {
    synchronized (workingDirLock) {
      if (installation != null) {
        String src = installation.sandboxDirectory().getParentFile().getAbsolutePath();
        String dest = new File(tempDir, "archive-logs-" + System.currentTimeMillis() + ".zip").getAbsolutePath();

        String msg = "\n";
        msg += "*****************************\n";
        msg += "* Archiving logs in [" + src + "] to " + dest + "\n";
        msg += "*****************************\n";
        System.out.println(msg);

        Zip zip = new Zip();
        zip.setProject(new Project());
        zip.setDestFile(new File(dest));
        zip.setBasedir(new File(src));
        zip.setIncludes("**/*.log");
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
  protected final void addRoots(List rootsToAdd) {
    roots.addAll(rootsToAdd);
  }

  /*
   * This method should be called before DSO server is started.
   */
  protected final void addLocks(List locksToAdd) {
    locks.addAll(locksToAdd);
  }

  protected final void addInclude(String expression) {
    includes.add(expression);
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

      params.appendJvmArgs("-DNODE=" + NODE + nodeNumber);

      // params.appendJvmArgs("-Dtc.classloader.writeToDisk=true");

      params.appendJvmArgs("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX + "." + ConfigProperties.REQUEST_BENCHES
                           + "=true");

      params.appendJvmArgs("-verbose:gc");
      params.appendJvmArgs("-Xloggc:" + new File(this.workingDir, "node-" + nodeNumber + ".gc.log"));
      params.appendJvmArgs("-XX:+PrintGCDetails");

      if (false && nodeNumber == 0) {
        int debugPort = 8000 + nodeNumber;
        System.out.println("Waiting for debugger connection on port " + debugPort);
        params.appendJvmArgs("-Xdebug");
        params.appendJvmArgs("-Xrunjdwp:server=y,transport=dt_socket,address=" + debugPort + ",suspend=y");
      }

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

  private boolean awaitShutdown(int timewait) throws Exception {
    long start = System.currentTimeMillis();
    boolean foundAlive = false;
    do {
      Thread.sleep(1000);
      foundAlive = HeartBeatService.anyAppServerAlive();
    } while (foundAlive && System.currentTimeMillis() - start < timewait);

    return foundAlive;
  }

  /**
   * If overridden <tt>super.tearDown()</tt> must be called to ensure that servers are all shutdown properly
   * 
   * @throws Exception
   */
  protected void tearDown() throws Exception {
    try {
      System.out.println("in tearDown...");
      for (Iterator iter = appservers.iterator(); iter.hasNext();) {
        Server server = (Server) iter.next();
        server.stop();
      }
      awaitShutdown(10 * 1000);
      if (dsoServer != null && dsoServer.isRunning()) dsoServer.stop();
      System.out.println("Send kill signal to app servers...");
      HeartBeatService.sendKillSignalToChildren();
    } finally {
      VmStat.stop();
      synchronized (workingDirLock) {
        File dest = new File(tempDir, getName());
        System.err.println("Copying files from " + workingDir + " to " + dest);
        try {
          com.tc.util.io.FileUtils.copyFile(workingDir, dest);
        } catch (IOException ioe) {
          Banner.warnBanner("IOException caught while copying workingDir files");
          ioe.printStackTrace();
        }

        System.err.println("Deleting working directory files in " + workingDir);
        try {
          FileUtils.forceDelete(workingDir);
        } catch (IOException ioe) {
          Banner.warnBanner("IOException caught while deleting workingDir");
          // print this out, but don't fail test by re-throwing it
          ioe.printStackTrace();
        }
      }
    }
  }

  protected final void collectVmStats() throws IOException {
    VmStat.start(workingDir);
  }

  private synchronized File warFile() throws Exception {
    if (warFile != null) return warFile;
    War war = appServerFactory.createWar(testName());
    addServletsWebAppClasses(war);
    File resourceDir = installation.dataDirectory();
    warFile = new File(resourceDir + File.separator + war.writeWarFileToDirectory(resourceDir));
    return warFile;
  }

  private void addServletsWebAppClasses(War war) throws InstantiationException, IllegalAccessException {
    Class[] classes = getClass().getClasses();
    for (int i = 0; i < classes.length; i++) {
      Class clazz = classes[i];
      if (!isSafeClass(clazz)) {
        continue;
      }
      if (isServlet(clazz)) {
        war.addServlet(clazz);
      }
      if (isListener(clazz)) {
        war.addListener(clazz);
      }
      if (isFilter(clazz)) {
        TCServletFilter filterInstance = (TCServletFilter) clazz.newInstance();
        war.addFilter(clazz, filterInstance.getPattern(), filterInstance.getInitParams());
      }
      // it's just a class, add it
      war.addClass(clazz);
    }
  }

  private static boolean isSafeClass(Class clazz) {
    int mod = clazz.getModifiers();
    return Modifier.isStatic(mod) && Modifier.isPublic(mod) && !Modifier.isInterface(mod) && !Modifier.isAbstract(mod);
  }

  private static boolean isServlet(Class clazz) {
    return clazz.getSuperclass().equals(HttpServlet.class) && clazz.getName().toLowerCase().endsWith("servlet");
  }

  private static boolean isFilter(Class clazz) {
    return TCServletFilter.class.isAssignableFrom(clazz);
  }

  private static boolean isListener(Class clazz) {
    return HttpSessionActivationListener.class.isAssignableFrom(clazz)
           || HttpSessionAttributeListener.class.isAssignableFrom(clazz)
           || HttpSessionListener.class.isAssignableFrom(clazz);
  }

  private File makeDir(String dirPath) throws IOException {
    File dir = new File(dirPath);
    if (dir.exists()) {
      if (dir.isDirectory()) { return dir; }
      throw new IOException(dir + " exists, but is not a directory");
    }
    boolean created = dir.mkdirs();
    if (!created) { throw new IOException("Could not create directory " + dir); }
    return dir;
  }

  private String testName() {
    return ClassUtils.getShortClassName(getClass());
  }

  private synchronized TerracottaServerConfigGenerator configGen() throws Exception {
    if (configGen != null) { return configGen; }
    StandardTerracottaAppServerConfig configBuilder = appServerFactory.createTcConfig(installation.dataDirectory());

    if (isSessionTest()) {
      if (isSynchronousWrite) {
        configBuilder.addWebApplication(testName(), isSynchronousWrite);
      } else {
        configBuilder.addWebApplication(testName());
      }
    }
    
    if (NewAppServerFactory.JETTY.equals(config.appserverFactoryName())) {
      configBuilder.addModule("clustered-jetty", "6.1");
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
