/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.junit.Assert;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public abstract class AbstractToolkitStoreIteratorClient extends ClientBase {
  private static final Random random = new Random();

  public AbstractToolkitStoreIteratorClient(String[] args) {
    super(args);
  }

  public abstract Consistency getConsistency();

  private static int getNextRandom() {
    return random.nextInt(300);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    final ToolkitStore<String, String> store = toolkit.getStore("MySampleStore", String.class);

    for (int i = 0; i < 10; i++) {
      Thread randomPutRemover = new Thread(new Runnable() {

        @Override
        public void run() {
          while (true) {
            int number = getNextRandom();
            store.put(String.valueOf(number), String.valueOf(number));
            number = getNextRandom();
            store.remove(String.valueOf(number));
          }
        }
      });
      randomPutRemover.setDaemon(true);
      randomPutRemover.start();
    }
    long startTime = System.nanoTime();
    long test_duration = TimeUnit.MINUTES.toNanos(8);
    while (System.nanoTime() - startTime < test_duration) {
      for (String s : store.keySet()) {
        Assert.assertNotNull(s);
      }
      for (String s : store.values()) {
        Assert.assertNotNull(s);
      }
      for (Entry<String, String> entry : store.entrySet()) {
        Assert.assertNotNull(entry.getKey());
        Assert.assertNotNull(entry.getValue());
        Assert.assertEquals(entry.getKey(), entry.getValue());
      }
    }
  }

}