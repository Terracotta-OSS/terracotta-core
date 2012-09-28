package com.tc.gbapi;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  private final ConcurrentMap<String, GBMap<?, ?>> maps = new ConcurrentHashMap<String, GBMap<?, ?>>();
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
          if (maps.putIfAbsent(mapConfigEntry.getKey(), map) != null) {
            throw new IllegalStateException("Duplicated map for alias: " + mapConfigEntry.getKey());
          }
        }
        status = Status.STARTED;
      }
    }, null);
    new Thread(future).start();
    return future;
  }

  public <K, V> void attachMap(String name, GBMap<K, V> map) throws IllegalStateException {
    checkIsStarted();
    // depending on the GBManager implementation, could fail and throw an
    // IllegalStateException
  }

  public void detachMap(String name) {
    checkIsStarted();
    // Detaches the map from the object manager, compaction should clear
    // this maps contents from disk eventually.
  }

  public <K, V> GBMap<K, V> getMap(String name, Class<K> keyClass, Class<V> valueClass) {
    checkIsStarted();
    return (GBMap<K, V>)maps.get(name);
  }

  public void begin() {
    checkIsStarted();
  }

  public void commit() {
    checkIsStarted();
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

  ;

}
