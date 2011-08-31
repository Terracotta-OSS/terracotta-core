/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class NamedLockUpgradeFailureTestApp extends AbstractErrorCatchingTransparentApp {
  private CyclicBarrier        barrier;
  private NameLockUpgradeClass root = new NameLockUpgradeClass();

  public NamedLockUpgradeFailureTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  public void runTest() throws Throwable {
    int index = barrier.barrier();

    testMethodReturnObject(index);

    testMethodReturnInt(index);

    testMethodReturnLargeValue(index);

    testMethodVoid(index);
  }

  private void testMethodReturnObject(int index) throws Throwable {
    testNameLockWithinSingleNodeReturnObject(index);
  }

  private void testMethodReturnInt(int index) throws Throwable {
    testNameLockWithinSingleNodeReturnInt(index);
  }

  private void testMethodReturnLargeValue(int index) throws Throwable {
    testNameLockWithinSingleNodeReturnLong(index);
    
    testNameLockWithinSingleNodeReturnDouble(index);
  }

  private void testMethodVoid(int index) throws Throwable {
    testNameLockWithMultiNodes(index);

    testNameLockWithinSingleNode(index);
  }

  private void testNameLockWithMultiNodes(int index) throws Throwable {
    testNameLockUpgradeWithMultiNodes(index);
    testNameLockThrowExceptionWithMultiNodes(index);
    testNameLockUpgradeOnDifferentLocks1WithMultiNodes(index);
    testNameLockUpgradeOnDifferentLocks2WithMultiNodes(index);
  }

  private void testNameLockWithinSingleNode(int index) throws Throwable {
    testNameLockThrowException(index);

    testNameLockUpgrade(index);

    testNameLockUpgradeOnDifferentLocks1(index);

    testNameLockUpgradeOnDifferentLocks2(index);
  }

  private void testNameLockWithinSingleNodeReturnObject(int index) throws Throwable {
    testNameLockUpgradeReturnObject(index);

    testNameLockThrowExceptionReturnObject(index);

    testNameLockUpgradeOnDifferentLocks1ReturnObject(index);

    testNameLockUpgradeOnDifferentLocks2ReturnObject(index);
  }

  private void testNameLockWithinSingleNodeReturnInt(int index) throws Throwable {
    testNameLockUpgradeReturnInt(index);

    testNameLockThrowExceptionReturnInt(index);

    testNameLockUpgradeOnDifferentLocks1ReturnInt(index);

    testNameLockUpgradeOnDifferentLocks2ReturnInt(index);
  }

  private void testNameLockWithinSingleNodeReturnLong(int index) throws Throwable {
    testNameLockUpgradeReturnLong(index);
  }
  
  private void testNameLockWithinSingleNodeReturnDouble(int index) throws Throwable {
    testNameLockUpgradeReturnDouble(index);
  }
  
  private void testNameLockUpgradeReturnDouble(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            Assert.assertEquals(Double.MAX_VALUE, root.doubleWrite());
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.doubleReadWrite(localBarrier);
        throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
      } catch (TCLockUpgradeNotSupportedError e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeReturnLong(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            Assert.assertEquals(Long.MAX_VALUE, root.longWrite());
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.longReadWrite(localBarrier);
        throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
      } catch (TCLockUpgradeNotSupportedError e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeReturnInt(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            Assert.assertEquals(2, root.intWrite());
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.intReadWrite(localBarrier);
        throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
      } catch (TCLockUpgradeNotSupportedError e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeReturnObject(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.objWrite();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.objReadWrite(localBarrier);
        throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
      } catch (TCLockUpgradeNotSupportedError e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();
  }

  private void testNameLockThrowExceptionWithMultiNodes(int index) throws Throwable {
    if (index == 0) {
      barrier.barrier();
      root.doWrite();
      barrier.barrier();
    } else if (index == 1) {
      try {
        root.doThrowException(barrier);
      } catch (RuntimeException e) {
        barrier.barrier();
      }
    }

    barrier.barrier();

  }

  private void testNameLockThrowException(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);

      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.doWrite();
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.doThrowException(localBarrier);
      } catch (RuntimeException e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();

  }

  private void testNameLockThrowExceptionReturnInt(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);

      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            Assert.assertEquals(2, root.intWrite());
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.intThrowException(localBarrier);
      } catch (RuntimeException e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();

  }

  private void testNameLockThrowExceptionReturnObject(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);

      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.objWrite();
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.objThrowException(localBarrier);
      } catch (RuntimeException e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();

  }

  private void testNameLockUpgrade(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.doWrite();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      try {
        root.doReadWrite(localBarrier);
        throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
      } catch (TCLockUpgradeNotSupportedError e) {
        localBarrier.barrier();
      }
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeWithMultiNodes(int index) throws Throwable {
    if (index == 0) {
      barrier.barrier();
      root.doWrite();
    } else if (index == 1) {
      try {
        root.doReadWrite(barrier);
        throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
      } catch (TCLockUpgradeNotSupportedError e) {
        barrier.barrier();
      }
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks1WithMultiNodes(int index) throws Throwable {
    if (index == 0) {
      barrier.barrier();
      root.doWrite();
      barrier.barrier();
    } else if (index == 1) {
      root.doReadWriteOnTwoNameLock(barrier);
      barrier.barrier();
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks1(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.doWrite();
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      root.doReadWriteOnTwoNameLock(localBarrier);
      localBarrier.barrier();
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks1ReturnInt(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            Assert.assertEquals(2, root.intWrite());
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      Assert.assertEquals(3, root.intReadWriteOnTwoNameLock(localBarrier));
      localBarrier.barrier();
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks1ReturnObject(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.objWrite();
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      root.objReadWriteOnTwoNameLock(localBarrier);
      localBarrier.barrier();
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks2WithMultiNodes(int index) throws Throwable {
    if (index == 0) {
      barrier.barrier();
      root.doWriteWithNameLock1();
      barrier.barrier();
    } else if (index == 1) {
      root.doReadWriteOnTwoNameLock(barrier);
      barrier.barrier();
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks2(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.doWriteWithNameLock1();
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      root.doReadWriteOnTwoNameLock(localBarrier);
      localBarrier.barrier();
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks2ReturnInt(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            Assert.assertEquals(4, root.intWriteWithNameLock1());
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      Assert.assertEquals(3, root.intReadWriteOnTwoNameLock(localBarrier));
      localBarrier.barrier();
    }

    barrier.barrier();
  }

  private void testNameLockUpgradeOnDifferentLocks2ReturnObject(int index) throws Throwable {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.barrier();
            root.objWriteWithNameLock1();
            localBarrier.barrier();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t.start();
      root.objReadWriteOnTwoNameLock(localBarrier);
      localBarrier.barrier();
    }

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NamedLockUpgradeFailureTestApp.class.getName();

    config.getOrCreateSpec(testClass) //
        .addRoot("barrier", "barrier") //
        .addRoot("root", "root");

    config.getOrCreateSpec(NameLockUpgradeClass.class.getName());

    String methodExpression = "* " + testClass + "$NameLockUpgradeClass.doReadWrite(..)";
    LockDefinition definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.intReadWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.objReadWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.longReadWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);
    
    methodExpression = "* " + testClass + "$NameLockUpgradeClass.doubleReadWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.doThrowException(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.intThrowException(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.objThrowException(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.doWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.intWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.objWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.longWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    
    methodExpression = "* " + testClass + "$NameLockUpgradeClass.doubleWrite(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.doWriteWithNameLock1(..)";
    definition = new LockDefinitionImpl("nameLock1", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.intWriteWithNameLock1(..)";
    definition = new LockDefinitionImpl("nameLock1", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.objWriteWithNameLock1(..)";
    definition = new LockDefinitionImpl("nameLock1", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.doReadWriteOnTwoNameLock(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock1", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.intReadWriteOnTwoNameLock(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock1", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "$NameLockUpgradeClass.objReadWriteOnTwoNameLock(..)";
    definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);
    definition = new LockDefinitionImpl("nameLock1", ConfigLockLevel.READ);
    definition.commit();
    config.addLock(methodExpression, definition);

    new CyclicBarrierSpec().visit(visitor, config);

  }

  private static class NameLockUpgradeClass {
    public NameLockUpgradeClass() {
      super();
    }

    public void doReadWrite(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level
      System.err.println("In doReadWrite");
    }

    public void doWrite() {
      // This method will get a name lock with write level only
      System.err.println("In doWrite");
    }

    public void doWriteWithNameLock1() {
      // This method will get a name lock with write level only
      System.err.println("In doWriteWithNameLock1");
    }

    public void doThrowException(CyclicBarrier barrier) throws Throwable {
      barrier.barrier();
      Thread.sleep(2000);
      throw new RuntimeException("Test exception");
    }

    public void doReadWriteOnTwoNameLock(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level, but on two different name locks
      barrier.barrier();
      Thread.sleep(2000);
      System.err.println("In doReadWriteOnTwoNameLock");
    }

    public int intReadWrite(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level
      System.err.println("In intReadWrite");
      return 2;
    }

    public long longReadWrite(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level
      System.err.println("In longReadWrite");
      return Long.MAX_VALUE;
    }

    public double doubleReadWrite(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level
      System.err.println("In doubleReadWrite");
      return Double.MAX_VALUE;
    }

    public Object objReadWrite(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level
      System.err.println("In objReadWrite");
      return new Object();
    }

    public int intWrite() {
      // This method will get a name lock with write level only
      System.err.println("In intWrite");
      return 2;
    }

    public long longWrite() {
      System.err.println("In longWrite");
      return Long.MAX_VALUE;
    }

    public double doubleWrite() {
      System.err.println("In doubleWrite");
      return Double.MAX_VALUE;
    }

    public Object objWrite() {
      // This method will get a name lock with write level only
      System.err.println("In objWrite");
      return new Object();
    }

    public int intThrowException(CyclicBarrier barrier) throws Throwable {
      barrier.barrier();
      Thread.sleep(2000);
      throw new RuntimeException("Test exception");
    }

    public Object objThrowException(CyclicBarrier barrier) throws Throwable {
      barrier.barrier();
      Thread.sleep(2000);
      throw new RuntimeException("Test exception");
    }

    public int intReadWriteOnTwoNameLock(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level, but on two different name locks
      barrier.barrier();
      Thread.sleep(2000);
      System.err.println("In intReadWriteOnTwoNameLock");
      return 3;
    }

    public Object objReadWriteOnTwoNameLock(CyclicBarrier barrier) throws Throwable {
      // This method should get a name lock with read level and then write level, but on two different name locks
      barrier.barrier();
      Thread.sleep(2000);
      System.err.println("In objReadWriteOnTwoNameLock");
      return new Object();
    }

    public int intWriteWithNameLock1() {
      // This method will get a name lock with write level only
      System.err.println("In intWriteWithNameLock1");
      return 4;
    }

    public Object objWriteWithNameLock1() {
      // This method will get a name lock with write level only
      System.err.println("In objWriteWithNameLock1");
      return new Object();
    }
  }

}
