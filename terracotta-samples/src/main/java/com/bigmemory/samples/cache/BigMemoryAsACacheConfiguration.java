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
package com.bigmemory.samples.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import com.bigmemory.commons.model.Person;

/**
 * <p>Using BigMemory as a cache and dynamically adding
 * a cache instance
 * <p/>
 */
public class BigMemoryAsACacheConfiguration {


  public static void main(String[] args) throws InterruptedException {
    Configuration managerConfiguration = new Configuration();
    managerConfiguration.name("cacheManagerCompleteExample")
        .terracotta(new TerracottaClientConfiguration().url("localhost:9510"))
        .cache(
            new CacheConfiguration()
                .name("sample-cache")
                .maxBytesLocalHeap(128, MemoryUnit.MEGABYTES)
                .timeToLiveSeconds(4)
                .timeToIdleSeconds(2)
                .terracotta(new TerracottaConfiguration())
        );


    CacheManager manager = CacheManager.create(managerConfiguration);
    try {
      System.out.println("**** BigMemory as a cache - configuration ****");
      Cache testCache = manager.getCache("sample-cache");

      /** the cache is now ready **/
      System.out.println("**** Add value for key 1 to pamelaJones ****");
      final Person pamelaJones = new Person("Pamela Jones", 23, Person.Gender.FEMALE,
          "berry st", "Parsippany", "LA");
      Element element = new Element("1", pamelaJones);
      testCache.put(element);

      System.out.println("**** Waiting for element to reach the time to live ****");
      waitForElementToBeExpired(testCache, element);

      System.out.println("**** The element has expired ****");
      System.out.println("**** Successfully configured as a cache **** ");
    } finally {
      if (manager != null) manager.shutdown();
    }
  }

  private static void waitForElementToBeExpired(final Cache testCache, final Element element) {
    while (!testCache.isExpired(element)) {

    }
  }


}
