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
package com.bigmemory.samples.arc;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import java.io.IOException;

/**
 * <p>BigMemory configuration - size based config using Automatic Resource Control and dynamically adding one instance
 * so they share the memory allocated by Automatic Resource Control
 * <p/>
 * <p>Automatic Resource Control (ARC) is an intelligent approach to caching with fine-grained controls for tuning cache performance. ARC offers a wealth of benefits, including:
 * <ul>
 * <li>Sizing limitations on in-memory caches to avoid OutOfMemory errors</li>
 * <li>Pooled (CacheManager-level) sizing – no requirement to size caches individually</li>
 * <li>Differentiated tier-based sizing for flexibility</li>
 * <li>Sizing by bytes, entries, or percentages for more flexibility</li>
 * <li>Keeping hot or eternal data where it can substantially boost performance</li>
 * </ul>
 * </p>
 * <p>The two instances share 128 MB
 * <p/>
 * <p>Link to doc : http://ehcache.org/documentation/arc/index </p>
 * <p/>
 */
public class ArcConfiguration {


  public static void main(String[] args) throws IOException {
    CacheManager manager = CacheManager.newInstance(ArcConfiguration.class.getResource("/xml/ehcache-arc.xml"));

    try {
      System.out.println("**** Retrieve bigMemory1 from xml ****");
      Cache bigMemory1 = manager.getCache("bigMemory1");

      System.out.println("**** Dynamically add bigMemory2 ****");
      manager.addCache("bigMemory2");
      Cache bigMemory2 = manager.getCache("bigMemory2");

      System.out.println("**** bigMemory1 and bigMemory2 share ****" + manager.getConfiguration()
          .getMaxBytesLocalHeap() + "b heap");

      System.out.println("**** Successfully configured with ARC **** ");


    } finally {
      if (manager != null) manager.shutdown();
    }
  }

}
