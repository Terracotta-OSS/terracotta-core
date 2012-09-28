package com.tc.gbapi.impl;

import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapConfig;
import com.tc.gbapi.GBMapFactory;
import com.tc.gbapi.GBMapMutationListener;

import java.util.Collection;
import java.util.List;

/**
 * @author Alex Snaps
 */
public class GBOnHeapMapFactory implements GBMapFactory {
  @Override
  public <K, V> GBMap<K, V> createMap(final GBMapConfig<K, V> config, final Object ... configs) {

    List<? extends GBMapMutationListener<K, V>> mutationListeners = null;

    if(config != null) {
      mutationListeners = config.getMutationListeners();
    }

    return new GBOnHeapMapImpl<K, V>(mutationListeners);
  }
}
