package com.tc.gbapi.impl;

import org.junit.Test;

import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBRetriever;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class GBOnHeapMapImplTest {

  @Test
  public void testPutStoresValue() {
    GBMap<Integer, String> map = new GBOnHeapMapImpl<Integer, String>();
    final int maxKey = 1000;
    for(int i = 0; i < maxKey; i++) {
      map.put(i, Integer.toHexString(i));
    }
    for(int i = 0; i < maxKey; i++) {
      assertThat(map.get(i), equalTo(Integer.toHexString(i)));
    }
  }

  @Test
  public void testNotifiesListenersOnAddAndRemove() {

    final IntegerStringGBMapMutationListener<Integer, String> mapMutationListener = new IntegerStringGBMapMutationListener<Integer, String>();
    GBMap<Integer, String> map = new GBOnHeapMapImpl<Integer, String>(Collections.singletonList(mapMutationListener));
    final int maxKey = 1000;
    for(int i = 0; i < maxKey; i++) {
      map.put(i, Integer.toHexString(i));
    }
    assertThat(mapMutationListener.added.get(), is((long) maxKey));
    assertThat(mapMutationListener.removed.get(), is(0L));

    for(int i = 500; i < 500 + maxKey; i++) {
      map.remove(i);
    }

    assertThat(mapMutationListener.added.get(), is((long) maxKey));
    assertThat(mapMutationListener.removed.get(), is(500L));

  }

  private static class IntegerStringGBMapMutationListener<K, V> implements GBMapMutationListener<K, V> {

    final AtomicLong added = new AtomicLong();
    final AtomicLong removed = new AtomicLong();

    @Override
    public void removed(final GBRetriever<K> key, final GBRetriever<V> value, final Map<? extends Enum, Object> metadata) {
      if (value.retrieve() != null) {
        removed.incrementAndGet();
      }
    }

    @Override
    public void added(final GBRetriever<K> key, final GBRetriever<V> value, final Map<? extends Enum, Object> metadata) {
      added.incrementAndGet();
    }
  }
}
