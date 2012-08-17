package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.io.Serializable;

public class ForceTerminatingTestClient extends ClientBase {
  static final int                                  NUM_OF_ELEMENTS = 5000;
  private final ToolkitStore<String, Serializable> map;

  public ForceTerminatingTestClient(String[] args) {
    super(args);
    ToolkitStoreConfigBuilder clusteredMapConfig = new ToolkitStoreConfigBuilder();
    clusteredMapConfig.consistency(Consistency.SYNCHRONOUS_STRONG);
    this.map = getClusteringToolkit().getStore("testMap", clusteredMapConfig.build(), null);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    for (int i = 0; i < NUM_OF_ELEMENTS; i++) {
      this.map.put(String.valueOf(i), new MyObject());
    }
    System.out.println("[PASS: " + getClass().getName() + "]");
    System.err.println(this + " killed forceably");
    Runtime.getRuntime().halt(0);
  }

  private static class MyObject implements Serializable {
    //
  }

}