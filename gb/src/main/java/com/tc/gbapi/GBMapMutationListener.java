package com.tc.gbapi;

import java.util.Map;

/**
 * @author tim
 */
public interface GBMapMutationListener<K, V> {

  // We think this should return the key that was actually removed, as opposed to the
  // key that was used to perform the remove. (think funny equals contract).
  public void removed(GBRetriever<K> key, GBRetriever<V> value, Map<? extends Enum, Object> metadata);

  public void added(GBRetriever<K> key, GBRetriever<V> value, Map<? extends Enum, Object> metadata);
}
