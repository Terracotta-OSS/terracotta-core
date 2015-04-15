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
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;

import com.terracotta.toolkit.util.ToolkitObjectStatus;

import java.util.Comparator;
import java.util.SortedMap;

public class SubTypeWrapperSortedMap<K, V> extends SubTypeWrapperMap<K, V> implements SortedMap<K, V> {
  private final SortedMap<K, V> sortedMap;

  public SubTypeWrapperSortedMap(SortedMap<K, V> map, ToolkitObjectStatus status, String superTypeName,
                                 ToolkitObjectType toolkitObjectType) {
    super(map, status, superTypeName, toolkitObjectType);
    this.sortedMap = map;
  }

  @Override
  public Comparator<? super K> comparator() {
    assertStatus();
    return sortedMap.comparator();
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    assertStatus();
    return new SubTypeWrapperSortedMap<K, V>(sortedMap.subMap(fromKey, toKey), status, superTypeName, toolkitObjectType);
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    assertStatus();
    return new SubTypeWrapperSortedMap<K, V>(sortedMap.headMap(toKey), status, superTypeName, toolkitObjectType);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    assertStatus();
    return new SubTypeWrapperSortedMap<K, V>(sortedMap.tailMap(fromKey), status, superTypeName, toolkitObjectType);
  }

  @Override
  public K firstKey() {
    assertStatus();
    return sortedMap.firstKey();
  }

  @Override
  public K lastKey() {
    assertStatus();
    return sortedMap.lastKey();
  }

}