/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import org.apache.commons.lang.StringUtils;
import org.terracotta.NativeToolHandler;

import com.tc.config.Directories;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Contains configuration data for tests. </p>
 * <p>
 * This class is a singleton. This is <em>ONLY</em> because this is used all over the place, in JUnit tests.
 */
public class TestConfigObject {
  public static final String      NATIVE_LIB_LINUX_32              = "Linux";

  public static final String      NATIVE_LIB_LINUX_64              = "Linux64";

  public static final String      UNIX_NATIVE_LIB_NAME             = "libGetPid.so";

  public static final String      WINDOWS_NATIVE_LIB_NAME          = "GetPid.dll";

  public static final String      OSX_NATIVE_LIB_NAME              = "libGetPid.jnilib";

  public static final String      TC_BASE_DIR                      = "tc.base-dir";

  public static final String      SPRING_VARIANT                   = "spring";

  public static final String      WEBFLOW_VARIANT                  = "spring-webflow";

  private static final TCLogger   logger                           = TCLogging.getLogger(TestConfigObject.class);

  private static final String     OS_NAME                          = "os.name";

  private static final String     DYNAMIC_PROPERTIES_PREFIX        = "tc.tests.info.";

  private static final String     STATIC_PROPERTIES_PREFIX         = "tc.tests.configuration.";

  public static final String      PROPERTY_FILE_LIST_PROPERTY_NAME = DYNAMIC_PROPERTIES_PREFIX + "property-files";

  private static final String     TEMP_DIRECTORY_ROOT              = DYNAMIC_PROPERTIES_PREFIX + "temp-root";

  private static final String     DATA_DIRECTORY_ROOT              = DYNAMIC_PROPERTIES_PREFIX + "data-root";

  private static final String     LINKED_CHILD_PROCESS_CLASSPATH   = DYNAMIC_PROPERTIES_PREFIX
                                                                     + "linked-child-process-classpath";

  private static final String     AVAILABLE_VARIANTS_PREFIX        = DYNAMIC_PROPERTIES_PREFIX + "variants.available.";
  private static final String     VARIANT_LIBRARIES_PREFIX         = DYNAMIC_PROPERTIES_PREFIX + "libraries.variants.";
  private static final String     SELECTED_VARIANT_PREFIX          = DYNAMIC_PROPERTIES_PREFIX + "variants.selected.";
  private static final String     DEFAULT_VARIANT_PREFIX           = STATIC_PROPERTIES_PREFIX + "variants.selected.";

  private static final String     JUNIT_TEST_TIMEOUT_INSECONDS     = DYNAMIC_PROPERTIES_PREFIX
                                                                     + "junit-test-timeout-inseconds";

  public static final String      APP_SERVER_HOME                  = STATIC_PROPERTIES_PREFIX + "appserver.home";

  private static final String     APP_SERVER_FACTORY_NAME          = STATIC_PROPERTIES_PREFIX
                                                                     + "appserver.factory.name";

  private static final String     APP_SERVER_MAJOR_VERSION         = STATIC_PROPERTIES_PREFIX
                                                                     + "appserver.major-version";

  private static final String     APP_SERVER_MINOR_VERSION         = STATIC_PROPERTIES_PREFIX
                                                                     + "appserver.minor-version";

  private static final String     APP_SERVER_SPECIFICATION         = STATIC_PROPERTIES_PREFIX + "appserver";

  private static final String     SYSTEM_PROPERTIES_RESOURCE_NAME  = "/test-system-properties.properties";

  private static final String     L2_STARTUP_PREFIX                = DYNAMIC_PROPERTIES_PREFIX + "l2.startup.";
  public static final String      L2_STARTUP_MODE                  = L2_STARTUP_PREFIX + "mode";
  public static final String      L2_STARTUP_JAVA_HOME             = L2_STARTUP_PREFIX + "jvm";

  private static final String     EMMA_LIB                         = DYNAMIC_PROPERTIES_PREFIX + "emma.lib";
  private static final String     JAVA_HOME_15                     = DYNAMIC_PROPERTIES_PREFIX + "JAVA_HOME_15";
  private static final String     JAVA_HOME_16                     = DYNAMIC_PROPERTIES_PREFIX + "JAVA_HOME_16";

  private static TestConfigObject INSTANCE;

  private final Properties        properties;
  private final AppServerInfo     appServerInfo;
  private String                  extraClassPathForAppServer;
  private boolean                 springTest                       = false;

