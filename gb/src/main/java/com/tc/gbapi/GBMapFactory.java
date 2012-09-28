package com.tc.gbapi;

import java.util.Collection;

/**
 * @author tim
 */
public interface GBMapFactory {

  <K, V> GBMap<K, V> createMap(GBMapConfig<K, V> mapConfig, Object ... configs);
}
