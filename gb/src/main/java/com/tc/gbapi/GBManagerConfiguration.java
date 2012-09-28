package com.tc.gbapi;

import java.util.Collection;
import java.util.Map;

/**
 * @author tim
 */
public interface GBManagerConfiguration {

  Collection<Object> sharedConfig();

  Map<String, GBMapConfig<?, ?>> mapConfig();

  Map<String, GBCacheConfig<?, ?>> cacheConfig();

}
