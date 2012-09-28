package com.tc.gbapi;

import org.junit.Before;
import org.junit.Test;

import com.tc.gbapi.impl.GBOnHeapMapConfig;
import com.tc.gbapi.impl.GBOnHeapMapFactory;
import com.tc.gbapi.impl.GBOnHeapMapImpl;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class GBManagerTest {

  private GBManager manager;

  @Before
  public void setup() {
    manager = new GBManager(null, new GBOnHeapMapFactory());
  }

  @Test(expected = IllegalStateException.class)
  public void testCantAttachMapIfNotStarted() {
    manager.attachMap("whatever!", new GBOnHeapMapImpl<Object, Object>(), Object.class, Object.class);
  }

  @Test(expected = IllegalStateException.class)
  public void testCantAccessMapIfNotStarted() {
    manager.getMap("whatever!", Object.class, Object.class);
  }

  @Test
  public void testReturnNullWhenNotAttached() throws ExecutionException, InterruptedException {
    manager.start().get();
    assertThat(manager.getMap("whatever!", Object.class, Object.class), nullValue());
  }

  @Test
  public void testReturnsMapWhenConfigured() throws ExecutionException, InterruptedException {
    manager.getConfiguration().mapConfig().put("foo", new GBOnHeapMapConfig<String, String>(String.class, String.class));
    manager.start().get();
    assertThat(manager.getMap("foo", String.class, String.class), notNullValue());
  }

  @Test
  public void testThrowsWhenStopped() throws ExecutionException, InterruptedException {
    manager.getConfiguration().mapConfig().put("foo", new GBOnHeapMapConfig<String, String>(String.class, String.class));
    manager.start().get();
    assertThat(manager.getMap("foo", String.class, String.class), notNullValue());
    manager.shutdown();
    try {
      manager.getMap("foo", String.class, String.class);
      fail();
    } catch (IllegalStateException e) {
      // expected!
    }
  }

  @Test
  public void testThrowsWhenTypeNotAssignable() throws ExecutionException, InterruptedException {
    manager.getConfiguration().mapConfig().put("foo", new GBOnHeapMapConfig<Long, Integer>(Long.class, Integer.class));
    manager.start().get();
    assertThat(manager.getMap("foo", Long.class, Integer.class), notNullValue());
    assertThat(manager.getMap("foo", Number.class, Number.class), notNullValue());
    try {
      manager.getMap("foo", Integer.class, Long.class);
      fail();
    } catch (IllegalArgumentException e) {
      // expected!
    }
  }
}
