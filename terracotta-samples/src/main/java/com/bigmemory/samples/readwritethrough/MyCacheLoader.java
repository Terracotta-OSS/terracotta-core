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
package com.bigmemory.samples.readwritethrough;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;

import java.util.Collection;
import java.util.Map;

public class MyCacheLoader implements CacheLoader {

  public Object load(final Object o) throws CacheException {
    return new Element(o, "somevalue");
  }

  public Map loadAll(final Collection collection) {
    return null;
  }

  public Object load(final Object o, final Object o1) {
    return null;
  }

  public Map loadAll(final Collection collection, final Object o) {
    return null;
  }

  public String getName() {
    return null;
  }

  public CacheLoader clone(final Ehcache ehcache) throws CloneNotSupportedException {
    return null;
  }

  public void init() {

  }

  public void dispose() throws CacheException {

  }

  public Status getStatus() {
    return null;
  }
}
