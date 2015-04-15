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
package com.bigmemory.samples.configprogrammatic;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import java.io.IOException;

/**
 * <p>Size-based config using a programmatic configuration
 * <p/>
 * <p> Link to doc http://ehcache.org/documentation/configuration/bigmemory </p>
 *
 */
public class ProgrammaticBasedBigMemoryConfiguration {

  /**
   * Run a test with BigMemory using a programmatic configuration
   *
   * @throws java.io.IOException
   */
  public static void main(String[] args) throws IOException {

    System.out.println("**** Programatically configure an instance ****");

    Configuration managerConfiguration = new Configuration()
        .name("bigmemory-config")
        .terracotta(new TerracottaClientConfiguration().url("localhost:9510"))
        .cache(new CacheConfiguration()
            .name("bigMemory")
            .maxBytesLocalHeap(128, MemoryUnit.MEGABYTES)
            .copyOnRead(true)
            .eternal(true)
            .terracotta(new TerracottaConfiguration())
        );

    CacheManager manager = CacheManager.create(managerConfiguration);
    try {
      Cache bigMemory = manager.getCache("bigMemory");
      //bigMemory is now ready.

      System.out.println("**** Successfully created - Programmatic based **** ");


    } finally {
      if (manager != null) manager.shutdown();
    }
  }
}
