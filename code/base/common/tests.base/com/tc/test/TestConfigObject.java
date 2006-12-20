/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import org.apache.commons.lang.StringUtils;

import com.tc.config.Directories;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Contains configuration data for tests.
 * </p>
 * <p>
 * This class is a singleton. This is <em>ONLY</em> because this is used all over the place, in JUnit tests.
 */
public class TestConfigObject {

  private static final String     APP_SERVER_WORKING               = "sandbox";

  private static final String     TC_BASE_DIR                      = "tc.base-dir";

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

  private static final String     JVM_VERSION                      = DYNAMIC_PROPERTIES_PREFIX + "jvm.version";
  private static final String     JVM_TYPE                         = DYNAMIC_PROPERTIES_PREFIX + "jvm.type";
  private static final String     JVM_MODE                         = DYNAMIC_PROPERTIES_PREFIX + "jvm.mode";

  private static final String     BOOT_JAR_NORMAL                  = DYNAMIC_PROPERTIES_PREFIX + "bootjars.normal";

  private static final String     SESSION_CLASSPATH                = DYNAMIC_PROPERTIES_PREFIX + "session.classpath";

  private static final String     SHORT_PATH_TEMP_DIR              = DYNAMIC_PROPERTIES_PREFIX + "short-path-tempdir";

  private static final String     AVAILABLE_VARIANTS_PREFIX        = DYNAMIC_PROPERTIES_PREFIX + "variants.available.";
  private static final String     VARIANT_LIBRARIES_PREFIX         = DYNAMIC_PROPERTIES_PREFIX + "libraries.variants.";
  private static final String     SELECTED_VARIANT_PREFIX          = DYNAMIC_PROPERTIES_PREFIX + "variants.selected.";
  private static final String     DEFAULT_VARIANT_PREFIX           = STATIC_PROPERTIES_PREFIX + "variants.selected.";

  private static final String     EXECUTABLE_SEARCH_PATH           = DYNAMIC_PROPERTIES_PREFIX
                                                                     + "executable-search-path";

  private static final String     JUNIT_TEST_TIMEOUT_INSECONDS     = DYNAMIC_PROPERTIES_PREFIX
                                                                     + "junit-test-timeout-inseconds";

  public static final String      APP_SERVER_REPOSITORY_URL_BASE   = STATIC_PROPERTIES_PREFIX + "appserver.repository";

  public static final String      APP_SERVER_HOME                  = DYNAMIC_PROPERTIES_PREFIX + "appserver.home";

  private static final String     APP_SERVER_FACTORY_NAME          = STATIC_PROPERTIES_PREFIX
                                                                     + "appserver.factory.name";

  private static final String     APP_SERVER_MAJOR_VERSION         = STATIC_PROPERTIES_PREFIX
                                                                     + "appserver.major-version";

  private static final String     APP_SERVER_MINOR_VERSION         = STATIC_PROPERTIES_PREFIX
                                                                     + "appserver.minor-version";

  private static final String     TRANSPARENT_TESTS_MODE           = STATIC_PROPERTIES_PREFIX
                                                                     + "transparent-tests.mode";
  private static final String     SPRING_TESTS_TIMEOUT             = STATIC_PROPERTIES_PREFIX + "spring.tests.timeout";

  private static final String     SYSTEM_PROPERTIES_RESOURCE_NAME  = "/test-system-properties.properties";

  private static TestConfigObject INSTANCE;

  private final Properties        properties;

