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
package com.bigmemory.samples.nonstop;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import com.bigmemory.commons.model.Person;

import java.io.IOException;

import static com.bigmemory.commons.util.ReadUtil.waitForInput;
import static net.sf.ehcache.config.PersistenceConfiguration.Strategy.DISTRIBUTED;

/**
 * <p>Sample app briefly showing a working non stop use case.
 * BigMemory.
 * <p/>
 */
public class BigMemoryNonStopRejoin {

  public static void main(String[] args) throws IOException {


    // Create Cache
    Configuration managerConfig = new Configuration()
        .terracotta(new TerracottaClientConfiguration().url("localhost:9510").rejoin(true))
        .cache(new CacheConfiguration().name("nonstop-sample")
            .persistence(new PersistenceConfiguration().strategy(DISTRIBUTED))
            .maxBytesLocalHeap(128, MemoryUnit.MEGABYTES)
            .terracotta(new TerracottaConfiguration().nonstop(new NonstopConfiguration().immediateTimeout(false)
                .timeoutMillis(10000).enabled(true)
            )
            ));

    CacheManager manager = CacheManager.create(managerConfig);
    Ehcache bigMemory = manager.getEhcache("nonstop-sample");

    try {
      System.out.println("**** Put key 1 / value timDoe. ****");
      final Person timDoe = new Person("Tim Doe", 35, Person.Gender.MALE,
          "eck street", "San Mateo", "CA");
      bigMemory.put(new Element("1", timDoe));

      System.out.println("**** Get key 1 / value timDoe. ****");
      System.out.println(bigMemory.get("1"));
      waitForInput();

      System.out
          .println("**** Now you have to kill the server using the stop-sample-server.bat on Windows or stop-sample-server.sh otherwise ****");
      try {
        while (true) {
          bigMemory.get("1");
        }
      } catch (NonStopCacheException e) {
        System.out
            .println("**** Server is unreachable - NonStopException received when trying to do a get on the server. NonStop is working ****");
      }

      System.out
          .println("**** Now you have to restart the server using the start-sample-server.bat on Windows or start-sample-server.sh otherwise ****");

      boolean serverStart = false;
      while (serverStart  == false) {
        try {
          bigMemory.get("1");
          //if server is unreachable, exception is thrown when doing the get
          serverStart = true;
        } catch (NonStopCacheException e) {
        }
      }
      System.out
          .println("**** Server is reachable - No More NonStopException received when trying to do a get on the server. Rejoin is working ****");
    } finally

    {
      if (manager != null) manager.shutdown();
    }
  }

}
