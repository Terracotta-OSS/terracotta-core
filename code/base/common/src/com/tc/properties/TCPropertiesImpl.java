/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

import com.tc.config.TcProperty;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * This class is an easy way to read properties that will help tune DSO. It first loads properties from the
 * tc.properties file in the same package as this file and these properties can be overloaded by tc.properites in the
 * base directory where tc.jar is present. TODO:: Improve tcbuild to aggregate properties from different directories
 * during build time.
 */
/**
 *
 */
public class TCPropertiesImpl implements TCProperties {

  private static final TCLogger         logger                     = newLoggingProxy();

  public static final String            SYSTEM_PROP_PREFIX         = "com.tc.";

  // This file resides in src.resource/com/tc/properties directory
  private static final String           DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  // This file,if present, overrides the default properties and resides in the same directory as tc.jar
  private static final String           TC_PROPERTIES_FILE         = "tc.properties";

  // This is the system property that can be set to point to a tc.properties file
  private static final String           TC_PROPERTIES_SYSTEM_PROP  = "com.tc.properties";

  private static final TCPropertiesImpl INSTANCE;

  private final Properties              props                      = new Properties();

  private final Properties              localTcProperties          = new Properties();

  private volatile boolean              initialized                = false;

  static {
    INSTANCE = new TCPropertiesImpl();
  }

  private TCPropertiesImpl() {
    super();

    loadDefaults(DEFAULT_TC_PROPERTIES_FILE);
    String tcJarDir = getTCJarRootDirectory();
    if (tcJarDir != null) {
      loadOverrides(tcJarDir, TC_PROPERTIES_FILE);
    }
    String tcPropFile = System.getProperty(TC_PROPERTIES_SYSTEM_PROP);
    if (tcPropFile != null) {
      loadOverrides(tcPropFile);
    }

    // this happens last -- system properties have highest precedence
    processSystemProperties();

    trimWhiteSpace();

    warnForOldProperties();
  }

  public static String tcSysProp(final String prop) {
    return SYSTEM_PROP_PREFIX + prop;
  }

  private void warnForOldProperties() {
    String[] oldProperties = TCPropertiesConsts.OLD_PROPERTIES;
    int len = oldProperties.length;
    for (int i = 0; i < len; i++) {
      if (props.containsKey(oldProperties[i])) {
        logger
            .warn("The property \""
                  + oldProperties[i]
                  + "\" has been removed/renamed in the latest release. Please update the tc.properties file or some of your settings might not work");
      }
    }
  }

  private void trimWhiteSpace() {
    for (Iterator i = props.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      entry.setValue(((String) entry.getValue()).trim());
    }
  }

  private void processSystemProperties() {
    // find and record all tc properties set via system properties

    for (Iterator i = System.getProperties().entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      String key = (String) entry.getKey();
      if (key.startsWith(SYSTEM_PROP_PREFIX)) {
        localTcProperties.setProperty(key.substring(SYSTEM_PROP_PREFIX.length()), (String) entry.getValue());
        props.setProperty(key.substring(SYSTEM_PROP_PREFIX.length()), (String) entry.getValue());
      }
    }
  }

  public Properties addAllPropertiesTo(Properties properties) {
    return addAllPropertiesTo(properties, null);
  }

