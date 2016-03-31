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
 *
 */
package com.tc.util;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class TCPropertiesConstsTest extends TCTestCase {

  // This file resides in src.resource/com/tc/properties directory
  private static final String      DEFAULT_TC_PROPERTIES_FILE = "tc.properties";
  private static final Set<String> exemptedProperties         = new HashSet<String>();

  private final Properties         props                      = new Properties();

  @Override
  protected void setUp() {
    loadDefaults(DEFAULT_TC_PROPERTIES_FILE);
    exemptedProperties.add(TCPropertiesConsts.LICENSE_PATH);
    exemptedProperties.add(TCPropertiesConsts.PRODUCTKEY_PATH);
    exemptedProperties.add(TCPropertiesConsts.PRODUCTKEY_RESOURCE_PATH);

    exemptedProperties.add(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_WAIT_SECONDS);

    exemptedProperties.add(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT);

    exemptedProperties.add(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_INTERVAL_MS);
    exemptedProperties.add(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_QUEUE_SIZE);
    exemptedProperties.add(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_QUEUE_SIZE);
    
    exemptedProperties.add(TCPropertiesConsts.ENTITY_PROCESSOR_THREADS);
  
    exemptedProperties.add(TCPropertiesConsts.CLIENT_MAX_PENDING_REQUESTS);
    exemptedProperties.add(TCPropertiesConsts.CLIENT_MAX_SENT_REQUESTS);
  }

  private void loadDefaults(String propFile) {
    InputStream in = TCPropertiesImpl.class.getResourceAsStream(propFile);
    if (in == null) { throw new AssertionError("TC Property file " + propFile + " not Found"); }
    try {
      System.out.println("Loading default properties from " + propFile);
      props.load(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  public void testAllConstsDeclared() {
    Set<String> tcPropertiesConsts = new HashSet<String>();
    Field[] fields = TCPropertiesConsts.class.getDeclaredFields();
    for (Field field : fields) {
      try {
        tcPropertiesConsts.add(field.get(null).toString());
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
    tcPropertiesConsts.remove(TCPropertiesConsts.OLD_PROPERTIES.toString());
    
    Set<String> tcProperties = getKeys(props);
    for (String tcProperty : tcProperties) {
      Assert
          .assertTrue("There is no constant declared for " + tcProperty + " in " + TCPropertiesConsts.class.getName(),
                      tcPropertiesConsts.contains(tcProperty));
    }

    for (String tcProperty : tcPropertiesConsts) {
      // TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT is added only for internal testing purposes
      if (!exemptedProperties.contains(tcProperty) && !tcProperties.contains(tcProperty)) {
        int index;
        for (index = 0; index < TCPropertiesConsts.OLD_PROPERTIES.length; index++) {
          if (TCPropertiesConsts.OLD_PROPERTIES[index].equals(tcProperty)) {
            break;
          }
        }
        if (index == TCPropertiesConsts.OLD_PROPERTIES.length) {
          Assert.fail(tcProperty + " is defined in " + TCPropertiesConsts.class.getName()
                      + " but is not present in tc.properties file. " + " Plesase remove it from "
                      + TCPropertiesConsts.class.getName() + " and add it to " + TCPropertiesConsts.class.getName()
                      + " OLD_PROPERTIES field");
        }
      }
    }
  }

  private static Set<String> getKeys(Properties props) {
    Set<String> keys = new HashSet<String>();
    for (Object key : props.keySet()) {
      keys.add((String) key);
    }
    return keys;
  }
}
