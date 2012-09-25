package com.tc.objectserver.persistence.gb.gbapi;

/**
 * @author tim
 */
public interface GBMapConfig<K, V> {

  // Just make a single set serializer method that does both key and value
  public void setKeySerializer(GBSerializer<K> serializer);

  public void setValueSerializer(GBSerializer<V> serializer);

  public Class<K> getKeyClass();

  public Class<V> getValueClass();

  public void addListener(GBMapMutationListener<K, V> listener);
}
