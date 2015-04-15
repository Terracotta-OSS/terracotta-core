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

import com.tc.test.TCTestCase;
import com.tc.util.Conversion;

/**
 *
 * @author mscott
 */
public class OffHeapConfigTest extends TCTestCase {

  private static final long MB = 1024 * 1024;
  private static final long GB = 1024 * MB;
  
  public void testDynamicPageSizing() {
    OffHeapConfig config = new OffHeapConfig(true, "10G", true) {};
    Assert.assertTrue(config.getMinMapPageSize() <=config.getMaxMapPageSize());
    Assert.assertTrue(2 * 1024 * 1024 >= config.getMaxMapPageSize());
    config = new OffHeapConfig(true, "1G", true) {};
    Assert.assertTrue(config.getMinMapPageSize() <=config.getMaxMapPageSize());
    Assert.assertTrue(1 * 1024 * 1024 >= config.getMaxMapPageSize());
    config = new OffHeapConfig(true, "100G", true) {};
    Assert.assertTrue(config.getMinMapPageSize() <=config.getMaxMapPageSize());
    Assert.assertTrue(8 * 1024 * 1024 >= config.getMaxMapPageSize());
  }

  public void testAtLeast2Chunks() {
    OffHeapConfig config = new OffHeapConfig(true, "512M", true) {};
    System.out.println(config.getMaxChunkSize());
    Assert.assertTrue(config.getMaxChunkSize() != config.getOffheapSize());
  }

  public void testHeapToOffheapRatio() throws Exception {
    assertTrue(OffHeapConfig.verifyHeapToOffheapRatio(100L * MB, 512L * MB));
    assertFalse(OffHeapConfig.verifyHeapToOffheapRatio(100L * MB, 2L * GB));
    assertTrue(OffHeapConfig.verifyHeapToOffheapRatio(1L * GB, 10L * GB));
    assertFalse(OffHeapConfig.verifyHeapToOffheapRatio(1L * GB, 15L * GB));
    assertTrue(OffHeapConfig.verifyHeapToOffheapRatio(2L * GB, 100L * GB));
    assertFalse(OffHeapConfig.verifyHeapToOffheapRatio(2L * GB, 120L * GB));
    assertTrue(OffHeapConfig.verifyHeapToOffheapRatio(3L * GB, 120L * GB));
    // 5% interval
    assertTrue(OffHeapConfig.verifyHeapToOffheapRatio(3000 * 1024L * 1024L, 101L * GB));
    assertFalse(OffHeapConfig.verifyHeapToOffheapRatio(2700 * 1024L * 1024L, 101L * GB));

    long maxDataSize = Conversion.memorySizeAsLongBytes("125G");
    assertTrue(OffHeapConfig.verifyHeapToOffheapRatio(4L * GB, maxDataSize));
  }

  public void testJvmHeap() {
    final long total = Runtime.getRuntime().totalMemory();
    final long max = Runtime.getRuntime().maxMemory();
    System.out.println("Total memory: " + total + ", max memory: " + max);
    System.out.println("Total normalized memory: " + Conversion.toJvmArgument(total)
                       + ", max normalized memory: " + Conversion.toJvmArgument(max)
                       + ", required: " + Conversion.toJvmArgument(3L * GB));
  }
}
