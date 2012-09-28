package com.tc.gbapi;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.tc.gbapi.impl.GBOnHeapMapConfig;
import com.tc.gbapi.impl.GBOnHeapMapFactory;
import com.tc.gbapi.impl.GBOnHeapMapImpl;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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
    manager.attachMap("whatever!", new GBOnHeapMapImpl<Object, Object>());
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
}
