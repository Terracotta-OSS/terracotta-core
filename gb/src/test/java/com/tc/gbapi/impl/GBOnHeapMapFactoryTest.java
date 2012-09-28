package com.tc.gbapi.impl;

import org.hamcrest.core.IsNull;
import org.junit.Test;

import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapFactory;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBRetriever;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class GBOnHeapMapFactoryTest {
  @Test
  public void testCreateMap() throws Exception {
    GBMapFactory factory = new GBOnHeapMapFactory();
    assertThat(factory.createMap(null), IsNull.notNullValue());
  }

  @Test
  public void testUsesRegisteredMutationListeners() {
    final AtomicBoolean invoked = new AtomicBoolean();
    GBMapFactory factory = new GBOnHeapMapFactory();
    final GBOnHeapMapConfig<Integer, String> mapConfig = new GBOnHeapMapConfig<Integer, String>(Integer.class, String.class);
    mapConfig.addListener(new GBMapMutationListener<Integer, String>() {
      @Override
      public void removed(final GBRetriever<Integer> key, final GBRetriever<String> value, final Map<? extends Enum, Object> metadata) {
        invoked.set(true);
      }

      @Override
      public void added(final GBRetriever<Integer> key, final GBRetriever<String> value, final Map<? extends Enum, Object> metadata) {
        invoked.set(true);
      }
    });
    final GBMap<Integer,String> map = factory.createMap(mapConfig);
    assertThat(map, IsNull.notNullValue());
    assertThat(invoked.get(), is(false));
    map.put(1, "one");
    assertThat(invoked.get(), is(true));
  }
}
