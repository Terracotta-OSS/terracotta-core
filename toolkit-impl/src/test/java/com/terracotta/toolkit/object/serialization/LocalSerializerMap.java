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
package com.terracotta.toolkit.object.serialization;

import java.util.HashMap;
import java.util.Map;

/**
* @author tim
*/
class LocalSerializerMap<K, V> implements SerializerMap<K, V> {
  private final Map<K, V> localHashMap = new HashMap<K, V>();

  @Override
  public V put(K key, V value) {
    return localHashMap.put(key, value);
  }

  @Override
  public V get(K key) {
    return localHashMap.get(key);
  }

  @Override
  public V localGet(K key) {
    return get(key);
  }

}
