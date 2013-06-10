package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;

import com.terracotta.toolkit.util.ToolkitObjectStatus;

import java.util.Comparator;
import java.util.SortedSet;

public class SubTypeWrapperSortedSet<K> extends SubTypeWrapperSet<K> implements SortedSet<K> {
  private final SortedSet<K> sortedSet;

  public SubTypeWrapperSortedSet(SortedSet<K> sortedSet, ToolkitObjectStatus status, String superTypeName,
                                        ToolkitObjectType toolkitObjectType) {
    super(sortedSet, status, superTypeName, toolkitObjectType);
    this.sortedSet = sortedSet;
  }

  @Override
  public Comparator<? super K> comparator() {
    assertStatus();
    return sortedSet.comparator();
  }

  @Override
  public SortedSet<K> subSet(K fromKey, K toKey) {
    assertStatus();
    return new SubTypeWrapperSortedSet(sortedSet.subSet(fromKey, toKey), super.status, super.superTypeName,
                                              ToolkitObjectType.SORTED_SET);
  }

  @Override
  public SortedSet<K> headSet(K toKey) {
    assertStatus();
    return new SubTypeWrapperSortedSet(sortedSet.headSet(toKey), super.status, super.superTypeName,
                                              ToolkitObjectType.SORTED_SET);
  }

  @Override
  public SortedSet<K> tailSet(K fromKey) {
    assertStatus();
    return new SubTypeWrapperSortedSet(sortedSet.tailSet(fromKey), super.status, super.superTypeName,
                                              ToolkitObjectType.SORTED_SET);
  }

  @Override
  public K first() {
    assertStatus();
    return sortedSet.first();
  }

  @Override
  public K last() {
    assertStatus();
    return sortedSet.last();
  }
}