  public synchronized void overwriteTcPropertiesFromConfig(TcProperty[] tcProperties) {
    if (initialized) return;

    // tc properties are now fully initialized
    initialized = true;

    applyConfigOverrides(tcProperties);

    // flip the logger proxy to the real deal
    try {
      ((LoggingInvocationHandler) Proxy.getInvocationHandler(logger)).switchToRealLogger();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    logger.info("Loaded TCProperties : " + toString());
  }

  private void applyConfigOverrides(TcProperty[] tcProperties) {
    int noOfProperties = tcProperties.length;

    if (noOfProperties == 0) {
      logger.info("tc-config doesn't have any tc-property. No tc-property will be overridden");
      return;
    }

    String propertyName, propertyValue;

    for (int i = 0; i < noOfProperties; i++) {
      propertyName = tcProperties[i].getPropertyName();
      propertyValue = tcProperties[i].getPropertyValue();
      if (!this.localTcProperties.containsKey(propertyName)) {
        logger.info("The property \"" + propertyName + "\" has been overridden to " + propertyValue + " from "
                    + getProperty(propertyName) + " by the tc-config file");
        setProperty(propertyName, propertyValue);
      }
    }
  }

  Properties addAllPropertiesTo(Properties properties, String filter) {
    if (filter == null) {
      properties.putAll(props);
      return properties;
    }
    for (Iterator i = props.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      String key = (String) e.getKey();
      if (key.startsWith(filter)) {
        properties.put(key.substring(filter.length()), e.getValue());
      }
    }
    return properties;
  }

  private void loadOverrides(String propDir, String propFile) {
    File file = new File(propDir, propFile);
    loadOverrides(file);
  }

  private void loadOverrides(String propFile) {
    File file = new File(propFile);
    loadOverrides(file);
  }

  private void loadOverrides(File file) {
    if (file.canRead()) {
      try {
        FileInputStream fin = new FileInputStream(file);
        logger.info("Loading override properties from : " + file);
        localTcProperties.load(fin);
        fin.close();
        props.putAll(localTcProperties);
      } catch (FileNotFoundException e) {
        logger.info("Couldnt find " + file + ". Ignoring it", e);
      } catch (IOException e) {
        logger.info("Couldnt read " + file + ". Ignoring it", e);
      }
    }
  }

  private String getTCJarRootDirectory() {
    URL url = TCPropertiesImpl.class.getProtectionDomain().getCodeSource().getLocation();
    String path = url.getPath();
    if (!path.toLowerCase().endsWith(".jar")) { return null; }
    File jarFile = new File(path);
    String dir = jarFile.getParent();
    return dir;
  }

  private void loadDefaults(String propFile) {
    InputStream in = TCPropertiesImpl.class.getResourceAsStream(propFile);
    if (in == null) { throw new AssertionError("TC Property file " + propFile + " not Found"); }
    try {
      logger.info("Loading default properties from " + propFile);
      props.load(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static TCProperties getProperties() {
    return INSTANCE;
  }

  public TCProperties getPropertiesFor(String category) {
    if (category == null) { throw new AssertionError("Category cant be null"); }
    return new TCSubProperties(INSTANCE, category);
  }

  public String getProperty(String key) {
    return getProperty(key, false);
  }

  public String getProperty(String key, boolean missingOkay) {
    String val = props.getProperty(key);
    if (val == null && !missingOkay) { throw new AssertionError("TCProperties : Property not found for " + key); }
    if (!initialized) {
      logger.info("The property \"" + key + "\" has been read before the initialization is complete. \"" + key
                  + "\" = " + val);
    }
    return val;
  }

  /*
   * Used only in test
   */
  public void setProperty(String key, String value) {
    INSTANCE.props.setProperty(key, value);
  }

  public String toString() {
    return "TCProperties = { " + sortedPropertiesToString() + " }";
  }

  private String sortedPropertiesToString() {
    Object properties[] = props.keySet().toArray();
    Arrays.sort(properties);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < properties.length; i++) {
      sb.append(properties[i]).append(" = ").append(props.get(properties[i]));
      if (i != properties.length - 1) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  public boolean getBoolean(String key) {
    String val = getProperty(key);
    return Boolean.valueOf(val).booleanValue();
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    String val = getProperty(key, true);
    if (val == null) return defaultValue;
    return Boolean.valueOf(val).booleanValue();
  }

  public int getInt(String key) {
    String val = getProperty(key);
    return Integer.valueOf(val).intValue();
  }

  public int getInt(String key, int defValue) {
    String val = getProperty(key, true);
    if (val == null) return defValue;
    else return Integer.parseInt(val);
  }

  public long getLong(String key) {
    String val = getProperty(key);
    return Long.valueOf(val).longValue();
  }

  public long getLong(String key, long defValue) {
    String val = getProperty(key, true);
    if (val == null) return defValue;
    else return Long.parseLong(val);
  }

  public float getFloat(String key) {
    String val = getProperty(key);
    return Float.valueOf(val).floatValue();
  }

  private static TCLogger newLoggingProxy() {
    LoggingInvocationHandler handler = new LoggingInvocationHandler();
    Class[] interfaces = new Class[] { TCLogger.class };
    ClassLoader loader = TCPropertiesImpl.class.getClassLoader();
    return (TCLogger) Proxy.newProxyInstance(loader, interfaces, handler);
  }

  /**
   * The whole point of this class is to delay initializing logging until TCProperties have been initialized. This proxy
   * will buffer calls to logger until the switch method is called
   */
  private static class LoggingInvocationHandler implements InvocationHandler {
    private final List calls    = new ArrayList();
    private boolean    switched = false;
    private TCLogger   realLogger;

    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (switched) { return method.invoke(realLogger, args); }

      if ((method.getReturnType() != Void.TYPE) && !switched) {
        // if we haven't switched, it isn't clear what the return values should be
        throw new UnsupportedOperationException("only void return type methods can be called before the switch");
      }

      calls.add(new Call(method, args));
      return null;
    }

    synchronized void switchToRealLogger() throws Exception {
      if (switched) { throw new IllegalStateException("Already switched"); }

      realLogger = TCLogging.getLogger(TCProperties.class);

      for (Iterator i = calls.iterator(); i.hasNext();) {
        Call call = (Call) i.next();
        call.execute(realLogger);
      }

      calls.clear();
      switched = true;
    }

    private static class Call {

      private final Method   method;
      private final Object[] args;

      Call(Method method, Object[] args) {
        this.method = method;
        this.args = args;
      }

      void execute(TCLogger target) throws Exception {
        method.invoke(target, args);
      }
    }

  }

}