  public static synchronized TestConfigObject getInstance() throws IOException {
    if (INSTANCE == null) {
      INSTANCE = new TestConfigObject();
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

  // this is a hack
  private static void loadEnv() {
    File userDir = new File(System.getProperty("user.dir"));
    String baseDirProp = System.getProperty(TC_BASE_DIR);
    if (baseDirProp == null || baseDirProp.trim().equals("")) invalidBaseDir();
    String[] baseDirParts = baseDirProp.split("/");
    String baseDir = null;
    int count = baseDirParts.length - 1;
    File parent = null;

    while (true) {
      if (userDir.getName().equals(baseDirParts[count])) {
        if (count == baseDirParts.length - 1) baseDir = userDir.getPath();
        if (--count == -1) break;
      }
      if ((parent = userDir.getParentFile()) != null) userDir = parent;
      else break;
    }

    if (baseDir == null || baseDir.trim().equals("")) invalidBaseDir();

    if (StringUtils.isBlank(System.getProperty(Directories.TC_INSTALL_ROOT_PROPERTY_NAME))) {
      System.setProperty(Directories.TC_INSTALL_ROOT_PROPERTY_NAME, baseDir);
      System.setProperty(Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME, "true");
    }
    System.setProperty(Directories.TC_LICENSE_LOCATION_PROPERTY_NAME, baseDir);
  }

  private static void invalidBaseDir() {
    throw new RuntimeException("System Property: " + TC_BASE_DIR + " is not valid.\n" + System.getProperty(TC_BASE_DIR));
  }

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

    if (System.getProperty(PROPERTY_FILE_LIST_PROPERTY_NAME) == null) { throw new IOException(
                                                                                              "You must set the system property '"
                                                                                                  + PROPERTY_FILE_LIST_PROPERTY_NAME
                                                                                                  + "' to point to the appropriate test properties file for this set of tests. If you're running "
                                                                                                  + "from Eclipse, you can do this by running 'ant test.setup', which will generate a properties "
                                                                                                  + "file in build/<module>/tests.<type>.configuration, and then setting the above system "
                                                                                                  + "property to point to it."); }

    String[] components = System.getProperty(PROPERTY_FILE_LIST_PROPERTY_NAME).split(File.pathSeparator);

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

    this.properties.putAll(System.getProperties());

    logger.info("Loaded test configuration from " + loadedFrom.toString());
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

  public String jvmVersion() {
    String out = this.properties.getProperty(JVM_VERSION);
    Assert.assertNotBlank(out);
    return out;
  }

  public String jvmType() {
    String out = this.properties.getProperty(JVM_TYPE);
    Assert.assertNotBlank(out);
    return out;
  }

  public String jvmMode() {
    String out = this.properties.getProperty(JVM_MODE);
    Assert.assertNotBlank(out);
    return out;
  }

  public String osName() {
    return this.properties.getProperty(OS_NAME);
  }

  public String dataDirectoryRoot() {
    String out = this.properties.getProperty(DATA_DIRECTORY_ROOT);
    Assert.assertNotBlank(out);
    return out;
  }

  public String tempDirectoryRoot() {
    String out = this.properties.getProperty(TEMP_DIRECTORY_ROOT);
    Assert.assertNotBlank(out);
    return out;
  }

  public String appserverURLBase() {
    return this.properties.getProperty(APP_SERVER_REPOSITORY_URL_BASE);
  }

  public String appserverHome() {
    return this.properties.getProperty(APP_SERVER_HOME);
  }

  public String appserverFactoryName() {
    String out = this.properties.getProperty(APP_SERVER_FACTORY_NAME);
    Assert.assertNotBlank(out);
    return out;
  }

  public String appserverMajorVersion() {
    String out = this.properties.getProperty(APP_SERVER_MAJOR_VERSION);
    Assert.assertNotBlank(out);
    return out;
  }

  public String appserverMinorVersion() {
    String out = this.properties.getProperty(APP_SERVER_MINOR_VERSION);
    Assert.assertNotBlank(out);
    return out;
  }

  public String springTestsTimeout() {
    return this.properties.getProperty(SPRING_TESTS_TIMEOUT);
  }

  private String shortPathNameTempDirectory() {
    return this.properties.getProperty(SHORT_PATH_TEMP_DIR);
  }

  public String executableSearchPath() {
    return this.properties.getProperty(EXECUTABLE_SEARCH_PATH);
  }

  private File effectiveShortPathNameTempDirectory() {
    if (shortPathNameTempDirectory() != null) {
      return new File(shortPathNameTempDirectory());
    } else {
      return new File(tempDirectoryRoot(), "short-names");
    }
  }

  public String appserverServerInstallDir() {
    return new File(effectiveShortPathNameTempDirectory(), "app-server-install").getAbsolutePath();
  }

  public String appserverWorkingDir() {
    return new File(effectiveShortPathNameTempDirectory(), APP_SERVER_WORKING).getAbsolutePath();
  }

  public String normalBootJar() {
    String out = this.properties.getProperty(BOOT_JAR_NORMAL);
    Assert.assertNotBlank(out);
    assertFileExists(out);
    return out;
  }

  public String linkedChildProcessClasspath() {
    String out = this.properties.getProperty(LINKED_CHILD_PROCESS_CLASSPATH);
    Assert.assertNotBlank(out);
    assertValidClasspath(out);
    return out;
  }

  public String sessionClasspath() {
    String out = this.properties.getProperty(SESSION_CLASSPATH);
    Assert.assertNotBlank(out);
    assertValidClasspath(out);
    return out;
  }

  public int getJunitTimeoutInSeconds() {
    String seconds = this.properties.getProperty(JUNIT_TEST_TIMEOUT_INSECONDS);
    Assert.assertNotBlank(seconds);
    return Integer.parseInt(seconds);
  }

  public static final String    TRANSPARENT_TESTS_MODE_NORMAL  = "normal";
  public static final String    TRANSPARENT_TESTS_MODE_RESTART = "restart";
  public static final String    TRANSPARENT_TESTS_MODE_CRASH   = "crash";

  private static final String[] ALL_TRANSPARENT_TESTS_MODES    = { TRANSPARENT_TESTS_MODE_NORMAL,
      TRANSPARENT_TESTS_MODE_RESTART, TRANSPARENT_TESTS_MODE_CRASH };

  public String transparentTestsMode() {
    String out = this.properties.getProperty(TRANSPARENT_TESTS_MODE);
    Assert.assertNotBlank(out);

    boolean foundIt = false;
    for (int i = 0; i < ALL_TRANSPARENT_TESTS_MODES.length; ++i) {
      foundIt = foundIt || ALL_TRANSPARENT_TESTS_MODES[i].equals(out);
    }

    Assert.eval(foundIt);

    return out;
  }

  private void assertValidClasspath(String out) {
    String[] pathElements = out.split(File.pathSeparator);
    for (int i = 0; i < pathElements.length; i++) {
      String pathElement = pathElements[i];
      Assert.assertTrue("Path element is non-existent: " + pathElement, new File(pathElement).exists());

    }
  }

  private void assertFileExists(String out) {
    File file = new File(out);
    Assert.assertTrue("not a file: " + out, file.isFile());
  }
}
