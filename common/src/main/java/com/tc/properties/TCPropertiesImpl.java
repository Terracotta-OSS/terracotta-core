/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.properties;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.util.io.IOUtils;
import com.tc.util.properties.TCPropertyStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * This class is an easy way to read properties that will help tune DSO. It first loads properties from the
 * tc.properties file in the same package as this file and these properties can be overloaded by tc.properites in the
 * base directory where tc.jar is present. TODO:: Improve tcbuild to aggregate properties from different directories
 * during build time.
 */
public class TCPropertiesImpl implements TCProperties {

  private static final Logger logger = LoggerFactory.getLogger(TCPropertiesImpl.class);

  private static final Set<String> TC_PROPERTIES_WITH_NO_DEFAULTS = new HashSet<String>(Arrays.asList(TCPropertiesConsts.TC_PROPERTIES_WITH_NO_DEFAULTS));

  public static final String            SYSTEM_PROP_PREFIX         = "com.tc.";

  // This file resides in src.resource/com/tc/properties directory
  private static final String           DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  // This file,if present, overrides the default properties and resides in the same directory as tc.jar
  private static final String           TC_PROPERTIES_FILE         = "tc.properties";

  // This is the system property that can be set to point to a tc.properties file
  private static final String           TC_PROPERTIES_SYSTEM_PROP  = "com.tc.properties";

  private static final TCPropertiesImpl INSTANCE;

  private final TCPropertyStore         props                      = new TCPropertyStore();

  private final TCPropertyStore         localTcProperties          = new TCPropertyStore();

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

    warnForOldProperties();
  }

  public static String tcSysProp(String prop) {
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

  private void processSystemProperties() {
    // find and record all tc properties set via system properties

    // NOT using System.getProperties().entrySet() since that might throw ConcurrentModificationException
    for (String key : System.getProperties().stringPropertyNames()) {
      if (key.startsWith(SYSTEM_PROP_PREFIX)) {
        String value = System.getProperty(key);
        if (value != null) {
          localTcProperties.setProperty(key.substring(SYSTEM_PROP_PREFIX.length()), value);
          props.setProperty(key.substring(SYSTEM_PROP_PREFIX.length()), value);
        }
      }
    }
  }

  @Override
  public Properties addAllPropertiesTo(Properties properties) {
    return addAllPropertiesTo(properties, null);
  }

  @Override
  public synchronized void overwriteTcPropertiesFromConfig(Map<String, String> overwriteProps) {
    applyConfigOverrides(overwriteProps);

    if (!initialized) {
      initialized = true;
    } else {
      return;
    }

    logger.debug("Loaded TCProperties : " + toString());
  }

  private void applyConfigOverrides(Map<String, String> overwriteProps) {
    if (overwriteProps.isEmpty()) {
      logger.debug("tc-config doesn't have any tc-property. No tc-property will be overridden");
      return;
    }

    for (Entry<String, String> prop : overwriteProps.entrySet()) {
      String propertyName = prop.getKey();
      String propertyValue = prop.getValue();
      if (!this.props.containsKey(propertyName) && !TC_PROPERTIES_WITH_NO_DEFAULTS.contains(propertyName)) {
        logger.warn("The property \"" + propertyName
                    + "\" is not present in set of defined tc properties. Probably this is misspelled");
      }
      if (!this.localTcProperties.containsKey(propertyName)) {
        if(TC_PROPERTIES_WITH_NO_DEFAULTS.contains(propertyName)) {
          logger.info("The property \"" + propertyName + "\" was set to " + propertyValue + " by the tc-config file");
        } else {
          logger.info("The property \"" + propertyName + "\" was overridden to " + propertyValue + " from "
                      + props.getProperty(propertyName) + " by the tc-config file");
        }
        setProperty(propertyName, propertyValue);
      } else {
        logger.warn("The property \"" + propertyName + "\" was set by local settings to "
                    + props.getProperty(propertyName) + ". This will not be overridden to " + propertyValue
                    + " from the tc-config file");
      }
    }
  }

  Properties addAllPropertiesTo(Properties properties, String filter) {
    for (String key : props.keysArray()) {
      if (filter == null) {
        properties.put(key, props.getProperty(key));
      } else if (key.startsWith(filter)) {
        properties.put(key.substring(filter.length()), props.getProperty(key));
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
    if (url == null) { return null; }
    String path = url.getPath();
    if (!path.toLowerCase().endsWith(".jar")) { return null; }
    File jarFile = new File(path);
    String dir = jarFile.getParent();
    return dir;
  }

  private void loadDefaults(String propFile) {
    URL url = TCPropertiesImpl.class.getResource(propFile);
    if (url == null) { throw new AssertionError("TC Property file " + propFile + " not Found"); }
    InputStream in = null;
    try {
      in = url.openStream();
      logger.info("Loading default properties from " + propFile);
      props.load(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static TCProperties getProperties() {
    return INSTANCE;
  }

  @Override
  public TCProperties getPropertiesFor(String category) {
    if (category == null) { throw new AssertionError("Category cant be null"); }
    return new TCSubProperties(INSTANCE, category);
  }

  @Override
  public String getProperty(String key) {
    return getProperty(key, false);
  }

  @Override
  public String getProperty(String key, boolean missingOkay) {
    String val = props.getProperty(key);
    if (val == null && !missingOkay) { throw new AssertionError("TCProperties : Property not found for " + key); }
    if (!initialized) {
      logger.info("The property \"" + key + "\" was read before initialization completed. \"" + key + "\" = " + val);
    }
    return val;
  }

  /*
   * Used only in test
   */
  @Override
  public void setProperty(String key, String value) {
    INSTANCE.props.setProperty(key, value);
  }

  @Override
  public String toString() {
    return "TCProperties = { " + sortedPropertiesToString() + " }";
  }

  private String sortedPropertiesToString() {
    String properties[] = props.keysArray();
    Arrays.sort(properties);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < properties.length; i++) {
      sb.append(properties[i]).append(" = ").append(props.getProperty(properties[i]));
      if (i != properties.length - 1) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  @Override
  public boolean getBoolean(String key) {
    String val = getProperty(key);
    return Boolean.valueOf(val).booleanValue();
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    String val = getProperty(key, true);
    if (val == null) return defaultValue;
    return Boolean.valueOf(val).booleanValue();
  }

  @Override
  public int getInt(String key) {
    String val = getProperty(key);
    return Integer.valueOf(val).intValue();
  }

  @Override
  public int getInt(String key, int defValue) {
    String val = getProperty(key, true);
    if (val == null) return defValue;
    else return Integer.parseInt(val);
  }

  @Override
  public long getLong(String key) {
    String val = getProperty(key);
    return Long.valueOf(val).longValue();
  }

  @Override
  public long getLong(String key, long defValue) {
    String val = getProperty(key, true);
    if (val == null) return defValue;
    else return Long.parseLong(val);
  }

  @Override
  public float getFloat(String key) {
    String val = getProperty(key);
    return Float.valueOf(val).floatValue();
  }
}