  private TestConfigObject() throws IOException {
    this.properties = new Properties();
    StringBuffer loadedFrom = new StringBuffer();

    loadEnv();
    loadSystemProperties();

    int filesRead = 0;

    // *DO NOT* hardcode system properties in here just to make tests easier
    // to run in Eclipse.
    // Doing so makes it a *great* deal harder to modify the build system.
    // All you need to do
    // to run tests in Eclipse is to run 'tcbuild check_prep <module-name>
    // <test-type>' before
    // you run your tests.
    //
    // If these things are too hard for you to do, please come talk to the
    // build team. Hardcoding
    // properties here can make our lives very difficult.

    String[] components = {};
    if (System.getProperty(PROPERTY_FILE_LIST_PROPERTY_NAME) != null) {
      components = System.getProperty(PROPERTY_FILE_LIST_PROPERTY_NAME).split(File.pathSeparator);
    }

    for (int i = components.length - 1; i >= 0; --i) {
      File thisFile = new File(components[i]);
      if (thisFile.exists()) {
        Properties theseProperties = new Properties();
        theseProperties.load(new FileInputStream(thisFile));
        this.properties.putAll(theseProperties);
        if (filesRead > 0) loadedFrom.append(", ");
        loadedFrom.append("'" + thisFile.getAbsolutePath() + "'");
        ++filesRead;
      }
    }

    if (filesRead > 0) loadedFrom.append(", ");
    loadedFrom.append("system properties");

    properties.putAll(System.getProperties());
    appServerInfo = createAppServerInfo();
    extraClassPathForAppServer = linkedChildProcessPath();

    // if Emma is enabled, add it to app server classpath
    String emmaLib = properties.getProperty(EMMA_LIB);
    if (emmaLib != null) {
      extraClassPathForAppServer += File.pathSeparator + emmaLib;
    }

    logger.info("Loaded test configuration from " + loadedFrom.toString());
  }

  private AppServerInfo createAppServerInfo() {
    if (properties.containsKey(APP_SERVER_SPECIFICATION)) { return AppServerInfo.parse(properties
        .getProperty(APP_SERVER_SPECIFICATION)); }

    return new AppServerInfo(properties.getProperty(APP_SERVER_FACTORY_NAME, "unknown"),
                             properties.getProperty(APP_SERVER_MAJOR_VERSION, "unknown"),
                             properties.getProperty(APP_SERVER_MINOR_VERSION, "unknown"));
  }

