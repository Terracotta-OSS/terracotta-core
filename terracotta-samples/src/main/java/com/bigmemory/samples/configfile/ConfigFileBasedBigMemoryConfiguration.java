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
package com.bigmemory.samples.configfile;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import java.io.IOException;

/**
 * <p>
 * Size-based config using an xml configuration
 * <p/>
 */

public class ConfigFileBasedBigMemoryConfiguration {
  /**
   * Run a test with BigMemory, configured with an xml configuration file
   *
   * @throws java.io.IOException
   */
  public static void main(String[] args) throws IOException {
    System.out.println("**** Retrieve config from xml ****");
    CacheManager manager = CacheManager.newInstance(ConfigFileBasedBigMemoryConfiguration.class.getResource("/xml/ehcache.xml"));
    try {
      Cache bigMemory = manager.getCache("bigMemory");
      //bigMemory is now ready.

      //Successfully retrieve from the file

      System.out
          .println("**** Successfully created - Config file based **** ");


    } finally {
      if (manager != null) manager.shutdown();
    }
  }

}
