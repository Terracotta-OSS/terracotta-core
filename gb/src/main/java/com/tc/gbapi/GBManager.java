package com.tc.gbapi;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author tim
 */
public class GBManager {

  // no data
  // -> create fresh
  // -> fail
  // data exists
  // -> use
  // -> fail
  // -> delete

  private final GBMapFactory factory;
  private final GBManagerConfigurationDummy configuration;

  private final ConcurrentMap<String, MapHolder> maps = new ConcurrentHashMap<String, MapHolder>();
  private volatile Status status;


  public GBManager(File path, GBMapFactory factory) {
    this.factory = factory;
    this.configuration = new GBManagerConfigurationDummy();
  }

  public GBManagerConfiguration getConfiguration() {
    return this.configuration;
  }

  public Future<Void> start() {
    final FutureTask<Void> future = new FutureTask<Void>(new Runnable() {
      @Override
      public void run() {
        for (Map.Entry<String, GBMapConfig<?, ?>> mapConfigEntry : configuration.mapConfig().entrySet()) {
          final GBMap<?, ?> map = factory.createMap(mapConfigEntry.getValue());
          final String mapAlias = mapConfigEntry.getKey();
          registerMap(mapAlias, map, mapConfigEntry.getValue().getKeyClass(), mapConfigEntry.getValue().getValueClass());
        }
        status = Status.STARTED;
      }
    }, null);
    new Thread(future).start();
    return future;
  }

  public void shutdown() {
    status = Status.STOPPED;
    for (String alias : maps.keySet()) {
      unregisterMap(alias);
    }
  }

  public <K, V> void attachMap(String name, GBMap<K, V> map, Class<K> keyClass, Class<V> valueClass) throws IllegalStateException {
    checkIsStarted();
    registerMap(name, map, keyClass, valueClass);
  }

  public void detachMap(String name) {
    checkIsStarted();
    unregisterMap(name);
  }

  public <K, V> GBMap<K, V> getMap(String name, Class<K> keyClass, Class<V> valueClass) {
    checkIsStarted();
    final MapHolder mapHolder = maps.get(name);
    return mapHolder == null ? null : mapHolder.getMap(keyClass, valueClass);
  }

  public void begin() {
    checkIsStarted();
  }

  public void commit() {
    checkIsStarted();
  }

  private <K, V> void registerMap(final String mapAlias, final GBMap<?, ?> map, final Class<K> keyClass, final Class<V> valueClass) {
    if (maps.putIfAbsent(mapAlias, new MapHolder(map, keyClass, valueClass)) != null) {
      throw new IllegalStateException("Duplicated map for alias: " + mapAlias);
    }
  }

  private void unregisterMap(final String name) {
    maps.remove(name);
  }

  private void checkIsStarted() {
    if (status != Status.STARTED) {
      throw new IllegalStateException("We're " + status + " which is not started");
    }
  }

  private static class GBManagerConfigurationDummy implements GBManagerConfiguration {

    private HashMap<String, GBMapConfig<?, ?>> stringGBMapConfigHashMap = new HashMap<String, GBMapConfig<?, ?>>();

    @Override
    public Collection<Object> sharedConfig() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Map<String, GBMapConfig<?, ?>> mapConfig() {
      return stringGBMapConfigHashMap;
    }
  }

  private static enum Status {INITIALIZED, STARTED, STOPPED}

  private static class MapHolder {

    private final GBMap map;
    private final Class keyClass;
    private final Class valueClass;

    private MapHolder(final GBMap map, final Class keyClass, final Class valueClass) {
      this.map = map;
      this.keyClass = keyClass;
      this.valueClass = valueClass;
    }

    public <K, V> GBMap<K, V> getMap(final Class<K> keyClass, final Class<V> valueClass) {
      if(!keyClass.isAssignableFrom(this.keyClass) || !valueClass.isAssignableFrom(this.valueClass)) {
        throw new IllegalArgumentException("Classes don't match!");
      }
      return map;
    }
  }

}
