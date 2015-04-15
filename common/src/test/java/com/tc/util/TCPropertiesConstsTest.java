/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

    exemptedProperties.add(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_WAIT_SECONDS);
    
    exemptedProperties.add(TCPropertiesConsts.L1_SEDA_PINNED_ENTRY_FAULT_STAGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L1_SERVERMAPMANAGER_FAULT_INVALIDATED_PINNED_ENTRIES);
    exemptedProperties.add(TCPropertiesConsts.L1_OBJECTMANAGER_REMOVED_OBJECTS_SEND_TIMER);
    exemptedProperties.add(TCPropertiesConsts.L1_OBJECTMANAGER_REMOVED_OBJECTS_THRESHOLD);

    exemptedProperties.add(TCPropertiesConsts.L2_EVICTION_CRITICALTHRESHOLD);
    exemptedProperties.add(TCPropertiesConsts.L2_EVICTION_RESOURCEPOLLINGINTERVAL);
    exemptedProperties.add(TCPropertiesConsts.L2_EVICTION_HALTTHRESHOLD);
    exemptedProperties.add(TCPropertiesConsts.L2_EVICTION_OFFHEAP_STOPPAGE);
    exemptedProperties.add(TCPropertiesConsts.L2_EVICTION_STORAGE_STOPPAGE);

    exemptedProperties.add(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_ENABLED);
    exemptedProperties.add(TCPropertiesConsts.L2_OBJECTMANAGER_INVALIDATE_STRONG_CACHE_ENABLED);
    exemptedProperties.add(TCPropertiesConsts.L2_OBJECTMANAGER_REQUEST_PREFETCH_ENABLED);
    exemptedProperties.add(TCPropertiesConsts.L2_OBJECTMANAGER_OIDSET_TYPE);
    exemptedProperties.add(TCPropertiesConsts.L2_OBJECTMANAGER_CLIENT_STATE_VERBOSE_THRESHOLD);

    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_SKIP_JVMARG_CHECK);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_CONCURRENCY);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_INITIAL_DATASIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_TABLESIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_DISABLED);

    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_APPLY_STAGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_SERVER_MAP_CAPACITY_EVICTION_STAGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_MANAGEDOBJECTREQUESTSTAGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEDA_MANAGEDOBJECTRESPONSESTAGE_THREADS);

    exemptedProperties.add(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT);

    exemptedProperties.add(TCPropertiesConsts.L2_ALLOCATION_DISABLE_PARTIAL_MAPS);
    exemptedProperties.add(TCPropertiesConsts.L2_ALLOCATION_DISABLE_PARTIAL_OBJECTS);
    exemptedProperties.add(TCPropertiesConsts.L2_ALLOCATION_ENABLE_OBJECTS_HOTSET);
    exemptedProperties.add(TCPropertiesConsts.L2_ALLOCATION_DISABLE_MAPS_HOTSET);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_MAX_CHUNK_SIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_MIN_CHUNK_SIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_SIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE);
    exemptedProperties.add(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_COUNT);
    
    exemptedProperties.add(TCPropertiesConsts.SEARCH_USE_COMMIT_THREAD);
    exemptedProperties.add(TCPropertiesConsts.SEARCH_QUERY_WAIT_FOR_TXNS);
    exemptedProperties.add(TCPropertiesConsts.SEARCH_LUCENE_MERGE_FACTOR);
    exemptedProperties.add(TCPropertiesConsts.SEARCH_LUCENE_MAX_MERGE_THREADS);
    exemptedProperties.add(TCPropertiesConsts.SEARCH_LUCENE_MAX_MERGE_DOCS);
    exemptedProperties.add(TCPropertiesConsts.SEARCH_LUCENE_DISABLE_FIELD_COMPRESSION);
    exemptedProperties.add(TCPropertiesConsts.SEARCH_LUCENE_MAX_BUFFERED_DOCS);
    exemptedProperties.add(TCPropertiesConsts.SEARCH_LUCENE_MAX_BOOLEAN_CLAUSES);
    exemptedProperties.add(TCPropertiesConsts.L1_SEARCH_MAX_OPEN_RESULT_SETS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEARCH_MAX_PAGED_RESULT_SETS);
    exemptedProperties.add(TCPropertiesConsts.L2_SEARCH_MAX_RESULT_PAGE_SIZE);

    exemptedProperties.add(TCPropertiesConsts.L2_FRS_PREFIX);
    exemptedProperties.add(TCPropertiesConsts.L2_FRS_COMPACTOR_POLICY);
    exemptedProperties.add(TCPropertiesConsts.L2_FRS_COMPACTOR_LSNGAP_MAX_LOAD);
    exemptedProperties.add(TCPropertiesConsts.L2_FRS_COMPACTOR_LSNGAP_MIN_LOAD);
    exemptedProperties.add(TCPropertiesConsts.L2_FRS_COMPACTOR_SIZEBASED_THRESHOLD);
    exemptedProperties.add(TCPropertiesConsts.L2_FRS_COMPACTOR_SIZEBASED_AMOUNT);

    exemptedProperties.add(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_INTERVAL_MS);
    exemptedProperties.add(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_QUEUE_SIZE);
    exemptedProperties.add(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_THREADS);
    exemptedProperties.add(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_QUEUE_SIZE);

    exemptedProperties.add(TCPropertiesConsts.CAS_LOGGING_ENABLED);

    exemptedProperties.add(TCPropertiesConsts.EHCACHE_CLUSTERED_CONFIG_OVERRIDE_MODE);
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
