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
package com.tc.objectserver.persistence.offheap;

import org.junit.Assert;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;

/**
 *
 * @author mscott
 */
public class ManualOffHeapConfigTest extends TCTestCase {

  @Override
  public void setUp() {
    TCProperties props = TCPropertiesImpl.getProperties();
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, "4k");
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_SIZE, "8M");
  }

  public void testHighMinPageSize() {
    TCProperties props = TCPropertiesImpl.getProperties();
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, "8M");
    OffHeapConfig config = new OffHeapConfig(true, "10G", true) {};
    Assert.assertTrue(config.getMinMapPageSize() <=config.getMaxMapPageSize());
  }

  public void testBrokenPageSizing() {
    TCProperties props = TCPropertiesImpl.getProperties();
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, "8M");
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_SIZE, "1M");
    OffHeapConfig config = new OffHeapConfig(true, "10G", true) {};
    Assert.assertTrue(config.getMinMapPageSize() <=config.getMaxMapPageSize());
  }
}
