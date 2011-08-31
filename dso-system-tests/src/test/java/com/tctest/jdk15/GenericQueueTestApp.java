/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.GenericTransparentApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GenericQueueTestApp extends GenericTransparentApp {
  private final CyclicBarrier localBarrier = new CyclicBarrier(2);

  public GenericQueueTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, LinkedBlockingQueue.class);
  }

  protected Object getTestObject(String testName) {
    List lists = (List) sharedMap.get("queues");
    return lists.iterator();
  }

  protected void setupTestObject(String testName) {
    List lists = new ArrayList();
    lists.add(new LinkedBlockingQueue());
    lists.add(new LinkedBlockingQueue(5));

    sharedMap.put("queues", lists);
    sharedMap.put("arrayForBlockingQueue", new Object[4]);
    sharedMap.put("arrayForBlockingQueue2", new Object[4]);
    sharedMap.put("collectionForBlockingQueue", new ArrayList());
    sharedMap.put("collectionForBlockingQueue2", new ArrayList());
  }

  void testBasicAdd(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element" }), queue);
    } else {
      queue.add("First element");
      queue.add("Second element");
    }
  }
  
  void testAddAll(LinkedBlockingQueue queue, boolean validate) {
    List toAdd = new ArrayList();
    toAdd.add("First element");
    toAdd.add("Second element");
    if (validate) {
      assertQueueEqual(toAdd, queue);
    } else {
      queue.addAll(toAdd);
    }
  }
  
  void testElement(LinkedBlockingQueue queue, boolean validate) {
    List toAdd = new ArrayList();
    toAdd.add("First element");
    toAdd.add("Second element");
    if (validate) {
      Assert.assertEquals(toAdd.get(0), queue.element());
    } else {
      queue.addAll(toAdd);
    }
  }
  
  void testContains(LinkedBlockingQueue queue, boolean validate) {
    List toAdd = new ArrayList();
    toAdd.add("First element");
    toAdd.add("Second element");
    if (validate) {
      queue.contains(toAdd.get(1));
    } else {
      queue.addAll(toAdd);
    }
  }
  
  void testContainsAll(LinkedBlockingQueue queue, boolean validate) {
    List toAdd = new ArrayList();
    toAdd.add("First element");
    toAdd.add("Second element");
    if (validate) {
      Assert.assertFalse(queue.isEmpty());
      queue.containsAll(toAdd);
    } else {
      Assert.assertTrue(queue.isEmpty());
      queue.addAll(toAdd);
      Assert.assertFalse(queue.isEmpty());
    }
  }
  
  void testBasicPut(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertSingleElement("First element", queue);
    } else {
      try {
        queue.put("First element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  void testPut(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  void testClear(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertEmptyQueue(queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      queue.clear();
    }
  }

  void testDrainTo1(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertEmptyQueue(queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      List list = new ArrayList();
      queue.drainTo(list);
      assertCollectionEqual(Arrays.asList(new Object[] { "First element", "Second element" }), list);
    }
  }

  void testDrainToWithSharedCollection1(LinkedBlockingQueue queue, boolean validate) {
    Collection collection = getCollection(queue);
    if (validate) {
      assertEmptyQueue(queue);
      assertCollectionEqual(Arrays.asList(new Object[] { "First element", "Second element" }), collection);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      synchronized (collection) {
        queue.drainTo(collection);
      }
    }
  }

  void testDrainTo2(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "Third element", "Fourth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      List list = new ArrayList();
      queue.drainTo(list, 2);
      assertCollectionEqual(Arrays.asList(new Object[] { "First element", "Second element" }), list);
    }
  }

  void testDrainToWithSharedCollection2(LinkedBlockingQueue queue, boolean validate) {
    Collection collection = getCollection(queue);
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "Third element", "Fourth element" }), queue);
      assertCollectionEqual(Arrays.asList(new Object[] { "First element", "Second element" }), collection);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      synchronized (collection) {
        queue.drainTo(collection, 2);
      }
    }
  }

  void testOffer(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element", "Fifth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      queue.offer("Fifth element");
    }
  }

  void testOfferFull(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element", "Fifth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
        queue.put("Fifth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      if (queue.remainingCapacity() == 0) {
        queue.offer("Sixth element");
      }
    }
  }

  void testOfferTimeout1(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element", "Fifth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
        queue.put("Fifth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      if (queue.remainingCapacity() == 0) {
        try {
          queue.offer("Sixth element", 10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
  }

  void testOfferTimeout2(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "Second element", "Third element", "Fourth element",
          "Fifth element", "Sixth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
        queue.put("Fifth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      Thread thread = new Thread(new QueueReader(queue, localBarrier));
      thread.start();
      try {
        queue.offer("Sixth element", 100, TimeUnit.SECONDS);
        localBarrier.await();
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      } 
    }
  }

  void testPeek(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element", "Fifth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
        queue.put("Fifth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      Assert.assertEquals("First element", queue.peek());
    }
  }

  void testPoll(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "Second element", "Third element", "Fourth element",
          "Fifth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
        queue.put("Fifth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      Assert.assertEquals("First element", queue.poll());
    }
  }

  void testPollTimeout1(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "Second element", "Third element", "Fourth element",
          "Fifth element" }), queue);
    } else {
      try {
        Assert.assertEquals(null, queue.poll(10, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
        queue.put("Fifth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      Assert.assertEquals("First element", queue.poll());
    }
  }

  void testPollTimeout2(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertEmptyQueue(queue);
    } else {
      Thread thread = new Thread(new QueuePutter(queue, localBarrier));
      thread.start();
      try {
        localBarrier.await();
        Assert.assertEquals("New element", queue.poll(100, TimeUnit.SECONDS));
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  void testTake(LinkedBlockingQueue queue, boolean validate) {
    if (!validate) {
      Thread thread = new Thread(new QueuePutter(queue, localBarrier));
      thread.start();

      Object queueElement = null;
      try {
        localBarrier.await();
        queueElement = queue.take();
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }

      Assert.assertEquals("New element", queueElement);
    }
  }

  void testRemove1(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "Second element", "Third element", "Fourth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      queue.remove();
    }
  }

  void testRemove2(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Fourth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      queue.remove("Third element");
    }
  }

  void testRemove3(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      queue.remove("Fifth element");
    }
  }
  
  void testRemoveAll(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Fourth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      List toRemove = new ArrayList();
      toRemove.add("Second element");
      toRemove.add("Third element");
      queue.removeAll(toRemove);
    }
  }

  void testToArray1(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      Object[] array = queue.toArray();
      assertQueueEqual(Arrays.asList(array), queue);
    }
  }

  void testToArray2(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      Object[] array = new Object[queue.size()];
      Object[] array2 = queue.toArray(array);
      Assert.assertTrue(array == array2);
      assertQueueEqual(Arrays.asList(array), queue);
    }
  }

  void testToArrayWithSharedArray(LinkedBlockingQueue queue, boolean validate) {
    Object[] array = getArray(queue);
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Second element", "Third element",
          "Fourth element" }), queue);
      assertQueueEqual(Arrays.asList(array), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
        queue.put("Fourth element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }

      // Since the implementation of queue.toArray() is protected by an ReentrantLock, no
      // synchronization is needed on the array object.
      Object[] array2 = null;
      array2 = queue.toArray(array);
      Assert.assertTrue(array == array2);
    }
  }

  // Read Only test.
  void testReadOnlyDrainTo(LinkedBlockingQueue queue, boolean validate) {
    Collection collection = getCollection(queue);
    if (validate) {
      assertEmptyQueue(queue);
      Assert.assertEquals(0, collection.size());
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      synchronized (collection) {
        try {
          queue.drainTo(collection);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException t) {
          // Expected
        }
      }
    }
  }

  void testIteratorRemove(LinkedBlockingQueue queue, boolean validate) {
    if (validate) {
      assertQueueEqual(Arrays.asList(new Object[] { "First element", "Third element" }), queue);
    } else {
      try {
        queue.put("First element");
        queue.put("Second element");
        queue.put("Third element");
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      Iterator itr = queue.iterator();
      itr.next();
      itr.next();
      synchronized (itr) {
        itr.remove();
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GenericQueueTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("localBarrier", "localBarrier");
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
  }

  private Collection getCollection(LinkedBlockingQueue queue) {
    if (queue.remainingCapacity() == 5) {
      return (Collection) sharedMap.get("collectionForBlockingQueue2");
    } else {
      return (Collection) sharedMap.get("collectionForBlockingQueue");
    }
  }

  private Object[] getArray(LinkedBlockingQueue queue) {
    if (queue.remainingCapacity() == 5) {
      return (Object[]) sharedMap.get("arrayForBlockingQueue2");
    } else {
      return (Object[]) sharedMap.get("arrayForBlockingQueue");
    }
  }

  private void assertEmptyQueue(LinkedBlockingQueue queue) {
    Assert.assertEquals(0, queue.size());
  }

  private void assertSingleElement(Object expected, LinkedBlockingQueue queue) {
    Assert.assertEquals(1, queue.size());
    Assert.assertEquals(expected, queue.peek());
  }

  private void assertCollectionEqual(Collection expect, Collection actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Assert.assertTrue(expect.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expect));
  }

  private void assertQueueEqual(List expect, LinkedBlockingQueue actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Assert.assertTrue(expect.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expect));

    for (Iterator iExpect = expect.iterator(), iActual = actual.iterator(); iExpect.hasNext();) {
      Assert.assertEquals(iExpect.next(), iActual.next());
    }

    if (expect.isEmpty()) {
      Assert.assertTrue(actual.isEmpty());
    } else {
      Assert.assertFalse(actual.isEmpty());
    }
  }

  private static class QueuePutter implements Runnable {
    private LinkedBlockingQueue queue;
    private CyclicBarrier barrier;

    public QueuePutter(LinkedBlockingQueue queue, CyclicBarrier barrier) {
      this.queue = queue;
      this.barrier = barrier;
    }

    public void run() {
      try {
        queue.put("New element");
        barrier.await();
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  private static class QueueReader implements Runnable {
    private LinkedBlockingQueue queue;
    private CyclicBarrier barrier;

    public QueueReader(LinkedBlockingQueue queue, CyclicBarrier barrier) {
      this.queue = queue;
      this.barrier = barrier;
    }

    public void run() {
      queue.poll();
      try {
        barrier.await();
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }
  }
}
