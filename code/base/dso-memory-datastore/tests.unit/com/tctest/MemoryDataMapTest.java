/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.TCRuntimeException;
import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.memorydatastore.message.TCByteArrayKeyValuePair;
import com.tc.memorydatastore.server.MemoryDataStoreServer;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class MemoryDataMapTest extends TestCase {
  private MemoryDataStoreServer server = null;

  protected void setUp() throws Exception {
    super.setUp();
    server = MemoryDataStoreServer.createInstance(0);
    server.start();
    System.err.println("Server started on port " + server.getListenPort());
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    server.shutdown();
    server = null;
    Thread.sleep(5000);
  }

  public void testPerf() throws Exception {
    int numOfPut = 5000;
    MemoryDataStoreClient client = new MemoryDataStoreClient("storePerf", "localhost", server.getListenPort());
    for (int i=0; i<numOfPut; i++) {
      String key = "key"+i;
      String value = "value"+i;
      client.put(key.getBytes(), value.getBytes());
      if (i%100 == 0) {
        Thread.sleep(1);
      }
      if (i%100000 == 0) {
        System.err.println("Send out: " + i + " requests");
      }
    }
    long start = System.currentTimeMillis();
    Object o = client.get("key10".getBytes());
    Assert.assertNotNull(o);
    long end = System.currentTimeMillis();
    System.err.println("Time to get 1 item: " + (end-start) + "ms");

    byte[] bytes = new byte[]{107, 101};
    start = System.currentTimeMillis();
    Collection allValues = client.getAll(bytes);
    end = System.currentTimeMillis();
    Assert.assertEquals(numOfPut, allValues.size());
    System.err.println("Time to get " + numOfPut + " items: " + (end-start) + "ms");

    client.close();
  }

  public void testBasic() throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(16);

    MemoryDataStoreClient client1 = new MemoryDataStoreClient("store1", "localhost", server.getListenPort());
    MemoryDataStoreClient client2 = new MemoryDataStoreClient("store2", "localhost", server.getListenPort());

    Runnable[] putClients = new Runnable[15];
    putClients[0] = new TestPutClient(barrier, client1, "key1".getBytes(), "value1".getBytes());
    putClients[1] = new TestPutClient(barrier, client1, "key2".getBytes(), "value2".getBytes());
    putClients[2] = new TestPutClient(barrier, client1, "key3".getBytes(), "value3".getBytes());
    putClients[3] = new TestPutClient(barrier, client1, "key4".getBytes(), "value4".getBytes());
    putClients[4] = new TestPutClient(barrier, client1, "key5".getBytes(), "value5".getBytes());
    putClients[5] = new TestPutClient(barrier, client1, "key6".getBytes(), "value6".getBytes());
    putClients[6] = new TestPutClient(barrier, client1, "key7".getBytes(), "value7".getBytes());
    putClients[7] = new TestPutClient(barrier, client1, "key8".getBytes(), "value8".getBytes());
    putClients[8] = new TestPutClient(barrier, client1, "key9".getBytes(), "value9".getBytes());
    putClients[9] = new TestPutClient(barrier, client1, "key10".getBytes(), "value10".getBytes());
    putClients[10] = new TestPutClient(barrier, client2, "key1".getBytes(), "value1".getBytes());
    putClients[11] = new TestPutClient(barrier, client2, "key2".getBytes(), "value2".getBytes());
    putClients[12] = new TestPutClient(barrier, client2, "key3".getBytes(), "value3".getBytes());
    putClients[13] = new TestPutClient(barrier, client2, "key4".getBytes(), "value4".getBytes());
    putClients[14] = new TestPutClient(barrier, client2, "key5".getBytes(), "value5".getBytes());

    runAllClients(putClients);

    barrier.barrier();

    Runnable[] getClients = new Runnable[17];
    barrier = new CyclicBarrier(18);

    getClients[0] = new TestGetClient(barrier, client1, "key1".getBytes(), "value1".getBytes());
    getClients[1] = new TestGetClient(barrier, client1, "key2".getBytes(), "value2".getBytes());
    getClients[2] = new TestGetClient(barrier, client1, "key3".getBytes(), "value3".getBytes());
    getClients[3] = new TestGetClient(barrier, client1, "key4".getBytes(), "value4".getBytes());
    getClients[4] = new TestGetClient(barrier, client1, "key5".getBytes(), "value5".getBytes());
    getClients[5] = new TestGetClient(barrier, client1, "key6".getBytes(), "value6".getBytes());
    getClients[6] = new TestGetClient(barrier, client1, "key7".getBytes(), "value7".getBytes());
    getClients[7] = new TestGetClient(barrier, client1, "key8".getBytes(), "value8".getBytes());
    getClients[8] = new TestGetClient(barrier, client1, "key9".getBytes(), "value9".getBytes());
    getClients[9] = new TestGetClient(barrier, client1, "key10".getBytes(), "value10".getBytes());
    getClients[10] = new TestGetClient(barrier, client2, "key1".getBytes(), "value1".getBytes());
    getClients[11] = new TestGetClient(barrier, client2, "key2".getBytes(), "value2".getBytes());
    getClients[12] = new TestGetClient(barrier, client2, "key3".getBytes(), "value3".getBytes());
    getClients[13] = new TestGetClient(barrier, client2, "key4".getBytes(), "value4".getBytes());
    getClients[14] = new TestGetClient(barrier, client2, "key5".getBytes(), "value5".getBytes());
    getClients[15] = new TestGetClient(barrier, client2, "key6".getBytes(), null);
    getClients[16] = new TestGetClient(barrier, client1, "key11".getBytes(), null);

    runAllClients(getClients);

    barrier.barrier();

    client1.close();
    client2.close();

    MemoryDataStoreClient client = new MemoryDataStoreClient("store2", "localhost", server.getListenPort());
    byte[] bytes = new byte[]{107, 101};
    Collection allValues = client.getAll(bytes);
    System.err.println("Getting all of [107 101]:");
    printByteArrayCollection(allValues);

    client = new MemoryDataStoreClient("store1", "localhost", server.getListenPort());
    client.remove("key1".getBytes());

    Object o = client.get("key1".getBytes());
    Assert.assertNull(o);

    bytes = new byte[]{107, 101};
    client.removeAll(bytes);

    o = client.get("key5".getBytes());
    Assert.assertNull(o);

  }

  private void printByteArrayCollection(Collection allValues) {
    int size = allValues.size();

    System.err.println("No of values: " + size);
    for (Iterator i=allValues.iterator(); i.hasNext(); ) {
      TCByteArrayKeyValuePair keyValuePair = (TCByteArrayKeyValuePair)i.next();

      byte[] key = keyValuePair.getKey();
      byte[] value = keyValuePair.getValue();

      System.err.print("key: [");
      for (int j=0; j<key.length; j++) {
        System.err.print(key[j]);
        System.err.print(" ");
      }
      System.err.print("], value: [");
      for (int j=0; j<value.length; j++) {
        System.err.print(value[j]);
        System.err.print(" ");
      }
      System.err.println("]");
    }
  }

  private static void runAllClients(Runnable[] runnableClients) {
    Thread[] allClients = new Thread[runnableClients.length];
    for (int i=0; i<runnableClients.length; i++) {
      allClients[i] = new Thread(runnableClients[i]);
      allClients[i].start();
    }
  }

  private static class TestGetClient implements Runnable {
    private final CyclicBarrier barrier;
    private final byte[] key;
    private final byte[] expectedValue;
    private final MemoryDataStoreClient client;

    public TestGetClient(CyclicBarrier barrier, MemoryDataStoreClient client, byte[] key, byte[] expectedValue) {
      this.barrier = barrier;
      this.key = key;
      this.expectedValue = expectedValue;
      this.client = client;
    }

    public void run() {
      byte[] value = client.get(key);
      Assert.assertEquals(expectedValue, value);

      try {
        barrier.barrier();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      } catch (BrokenBarrierException e) {
        throw new TCRuntimeException(e);
      }
      System.err.println("Finish running get client");
    }
  }

  private static class TestPutClient implements Runnable {
    private final CyclicBarrier barrier;
    private final byte[] key;
    private final byte[] value;
    private final MemoryDataStoreClient client;

    public TestPutClient(CyclicBarrier barrier, MemoryDataStoreClient client, byte[] key, byte[] value) {
      this.barrier = barrier;
      this.key = key;
      this.value = value;
      this.client = client;
    }

    public void run() {
      client.put(key, value);

      try {
        barrier.barrier();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      } catch (BrokenBarrierException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

}
