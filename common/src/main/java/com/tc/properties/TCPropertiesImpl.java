/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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


import com.tc.lang.TCThreadGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

  private final Map<String, String>         props                      = new LinkedHashMap<>();

  private final ThreadLocal<Map<String, String>>         localTcProperties          = new InheritableThreadLocal<>();

  static {
    INSTANCE = new TCPropertiesImpl();
  }

  private TCPropertiesImpl() {
    loadDefaults(DEFAULT_TC_PROPERTIES_FILE);
    String tcJarDir = getTCJarRootDirectory();
    if (tcJarDir != null) {
      loadOverrides(new File(tcJarDir, TC_PROPERTIES_FILE));
    }
    String tcPropFile = System.getProperty(TC_PROPERTIES_SYSTEM_PROP);
    if (tcPropFile != null) {
      loadOverrides(new File(tcPropFile));
    }

    // this happens last -- system properties have highest precedence
    processSystemProperties();

    warnForOldProperties();
  }

  public static String tcSysProp(String prop) {
    return SYSTEM_PROP_PREFIX + prop;
  }

  private void warnForOldProperties() {
    for (String oldProperty : TCPropertiesConsts.OLD_PROPERTIES) {
      if (props.containsKey(key(oldProperty))) {
        logger.warn("The property \"" + oldProperty
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
          props.put(key(key.substring(SYSTEM_PROP_PREFIX.length())), value.trim());
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

    logger.debug("Loaded TCProperties : " + this);
  }

  private void applyConfigOverrides(Map<String, String> overwriteProps) {
    if (overwriteProps.isEmpty()) {
      logger.debug("tc-config doesn't have any tc-property. No tc-property will be overridden");
      return;
    }

    Map<String, String> locals = TCThreadGroup.threadGroupSingleton(TCPropertyStore.class);
    if (locals == null) {
      locals = localTcProperties.get();
      if (locals == null) {
        locals = new LinkedHashMap<>();
        localTcProperties.set(locals);
      }
    }

    for (Entry<String, String> prop : overwriteProps.entrySet()) {
      String propertyName = prop.getKey();
      String propertyValue = prop.getValue();
      String key = key(propertyName);
      String value = propertyValue.trim();

      if (!this.props.containsKey(key) && !TC_PROPERTIES_WITH_NO_DEFAULTS.contains(key)) {
        logger.warn("The property \"" + propertyName
                    + "\" is not present in set of defined tc properties. Probably this is misspelled");
      }
      if (locals != null) {
        if (!locals.containsKey(key)) {
          if(TC_PROPERTIES_WITH_NO_DEFAULTS.contains(key)) {
            logger.info("The property \"" + propertyName + "\" was set to " + propertyValue + " by the tc-config file");
          } else {
            logger.info("The property \"" + propertyName + "\" was overridden to " + propertyValue + " from "
                        + props.get(key) + " by the tc-config file");
          }
          locals.put(key, value);
        } else {
          logger.warn("The property \"" + propertyName + "\" was set by local settings to "
                      + props.get(key) + ". This will not be overridden to " + propertyValue
                      + " from the tc-config file");
        }
      } else {
        props.put(key, value);
        logger.info("The property \"" + propertyName + "\" was overridden to " + propertyValue + " from "
                        + props.get(key) + " by the tc-config file");
      }
    }

  }

  Properties addAllPropertiesTo(Properties properties, String filter) {
    if (filter == null) {
      properties.putAll(props);
    } else {
      for (Entry<String, String> entry : props.entrySet()) {
        if (entry.getKey().startsWith(filter)) {
          properties.put(entry.getKey().substring(filter.length()), entry.getValue());
        }
      }
    }
    return properties;
  }

  private void loadOverrides(File file) {
    if (file.canRead()) {
      try (FileInputStream fin = new FileInputStream(file)) {
        logger.info("Loading override properties from : " + file);
        load(fin);
      } catch (FileNotFoundException e) {
        logger.info("Couldn't find " + file + ". Ignoring it", e);
      } catch (IOException e) {
        logger.info("Couldn't read " + file + ". Ignoring it", e);
      }
    }
  }

  private String getTCJarRootDirectory() {
    URL url = TCPropertiesImpl.class.getProtectionDomain().getCodeSource().getLocation();
    if (url == null) {
      return null;
    } else {
      String path = url.getPath();
      if (path.toLowerCase(Locale.ROOT).endsWith(".jar")) {
        File jarFile = new File(path);
        return jarFile.getParent();
      } else {
        return null;
      }
    }
  }

  private void loadDefaults(String propFile) {
    URL url = TCPropertiesImpl.class.getResource(propFile);
    if (url == null) {
      throw new AssertionError("TC Property file " + propFile + " not Found");
    } else {
      try (InputStream in = url.openStream()) {
        logger.debug("Loading default properties from " + propFile);
        load(in);
      } catch (IOException e) {
        throw new AssertionError(e);
      }
      }
  }

  private void load(InputStream in) throws IOException {
    Properties properties = new Properties();
    properties.load(in);
    properties.forEach((k, v) -> setProperty(k.toString(), v.toString()));
  }

  public static TCProperties getProperties() {
    return INSTANCE;
  }

  @Override
  public TCProperties getPropertiesFor(String category) {
    if (category == null) {
      throw new AssertionError("Category cant be null");
    } else {
      return new TCSubProperties(this, category);
    }
  }

  @Override
  public String getProperty(String key, boolean missingOkay) {
    if (missingOkay) {
      return getProperty(key, () -> null);
    } else {
      return getProperty(key, () -> {
        throw new AssertionError("TCProperties : Property not found for " + key);
      });
    }
  }

  private String getProperty(String propertyName, Supplier<String> missing) {
    String key = key(propertyName);

    Map<String, String> localProperties = TCThreadGroup.threadGroupSingleton(TCPropertyStore.class);
    if (localProperties == null) {
      localProperties = localTcProperties.get();
    }

    String val = localProperties == null ? null : localProperties.get(key);
    if (val == null) {
      val = props.get(key);
    }
    if (val == null) {
      return missing.get();
    }

    return val;
  }

  /*
   * Used only in test
   */
  @Override
  public void setProperty(String propertyName, String value) {
    String key = key(propertyName);

    if (value == null) {
      props.remove(key);
    } else {
      props.put(key, value.trim());
    }
  }

  @Override
  public String toString() {
    return props.entrySet().stream()
            .sorted(Entry.comparingByKey())
            .map(e -> e.getKey() + " = " + e.getValue())
            .collect(Collectors.joining(", ", "TCProperties = { ", " }"));
  }

  private static String key(String key) {
    return key.toLowerCase(Locale.ROOT);
  }

  private static class TCPropertyStore extends HashMap<String, String> {

  }
}
