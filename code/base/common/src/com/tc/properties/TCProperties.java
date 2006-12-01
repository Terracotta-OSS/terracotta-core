/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
import java.util.Properties;

/**
 * This class is an easy way to read properties that will help tune DSO. It first loads properties from the
 * tc.properties file in the same package as this file and these properties can be overloaded by tc.properites in the
 * base directory where tc.jar is present. TODO:: Improve tcbuild to aggregate properties from different directories
 * during build time.
 */
public class TCProperties extends Properties {

  private static final TCLogger     logger                     = TCLogging.getLogger(TCProperties.class);

  // This file resides in src.resource/com/tc/properties directory
  private static final String       DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  // This file,if present, overrides the default properties and resides in the same directory as tc.jar
  private static final String       TC_PROPERTIES_FILE         = "tc.properties";

  private static final TCProperties properties;
  static {
    properties = new TCProperties();
  }

  private TCProperties() {
    super();
    loadDefaults(DEFAULT_TC_PROPERTIES_FILE);
    String tcJarDir = getTCJarRootDirectory();
    if (tcJarDir != null) {
      loadOverrides(tcJarDir, TC_PROPERTIES_FILE);
    }
    logger.info("Loaded TCProperties : " + this);
  }
  
  protected TCProperties(String category) {
    // No Op - Used by the subclass
  }

  private void loadOverrides(String propDir, String propFile) {
    File file = new File(propDir, propFile);
    if (file.canRead()) {
      try {
        FileInputStream fin = new FileInputStream(file);
        logger.info("Loading override properties from : " + propDir + File.separator + propFile);
        load(fin);
      } catch (FileNotFoundException e) {
        logger.info("Couldnt find " + propFile + ". Ignoring it", e);
      } catch (IOException e) {
        logger.info("Couldnt read " + propFile + ". Ignoring it", e);
      }
    }
  }
  
  private String getTCJarRootDirectory() {
    URL url = TCProperties.class.getProtectionDomain().getCodeSource().getLocation();
    String path = url.getPath();
    if (!path.toLowerCase().endsWith(".jar")) { return null; }
    File jarFile = new File(path);
    String dir = jarFile.getParent();
    return dir;
  }

  private void loadDefaults(String propFile) {
    InputStream in = TCProperties.class.getResourceAsStream(propFile);
    if (in == null) { throw new AssertionError("TC Property file " + propFile + " not Found"); }
    try {
      logger.info("Loading default properties from " + propFile);
      load(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static TCProperties getProperties() {
    return properties;
  }
  
  public TCProperties getPropertiesFor(String category) {
    if(category == null) {
      throw new AssertionError("Category cant be null");
    }
    return new TCSubProperties(properties, category);
  }
  
  public String getProperty(String key) {
    String val = super.getProperty(key);
    if (val == null) { throw new AssertionError("TCProperties : Property not found for " + key); }
    return val;
  }
  
  public String toString() {
    return "TCProperties="+super.toString();
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

}
