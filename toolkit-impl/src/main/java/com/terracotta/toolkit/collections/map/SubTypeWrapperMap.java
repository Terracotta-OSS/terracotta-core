package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.util.ToolkitObjectStatus;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SubTypeWrapperMap<K, V> implements Map<K, V> {
  protected final Map<K, V>           map;

  protected final ToolkitObjectStatus status;
  protected final int                 rejoinCount;
  protected final String              superTypeName;
  protected final ToolkitObjectType   toolkitObjectType;

  public SubTypeWrapperMap(Map<K, V> map, ToolkitObjectStatus status, String superTypeName,
                           ToolkitObjectType toolkitObjectType) {
    super();
    this.map = map;
    this.status = status;
    this.rejoinCount = status.getCurrentRejoinCount();
    this.superTypeName = superTypeName;
    this.toolkitObjectType = toolkitObjectType;
  }

  @Override
  public int size() {
    assertStatus();
    return map.size();
  }

  protected void assertStatus() {
    if (status.isDestroyed()) { throw new IllegalStateException(
                                                                "The object "
                                                                    + this.superTypeName
                                                                    + " of type "
                                                                    + this.toolkitObjectType
                                                                    + "  has already been destroyed, all SubTypes associated with are unusable "); }
    if (this.rejoinCount != status.getCurrentRejoinCount()) { throw new RejoinException(
                                                                                        "The SubTypes associated with "
                                                                                            + this.superTypeName
                                                                                            + " of type "
                                                                                            + this.toolkitObjectType
                                                                                            + " are not usable anymore afer rejoin!"); }
  }

  @Override
  public boolean isEmpty() {
    assertStatus();
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object paramObject) {
    assertStatus();
    return map.containsKey(paramObject);
  }

  @Override
  public boolean containsValue(Object paramObject) {
    assertStatus();
    return map.containsValue(paramObject);
  }

  @Override
  public V get(Object paramObject) {
    assertStatus();
    return map.get(paramObject);
  }

  @Override
  public V put(K paramK, V paramV) {
    assertStatus();
    return map.put(paramK, paramV);
  }

  @Override
  public V remove(Object paramObject) {
    assertStatus();
    return map.remove(paramObject);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> paramMap) {
    assertStatus();
    map.putAll(paramMap);
  }

  @Override
  public void clear() {
    assertStatus();
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    assertStatus();
    return new SubTypeWrapperSet<K>(map.keySet(), status, this.superTypeName, ToolkitObjectType.CACHE);
  }

  @Override
  public Collection<V> values() {
    assertStatus();
    return new SubTypeWrapperCollection<V>(map.values(), status, superTypeName, ToolkitObjectType.CACHE);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    assertStatus();
    return new SubTypeWrapperSet<Entry<K, V>>(map.entrySet(), status, this.superTypeName, ToolkitObjectType.CACHE);
  }
}