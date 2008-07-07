/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

import com.tc.config.TcProperty;
import com.tc.logging.LogLevel;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
public class TCPropertiesImpl implements TCProperties {

  public static final String            SYSTEM_PROP_PREFIX         = "com.tc.";

  private static final LogBuffer        LOG_BUFFER                 = new LogBuffer();

  // This file resides in src.resource/com/tc/properties directory
  private static final String           DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  // This file,if present, overrides the default properties and resides in the same directory as tc.jar
  private static final String           TC_PROPERTIES_FILE         = "tc.properties";

  // This is the system property that can be set to point to a tc.properties file
  private static final String           TC_PROPERTIES_SYSTEM_PROP  = "com.tc.properties";

  private static final TCPropertiesImpl INSTANCE;

  private final Properties              props                      = new Properties();

  private final Properties              localTcProperties          = new Properties();

  private boolean                       tcPropertiesInitialized    = false;

  private TCLogger                      logger                     = null;

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
        LOG_BUFFER
            .addLog(
                    "The property \""
                        + oldProperties[i]
                        + "\" has been removed/renamed in the latest release. Please update the tc.properties file or some of your settings might not work",
                    LogLevel.WARN);
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

  public void overwriteTcPropertiesFromConfig(TcProperty[] tcProperties) {
    // tc properties are now fully initialized
    tcPropertiesInitialized = true;
    int noOfProperties = tcProperties.length;

    if(this.logger == null){
      logger = TCLogging.getLogger(TCProperties.class);
      Assert.assertNotNull(logger);
    }

    if (noOfProperties == 0) {
      logger.info("tc-config doesn't have any tc-property. No tc-property will be overridden");
      return;
    }

    String propertyName, propertyValue;

    TCProperties tcProps = TCPropertiesImpl.getProperties();
    for (int i = 0; i < noOfProperties; i++) {
      propertyName = tcProperties[i].getPropertyName();
      propertyValue = tcProperties[i].getPropertyValue();
      if (!this.localTcProperties.containsKey(propertyName)) {
        logger.info("The property \"" + propertyName + "\" has been overridden to " + propertyValue + " from "
                    + tcProps.getProperty(propertyName) + " by the tc-config file");
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
        LOG_BUFFER.addLog("Loading override properties from : " + file);
        localTcProperties.load(fin);
        fin.close();
        props.putAll(localTcProperties);
      } catch (FileNotFoundException e) {
        LOG_BUFFER.addLog("Couldnt find " + file + ". Ignoring it", e);
      } catch (IOException e) {
        LOG_BUFFER.addLog("Couldnt read " + file + ". Ignoring it", e);
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
      LOG_BUFFER.addLog("Loading default properties from " + propFile);
      props.load(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static TCProperties getProperties() {
    return INSTANCE;
  }

  public Properties getLocalTcProperties() {
    return localTcProperties;
  }

  public TCProperties getPropertiesFor(String category) {
    if (category == null) { throw new AssertionError("Category cant be null"); }
    return new TCSubProperties(INSTANCE, category);
  }

  public String getProperty(String key) {
    return getProperty(key, false);
  }

  public String getProperty(String key, boolean missingOkay) {
    LoggingWorkaround.doLog();
    if(this.logger == null){
      logger = TCLogging.getLogger(TCProperties.class);
      Assert.assertNotNull(logger);
    }
    
    String val = props.getProperty(key);
    if (val == null && !missingOkay) { throw new AssertionError("TCProperties : Property not found for " + key); }
    if (tcPropertiesInitialized == false) {
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

  static class LogBuffer {
    // This class could be made fancier if it needs to log message at different levels (ie. INFO vs ERROR, etc)

    private final List logs = new ArrayList();

    void addLog(String msg) {
      logs.add(new Entry(msg));
    }

    void addLog(String msg, LogLevel logLevel) {
      logs.add((new Entry(msg, logLevel)));
    }

    void addLog(String msg, Throwable t) {
      logs.add(new Entry(msg, t, LogLevel.INFO));
    }

    void logTo(TCLogger logger) {
      for (Iterator iter = logs.iterator(); iter.hasNext();) {
        Entry e = (Entry) iter.next();
        log(logger, e);
      }
      logs.clear();
    }

    void log(TCLogger logger, Entry e) {
      if (e.logLevel == LogLevel.WARN) {
        if (e.t != null) {
          logger.warn(e.msg, e.t);
        } else {
          logger.warn(e.msg);
        }
      } else if (e.logLevel == LogLevel.ERROR) {
        if (e.t != null) {
          logger.error(e.msg, e.t);
        } else {
          logger.error(e.msg);
        }
      } else {
        if (e.t != null) {
          logger.info(e.msg, e.t);
        } else {
          logger.info(e.msg);
        }
      }
    }

    static class Entry {
      final String    msg;
      final Throwable t;
      final LogLevel  logLevel;

      Entry(String msg) {
        this(msg, null, LogLevel.INFO);
      }

      Entry(String msg, LogLevel logLevel) {
        this(msg, null, logLevel);
      }

      Entry(String msg, Throwable t, LogLevel loglevel) {
        this.msg = msg;
        this.t = t;
        logLevel = loglevel;
      }
    }
  }

  private static class LoggingWorkaround {

    static {
      TCLogger logger = TCLogging.getLogger(TCProperties.class);
      LOG_BUFFER.logTo(logger);
      logger.info("Loaded TCProperties : " + INSTANCE);
    }

    /**
     * the only reason this method is here is to trigger the static initializer of this inner class one (and only once)
     */
    static void doLog() {
      //
    }

  }

}
