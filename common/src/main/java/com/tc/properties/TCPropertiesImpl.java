/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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

  private static final Set<String> TC_PROPERTIES_WITH_NO_DEFAULTS = new HashSet<>(Arrays.asList(TCPropertiesConsts.TC_PROPERTIES_WITH_NO_DEFAULTS));

  public static final String            SYSTEM_PROP_PREFIX         = "com.tc.";

  // This file resides in src.resource/com/tc/properties directory
  private static final String           DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  // This file,if present, overrides the default properties and resides in the same directory as tc.jar
  private static final String           TC_PROPERTIES_FILE         = "tc.properties";

  // This is the system property that can be set to point to a tc.properties file
  private static final String           TC_PROPERTIES_SYSTEM_PROP  = "com.tc.properties";

  private static final TCPropertiesImpl INSTANCE;

  private final TCPropertyStore         props                      = new TCPropertyStore();

  private static final ThreadLocal<TCPropertyStore>         localTcProperties          = new InheritableThreadLocal<>() {
    @Override
    protected TCPropertyStore initialValue() {
      return new TCPropertyStore();
    }
  };

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
        logger.warn("The property \""
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
    
    logger.debug("Loaded TCProperties : " + toString());
  }

  private void applyConfigOverrides(Map<String, String> overwriteProps) {
    if (overwriteProps.isEmpty()) {
      logger.debug("tc-config doesn't have any tc-property. No tc-property will be overridden");
      return;
    }

    TCPropertyStore locals = localTcProperties.get();

    for (Entry<String, String> prop : overwriteProps.entrySet()) {
      String propertyName = prop.getKey();
      String propertyValue = prop.getValue();
      if (!this.props.containsKey(propertyName) && !TC_PROPERTIES_WITH_NO_DEFAULTS.contains(propertyName)) {
        logger.warn("The property \"" + propertyName
                    + "\" is not present in set of defined tc properties. Probably this is misspelled");
      }
      if (!locals.containsKey(propertyName)) {
        if(TC_PROPERTIES_WITH_NO_DEFAULTS.contains(propertyName)) {
          logger.info("The property \"" + propertyName + "\" was set to " + propertyValue + " by the tc-config file");
        } else {
          logger.info("The property \"" + propertyName + "\" was overridden to " + propertyValue + " from "
                      + props.getProperty(propertyName) + " by the tc-config file");
        }
        locals.setProperty(propertyName, propertyValue);
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
      TCPropertyStore locals = new TCPropertyStore();
      try (FileInputStream fin = new FileInputStream(file)) {
        logger.info("Loading override properties from : " + file);
        locals.load(fin);
        props.putAll(locals);
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
      logger.debug("Loading default properties from " + propFile);
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
    String val = localTcProperties.get().getProperty(key);
    if (val == null) {
      val = props.getProperty(key);
    }
    if (val == null && !missingOkay) { throw new AssertionError("TCProperties : Property not found for " + key); }

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
    StringBuilder sb = new StringBuilder();
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
    return Boolean.parseBoolean(val);
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    String val = getProperty(key, true);
    if (val == null) return defaultValue;
    return Boolean.parseBoolean(val);
  }

  @Override
  public int getInt(String key) {
    String val = getProperty(key);
    return Integer.parseInt(val);
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
    return Long.parseLong(val);
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
    return Float.parseFloat(val);
  }
}