  public static synchronized TestConfigObject getInstance() {
    if (INSTANCE == null) {
      try {
        INSTANCE = new TestConfigObject();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return INSTANCE;
  }

  private static void loadSystemProperties() throws IOException {
    InputStream in = null;

    if (System.getProperty(PROPERTY_FILE_LIST_PROPERTY_NAME) == null) {
      in = TestConfigObject.class.getResourceAsStream(SYSTEM_PROPERTIES_RESOURCE_NAME);
      if (in != null) {
        try {
          Properties systemProperties = new Properties();
          systemProperties.load(in);
          Iterator iter = systemProperties.entrySet().iterator();

          while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            System.setProperty((String) entry.getKey(), (String) entry.getValue());
          }

          logger.info("Set " + systemProperties.size() + " system properties from resource '"
                      + SYSTEM_PROPERTIES_RESOURCE_NAME + "'.");
        } finally {
          in.close();
        }
      }
    }
  }

  private static void loadEnv() {
    initBaseDir();

    if (!StringUtils.isBlank(System.getProperty(Directories.TC_INSTALL_ROOT_PROPERTY_NAME))) { throw new RuntimeException(
                                                                                                                          "Don't set '"
                                                                                                                              + Directories.TC_INSTALL_ROOT_PROPERTY_NAME
                                                                                                                              + "' in tests."); }
    System.setProperty(Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME, "true");
  }

  private static void initBaseDir() {
    String baseDirProp = System.getProperty(TC_BASE_DIR);
    if (baseDirProp == null || baseDirProp.trim().equals("")) invalidBaseDir();
    baseDir = new File(baseDirProp);
    if (!baseDir.isDirectory()) invalidBaseDir();
  }

  private static void invalidBaseDir() {
    baseDir = null;
    String value = System.getProperty(TC_BASE_DIR);
    StringBuffer buf = new StringBuffer();
    buf.append("The value of the system property " + TC_BASE_DIR + " is not valid.");
    buf.append(" The value is: \"").append(value).append("\"");
    throw new RuntimeException(buf.toString());
  }

  public String getProperty(String key, String defaultValue) {
    String result = this.properties.getProperty(key);
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }

  public String getProperty(String key) {
    return getProperty(key, null);
  }

  public String getL2StartupMode() {
    return this.properties.getProperty(L2_STARTUP_MODE);
  }

  public boolean isL2StartupModeExternal() {
    return "external".equalsIgnoreCase(getL2StartupMode());
  }

  public String getL2StartupJavaHome() {
    String result = this.properties.getProperty(L2_STARTUP_JAVA_HOME);
    if (result == null && Vm.isJDK15Compliant()) {
      result = System.getProperty("java.home");
    }
    return result;
  }

  public String[] availableVariantsFor(String variantName) {
    String out = this.properties.getProperty(AVAILABLE_VARIANTS_PREFIX + variantName);
    if (StringUtils.isBlank(out)) return new String[0];
    return out.split(",");
  }

  public String variantLibraryClasspathFor(String variantName, String variantValue) {
    return this.properties.getProperty(VARIANT_LIBRARIES_PREFIX + variantName + "." + variantValue, "");
  }

  public String selectedVariantFor(String variantName) {
    String selected = this.properties.getProperty(SELECTED_VARIANT_PREFIX + variantName);
    if (null == selected) {
      selected = this.properties.getProperty(DEFAULT_VARIANT_PREFIX + variantName);
    }

    return selected;
  }

  /**
   * Returns the version string for the current JVM. Equivalent to
   * <code>System.getProperty("java.runtime.version")</code>.
   */
  public String jvmVersion() {
    return System.getProperty("java.runtime.version");
  }

  /**
   * Returns the type of the current JVM. Equivalent to <code>System.getProperty("java.vm.name")</code>.
   */
  public String jvmName() {
    return System.getProperty("java.vm.name");
  }

  public String javaHome15() {
    return getProperty(JAVA_HOME_15);
  }

  public String javaHome16() {
    return getProperty(JAVA_HOME_16);
  }

  public String osName() {
    return getProperty(OS_NAME);
  }

  public String platform() {
    String osname = osName();
    if (osname.startsWith("Windows")) {
      return "windows";
    } else if (osname.startsWith("Linux")) {
      return "linux";
    } else if (osname.startsWith("SunOS")) {
      return "solaris";
    } else return osname;
  }

  public String nativeLibName() {
    String osname = osName();
    if (osname.startsWith("Windows")) {
      return WINDOWS_NATIVE_LIB_NAME;
    } else if (osname.startsWith("Darwin") || osname.startsWith("Mac")) {
      return OSX_NATIVE_LIB_NAME;
    } else {
      return UNIX_NATIVE_LIB_NAME;
    }
  }

  public String dataDirectoryRoot() {
    return getProperty(DATA_DIRECTORY_ROOT, new File(baseDir, "test-data").getAbsolutePath());
  }

  public String tempDirectoryRoot() {
    return getProperty(TEMP_DIRECTORY_ROOT, new File(baseDir, "temp").getAbsolutePath());
  }

  public String appserverHome() {
    return this.properties.getProperty(APP_SERVER_HOME);
  }

  public AppServerInfo appServerInfo() {
    return appServerInfo;
  }

  public int appServerId() {
    return appServerInfo.getId();
  }

  public String executableSearchPath() {
    NativeToolHandler toolHandler = new NativeToolHandler();

    String nativeLibDirPath = toolHandler.getToolLocation().getAbsolutePath();
    if (nativeLibDirPath == null) return null;

    if (nativeLibDirPath.endsWith(NATIVE_LIB_LINUX_32) || nativeLibDirPath.endsWith(NATIVE_LIB_LINUX_64)) {
      int lastSeparator = nativeLibDirPath.lastIndexOf(File.separator);
      String vmType = System.getProperty("sun.arch.data.model");
      if (vmType.equals("32")) {
        nativeLibDirPath = nativeLibDirPath.substring(0, lastSeparator) + File.separator + NATIVE_LIB_LINUX_32;
      } else if (vmType.equals("64")) {
        nativeLibDirPath = nativeLibDirPath.substring(0, lastSeparator) + File.separator + NATIVE_LIB_LINUX_64;
      }
    }
    return nativeLibDirPath;
  }

  public File cacheDir() {
    String root = System.getProperty("user.home");
    if (Os.isWindows()) {
      File temp = new File("c:/temp");
      if (!temp.exists()) {
        temp = new File(root.substring(0, 2) + "/temp");
      }
      return temp;
    }
    return new File(root, ".tc");
  }

  public File appserverServerInstallDir() {
    File installDir = new File(cacheDir(), "appservers");
    if (!installDir.exists()) installDir.mkdirs();
    return installDir;
  }

  public String extraClassPathForAppServer() {
    return extraClassPathForAppServer;
  }

  public void addToAppServerClassPath(String cp) {
    extraClassPathForAppServer += File.pathSeparator + cp;
  }

  public String linkedChildProcessPath() {
    String out = this.properties.getProperty(LINKED_CHILD_PROCESS_CLASSPATH);
    Assert.assertNotBlank(out);
    assertValidClasspath(out);
    return out;
  }

  public int getJunitTimeoutInSeconds() {
    return Integer.parseInt(getProperty(JUNIT_TEST_TIMEOUT_INSECONDS, "900"));
  }

  private static File baseDir;

  private void assertValidClasspath(String out) {
    String[] pathElements = out.split(File.pathSeparator);
    for (String pathElement : pathElements) {
      Assert.assertTrue("Path element is non-existent: " + pathElement, new File(pathElement).exists());

    }
  }

  public boolean isSpringTest() {
    return springTest;
  }

  public void setSpringTest(boolean springTest) {
    this.springTest = springTest;
  }
}
