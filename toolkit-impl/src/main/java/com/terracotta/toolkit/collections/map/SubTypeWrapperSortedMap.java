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