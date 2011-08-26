/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

/**
 * A wrapper class to store keys and values for the databases.
 */
public class TCDatabaseEntry<K, V> {
  private K key;
  private V value;

  public TCDatabaseEntry() {
    //
  }

  public TCDatabaseEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public K getKey() {
    return key;
  }

  public TCDatabaseEntry setKey(K key) {
    this.key = key;
    return this;
  }

  public V getValue() {
    return value;
  }

  public TCDatabaseEntry setValue(V value) {
    this.value = value;
    return this;
  }

}
