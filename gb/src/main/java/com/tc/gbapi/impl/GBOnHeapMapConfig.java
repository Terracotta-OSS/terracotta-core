package com.tc.gbapi.impl;

import com.tc.gbapi.GBMapConfig;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Snaps
 */
public class GBOnHeapMapConfig<K, V> implements GBMapConfig<K, V> {

  private final Class<K> keyClass;
  private final Class<V> valueClass;
  private GBSerializer<K> keySerializer;
  private GBSerializer<V> valueSerializer;
  private final List<GBMapMutationListener<K, V>> mutationListeners = new ArrayList<GBMapMutationListener<K, V>>();

  public GBOnHeapMapConfig(final Class<K> keyClass, final Class<V> valueClass) {
    this.keyClass = keyClass;
    this.valueClass = valueClass;
  }

  @Override
  public void setKeySerializer(final GBSerializer<K> serializer) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void setValueSerializer(final GBSerializer<V> serializer) {
    throw new UnsupportedOperationException("Implement me!");
  }

  public GBSerializer<K> getKeySerializer() {
    return keySerializer;
  }

  public GBSerializer<V> getValueSerializer() {
    return valueSerializer;
  }

  @Override
  public Class<K> getKeyClass() {
    return keyClass;
  }

  @Override
  public Class<V> getValueClass() {
    return valueClass;
  }

  @Override
  public void addListener(final GBMapMutationListener<K, V> listener) {
    mutationListeners.add(listener);
  }

  @Override
  public List<GBMapMutationListener<K, V>> getMutationListeners() {
    return mutationListeners;
  }
}
