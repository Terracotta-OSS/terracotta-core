/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

public class LocksDiffNamespaceTest extends AbstractToolkitTestBase {

  public LocksDiffNamespaceTest(TestConfig testConfig) {
    super(testConfig, LocksDiffNamespaceTestClient.class);
  }

  public static class LocksDiffNamespaceTestClient extends ClientBase {

    public LocksDiffNamespaceTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      String tcUrl = getTerracottaUrl();
      debug("Starting new client with tcUrl: " + tcUrl);
      Toolkit tk = ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl());

      final String name = "name";
      final ToolkitLock lockOne = tk.getLock(name);
      ToolkitReadWriteLock readWriteLock = tk.getReadWriteLock(name);
      final ToolkitLock lockTwo = readWriteLock.writeLock();
      Assert.assertEquals(name, lockOne.getName());
      Assert.assertEquals(name, readWriteLock.getName());
      Assert.assertNull(readWriteLock.readLock().getName());
      Assert.assertNull(readWriteLock.writeLock().getName());

      final AtomicBoolean one = new AtomicBoolean(false);
      final AtomicBoolean two = new AtomicBoolean(false);
      final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

      new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            System.out.println("Taking first lock...");
            lockOne.lock();
            System.out.println("Taking first lock... - done");
            one.set(true);
          } catch (Throwable e) {
            error.set(e);
          }
        }
      }).start();

      new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            System.out.println("Taking second lock...");
            lockTwo.lock();
            System.out.println("Taking second lock... - done");
            two.set(true);
          } catch (Throwable e) {
            error.set(e);
          }
        }
      }).start();

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          boolean oneStatus = one.get();
          boolean twoStatus = two.get();
          System.out.println("locked - one: " + oneStatus + ", two: " + twoStatus);
          return oneStatus && twoStatus;
        }
      });
    }

  }
}
