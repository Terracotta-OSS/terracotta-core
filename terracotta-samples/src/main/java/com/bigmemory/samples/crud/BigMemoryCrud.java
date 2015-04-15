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
package com.bigmemory.samples.crud;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import com.bigmemory.commons.model.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.bigmemory.commons.util.ReadUtil.waitForInput;

/**
 * <p>Sample app briefly showing some of the api's one can use to create, read, update and delete in
 * BigMemory.
 * <p/>
 */
public class BigMemoryCrud {

  public static void main(String[] args) throws IOException {
    /** Setting up the bigMemory configuration **/

    Configuration managerConfiguration = new Configuration();
    managerConfiguration.name("config")
        .terracotta(new TerracottaClientConfiguration().url("localhost:9510"))
        .cache(new CacheConfiguration()
            .name("bigMemory-crud")
            .maxBytesLocalHeap(128, MemoryUnit.MEGABYTES)
            .terracotta(new TerracottaConfiguration())
        );

    CacheManager manager = CacheManager.create(managerConfiguration);
    Cache bigMemory = manager.getCache("bigMemory-crud");

    try {
      //put value
      System.out.println("**** Put key 1 / value timDoe. ****");
      final Person timDoe = new Person("Tim Doe", 35, Person.Gender.MALE,
          "eck street", "San Mateo", "CA");
      bigMemory.put(new Element("1", timDoe));
      waitForInput();

      //get value
      System.out.println("**** Retrieve key 1. ****");
      final Element element = bigMemory.get("1");
      System.out.println("The value for key 1 is  " + element.getObjectValue());
      waitForInput();

      //update value
      System.out.println("**** Update value for key 1 to pamelaJones ****");
      final Person pamelaJones = new Person("Pamela Jones", 23, Person.Gender.FEMALE,
          "berry st", "Parsippany", "LA");
      bigMemory.put(new Element("1", pamelaJones));
      final Element updated = bigMemory.get("1");
      System.out.println("The value for key 1 is now " + updated.getObjectValue() + ". Key 1 has been updated.");
      waitForInput();

      //delete value
      System.out.println("**** Delete key 1. ****");
      bigMemory.remove("1");
      System.out.println("Retrieve key 1.");
      final Element removed = bigMemory.get("1");
      System.out.println("Value for key 1 is " + removed + ". Key 1 has been deleted.");
      waitForInput();

      System.out.println("Number of element : " + bigMemory.getSize());
      waitForInput();

      //put all
      System.out.println("**** Put 5 keys/values.  ****");
      bigMemory.putAll(getFiveElements());
      System.out.println("Number of element : " + bigMemory.getSize());
      waitForInput();

      //get all
      System.out.println("**** Get elements of keys 1,2 and 3.  ****");
      final Map<Object, Element> elementsMap = bigMemory.getAll(Arrays.asList("1", "2", "3"));
      for (Element currentElement : elementsMap.values()) {
        System.out.println(currentElement);
      }

      waitForInput();

      //remove all
      System.out.println("**** Remove the element with keys 1,2,3. ****");
      bigMemory.removeAll(Arrays.asList("1", "2", "3"));
      System.out.println("Number of element : " + bigMemory.getSize());

      //removing all elements
      System.out.println("**** Remove all elements. ****");
      bigMemory.removeAll();
      waitForInput();
      System.out.println("Number of element : " + bigMemory.getSize());

    } finally {
      if (manager != null) manager.shutdown();
    }
  }


  private static Collection<Element> getFiveElements() {
    Collection<Element> elements = new ArrayList<Element>();
    elements.add(new Element("1", new Person("Jane Doe", 35, Person.Gender.FEMALE,
        "eck street", "San Mateo", "CA")));
    elements.add(new Element("2", new Person("Marie Antoinette", 23, Person.Gender.FEMALE,
        "berry st", "Parsippany", "LA")));
    elements.add(new Element("3", new Person("John Smith", 25, Person.Gender.MALE,
        "big wig", "Beverly Hills", "NJ")));
    elements.add(new Element("4", new Person("Paul Dupont", 25, Person.Gender.MALE,
        "big wig", "Beverly Hills", "NJ")));
    elements.add(new Element("5", new Person("Juliet Capulet", 25, Person.Gender.FEMALE,
        "big wig", "Beverly Hills", "NJ")));

    return elements;
  }


}
