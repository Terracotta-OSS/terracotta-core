/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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

  public static final String        SYSTEM_PROP_PREFIX         = "com.tc.properties";

  private static final LogBuffer    LOG_BUFFER                 = new LogBuffer();

  // This file resides in src.resource/com/tc/properties directory
  private static final String       DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  // This file,if present, overrides the default properties and resides in the same directory as tc.jar
  private static final String       TC_PROPERTIES_FILE         = "tc.properties";

  private static final TCProperties INSTANCE;

  private final Properties          props                      = new Properties();

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

    applySystemPropertyOverrides();
  }

  private void applySystemPropertyOverrides() {
    for (Iterator i = props.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      String key = (String) e.getKey();
      String sysPropOverride = System.getProperty(SYSTEM_PROP_PREFIX + "." + key);
      if (sysPropOverride != null) {
        e.setValue(sysPropOverride);
      }
    }
  }

  TCPropertiesImpl(String category) {
    // No Op - Used by the subclass
  }

  private void loadOverrides(String propDir, String propFile) {
    File file = new File(propDir, propFile);
    if (file.canRead()) {
      try {
        FileInputStream fin = new FileInputStream(file);
        LOG_BUFFER.addLog("Loading override properties from : " + propDir + File.separator + propFile);
        props.load(fin);
      } catch (FileNotFoundException e) {
        LOG_BUFFER.addLog("Couldnt find " + propFile + ". Ignoring it", e);
      } catch (IOException e) {
        LOG_BUFFER.addLog("Couldnt read " + propFile + ". Ignoring it", e);
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

  public TCProperties getPropertiesFor(String category) {
    if (category == null) { throw new AssertionError("Category cant be null"); }
    return new TCSubProperties(INSTANCE, category);
  }

  public String getProperty(String key) {
    return getProperty(key, false);
  }

  public String getProperty(String key, boolean missingOkay) {
    LoggingWorkaround.doLog();
    String val = props.getProperty(key);
    if (val == null && !missingOkay) { throw new AssertionError("TCProperties : Property not found for " + key); }
    return val;
  }

  public String toString() {
    return "TCProperties=" + props.toString();
  }

  public boolean getBoolean(String key) {
    String val = getProperty(key);
    return Boolean.valueOf(val).booleanValue();
  }

  public int getInt(String key) {
    String val = getProperty(key);
    return Integer.valueOf(val).intValue();
  }

  public long getLong(String key) {
    String val = getProperty(key);
    return Long.valueOf(val).longValue();
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

    void addLog(String msg, Throwable t) {
      logs.add(new Entry(msg, t));
    }

    void logTo(TCLogger logger) {
      for (Iterator iter = logs.iterator(); iter.hasNext();) {
        Entry e = (Entry) iter.next();
        if (e.t != null) {
          logger.info(e.msg, e.t);
        } else {
          logger.info(e.msg);
        }
      }
      logs.clear();
    }

    static class Entry {
      final String    msg;
      final Throwable t;

      Entry(String msg) {
        this(msg, null);
      }

      Entry(String msg, Throwable t) {
        this.msg = msg;
        this.t = t;
      }
    }
  }

  private static class LoggingWorkaround {
    static {
      TCLogger logger = TCLogging.getLogger(TCProperties.class);
      LOG_BUFFER.logTo(logger);
      logger.info("Loaded TCProperties : " + INSTANCE);
    }

    static void doLog() {
      // the only reason this method is here is to trigger the static initilizer of this inner class one (and only once)
    }

  }
}
