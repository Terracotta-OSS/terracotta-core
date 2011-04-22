/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
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

    exemptedProperties.add(TCPropertiesConsts.L2_CACHEMANAGER_THRESHOLD);
    exemptedProperties.add(TCPropertiesConsts.L2_CACHEMANAGER_CRITICALTHRESHOLD);

    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_SKIP_JVMARG_CHECK);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_CONCURRENCY);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_INITIAL_DATASIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_TABLESIZE);

    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_FAULTSTAGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_FLUSHSTAGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_MANAGEDOBJECTREQUESTSTAGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_MANAGEDOBJECTRESPONSESTAGE_THREADS);

    exemptedProperties.add(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT);

    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_CACHE_MAX_CHUNK_SIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_CACHE_MIN_CHUNK_SIZE);

    exemptedProperties.add(TCPropertiesConsts.DERBY_STORAGE_PAGESIZE);
    exemptedProperties.add(TCPropertiesConsts.DERBY_STORAGE_PAGECACHESIZE);
    exemptedProperties.add(TCPropertiesConsts.DERBY_PAGECACHE_HEAPUSAGE);
    exemptedProperties.add(TCPropertiesConsts.DERBY_SYSTEM_DURABILITY);
    exemptedProperties.add(TCPropertiesConsts.DERBY_LOG_DEVICE);
    exemptedProperties.add(TCPropertiesConsts.L2_DERBYDB_DERBY_STORAGE_PAGECACHESIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_DERBYDB_DERBY_SYSTEM_DURABILITY);
    exemptedProperties.add(TCPropertiesConsts.L2_DERBYDB_LOG_DEVICE);
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
    Set tcProperties = props.keySet();
    for (Iterator<String> iter = tcProperties.iterator(); iter.hasNext();) {
      String tcProperty = iter.next();
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
}
