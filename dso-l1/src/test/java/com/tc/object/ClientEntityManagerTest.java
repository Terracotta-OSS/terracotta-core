/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.entity.MessageCodecSupplier;
import org.junit.Assert;
import org.junit.Test;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;

import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage.Acks;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.ProductID;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class ClientEntityManagerTest extends TestCase {
  private ClientMessageChannel channel;
  private ClientEntityManager manager;
  private StageManager stageMgr;
  
  private EntityID entityID;
  private ClientInstanceID instance;
  private EntityDescriptor descriptor;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void setUp() throws Exception {
    this.channel = mock(ClientMessageChannel.class);
    when(this.channel.getProductId()).thenReturn(ProductID.STRIPE);
    this.stageMgr = mock(StageManager.class);
    when(this.stageMgr.createStage(any(String.class), any(Class.class), any(EventHandler.class), anyInt(), anyInt())).then(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Stage stage = mock(Stage.class);
        when(stage.getSink()).thenReturn(new FakeSink((EventHandler)invocation.getArguments()[2]));
        return stage;
      }
    });
    when(this.stageMgr.getStage(any(String.class), any(Class.class))).then(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Stage stage = mock(Stage.class);
        when(stage.getSink()).thenReturn(new FakeSink(null));
        return stage;
      }
    });
    this.manager = new ClientEntityManagerImpl(this.channel, stageMgr);
    
    String entityClassName = "Class Name";
    String entityInstanceName = "Instance Name";
    this.entityID = new EntityID(entityClassName, entityInstanceName);
    this.instance = new ClientInstanceID(1);
    this.descriptor = EntityDescriptor.createDescriptorForInvoke(new FetchID(1), instance);
  }
 
  public void testResponseSinkFlush() throws Exception {
    
  }

  // Test that a simple lookup will succeed.
  public void testSimpleLookupSuccess() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    
    // Set the target for success.
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });    
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we found the entity.
    assertTrue(didFindEndpoint(fetcher));
  }
  
  // Test to make sure we can still receive items without error after close, needed due to shutdown sequence
  public void testReceiveAfterClose() throws Exception {
    TransactionID tid = new TransactionID(1L);
    manager.shutdown(false);
    manager.complete(tid);
    manager.failed(tid, new EntityException(this.entityID.getClassName(), this.entityID.getEntityName(), "", null) {});
    manager.received(tid);
    manager.retired(tid);
    // nothing should throw exception
  }  

  // Test that a simple lookup can fail.
  public void testSimpleLookupFailure() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    
    // Set the target for failure.
    final byte[] resultObject = null;
    final EntityException resultException = new EntityNotFoundException(null, null);
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });       
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we couldn't find the entity.
    assertFalse(didFindEndpoint(fetcher));
  }

  // Test pause will block progress but it the lookup will complete (failing to find the entity) after unpause.
  public void testLookupStalledByPause() throws Exception {
    final EntityException resultException = new EntityNotFoundException(null, null);
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, null, resultException, true);
      }
    });    
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    
    // Pause the manager before we start anything.
    this.manager.pause();
    // Now we can start the lookup thread as we expect it to stall on the paused state.
    fetcher.start();
    // Spin until we can observe that this thread is in the WAITING state.
    while (fetcher.getState() != Thread.State.WAITING) {
      ThreadUtil.reallySleep(1000);
    }
    // Now unpause to ensure it progresses.
    this.manager.unpause();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we couldn't find the entity.
    assertFalse(didFindEndpoint(fetcher));
  }
  
  // Test fetch+release on success.
  public void testFetchReleaseOnSuccess() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    
    // Set the target for success.
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });       
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we found the entity.
    assertTrue(didFindEndpoint(fetcher));
  }

  // Test fetch+release on failure.
  public void testFetchReleaseOnFailure() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    
    // Set the target for failure.
    final byte[] resultObject = null;
    final EntityException resultException = new EntityNotFoundException(null, null);
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });       
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we couldn't find the entity.
    assertFalse(didFindEndpoint(fetcher));
    
    // Now, release it and expect to see the exception thrown, directly (since we are accessing the manager, directly).
    boolean didRelease = false;
    try {
      fetcher.close();
      didRelease = true;
    } catch (RuntimeException e) {
      didRelease = false;
    }
    assertFalse(didRelease);
  }

  public void testFetchandAsyncRelease() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    
    // Set the target for success.
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });   
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    try {
      EntityClientEndpoint endpoint = fetcher.getResult();
    } catch (EntityNotFoundException not) {
      Assert.fail();
    }
    
    // Now, release it and expect to see the exception thrown, directly (since we are accessing the manager, directly).
    boolean didRelease = false;
    try {
      Future<Void> released = fetcher.release();
      released.get();
      didRelease = true;
    } catch (Exception e) {
      didRelease = false;
    }
    assertTrue(didRelease);
  }
  
  
  public void testShutdownCausesReleaseException() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    
    // Set the target for success.
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });   
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    try {
      EntityClientEndpoint endpoint = fetcher.getResult();
    } catch (EntityNotFoundException not) {
      Assert.fail();
    }
    
    // Now, release it and expect to see the exception thrown, directly (since we are accessing the manager, directly).
    boolean didRelease = false;
    this.manager.shutdown(false);
    try {
      Future<Void> released = fetcher.release();
      released.get();
      didRelease = true;
    } catch (ExecutionException e) {
      // Expected.
      didRelease = false;
    }
    assertFalse(didRelease);
  }
  
  // That that we can shut down while in a paused state without locking up.
  public void testShutdownWhilePaused() throws Exception {
    // We will create a runnable which will attempt to fetch the entity (and we will get this stuck in "WAITING" on pause).
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });
    // Pause the manager before we start anything.
    this.manager.pause();
    // Now we can start the lookup thread as we expect it to stall on the paused state.
    fetcher.start();
    // Spin until we can observe that this thread is in the WAITING state.
    while (fetcher.getState() != Thread.State.WAITING) {
      ThreadUtil.reallySleep(1000);
    }
    
    // Now, shut down the manager.
    this.manager.shutdown(false);
    // Join on the waiter thread since the shutdown should have released it to fail in the expected way.
    fetcher.join();
    
    // We are expecting a TCNotRunningException.
    try {
      fetcher.getResult();
      fail();
    } catch (ConnectionClosedException e) {
      // Expected.
    } catch (Throwable t) {
      // Unexpected.
      fail();
    }
  }
  
  public void testFullQueueTimesOut() throws Exception {
    // Set the target for success.
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });       
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    fetcher.start();
    fetcher.join();
    
    List<TestRequestBatchMessage> full = new ArrayList<>();
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        TestRequestBatchMessage msg = new TestRequestBatchMessage(manager, resultObject, resultException, false);
        full.add(msg);
        return msg;
      }
    });       
    try {
      EntityClientEndpoint endpoint = fetcher.getResult();
      for (int x=0;x<ClientConfigurationContext.MAX_PENDING_REQUESTS;x++) {
        endpoint.beginInvoke().invokeWithTimeout(5, TimeUnit.SECONDS);
      }
      endpoint.beginInvoke().invokeWithTimeout(5, TimeUnit.SECONDS);
      Assert.fail("Timeout expected");
    } catch (TimeoutException to) {
      // expected
    } catch (Exception e) {
      e.printStackTrace();
      // not expected
      throw e;
    } finally {
      full.stream().forEach(m->{
        manager.complete(m.getTransactionID(), new byte[0]);
        manager.retired(m.getTransactionID());
       });
    }
  }
    
  public void testInvokeWithTimeoutZeroWaitsForever() throws Exception {
    // Set the target for success.
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });       
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    fetcher.start();
    fetcher.join();
    List<TestRequestBatchMessage> full = new ArrayList<>();
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        TestRequestBatchMessage msg = new TestRequestBatchMessage(manager, resultObject, resultException, false);
        full.add(msg);
        return msg;
      }
    }); 
    Thread t = Thread.currentThread();
    new Thread(()->{
     try {
       TimeUnit.SECONDS.sleep(1);
     } catch (InterruptedException ie) {
       
     }
     full.stream().forEach(m->{
        manager.complete(m.getTransactionID(), new byte[0]);
        manager.retired(m.getTransactionID());
       });
    }).start();
    
    try {
      EntityClientEndpoint endpoint = fetcher.getResult();
      for (int x=0;x<ClientConfigurationContext.MAX_PENDING_REQUESTS;x++) {
        endpoint.beginInvoke().invokeWithTimeout(5, TimeUnit.SECONDS);
      }
      endpoint.beginInvoke().invokeWithTimeout(0, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      to.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
      // not expected
      throw e;
    } 
  }
    
  public void testThreadInterruptsDontCauseSendIssues() throws Exception {
    // Set the target for success.
    final byte[] resultObject = new byte[8];
    ByteBuffer.wrap(resultObject).putLong(1L);
    final EntityException resultException = null;
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });       
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    fetcher.interruptAtStart();
    fetcher.start();
    fetcher.join();
    try {
      EntityClientEndpoint endpoint = fetcher.getResult();
      Assert.assertNotNull(endpoint);
    } catch (Exception e) {
      e.printStackTrace();
      // not expected
      throw e;
    }
  }

  // Test that concurrent lookups of the same non-existent entity both fail in the expected way, raising no exceptions.
  public void testObjectNotFoundConcurrentLookup() throws Exception {
    // Configure the test to return the failure for this.
    final byte[] resultObject = null;
    final EntityException resultException = new EntityNotFoundException(null, null);
    when(channel.createMessage(Mockito.eq(TCMessageType.VOLTRON_ENTITY_MESSAGE))).then(new Answer<TCMessage>() {
      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new TestRequestBatchMessage(manager, resultObject, resultException, true);
      }
    });
    
    TestFetcher fetcher1 = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    TestFetcher fetcher2 = new TestFetcher(this.manager, this.entityID, 1L, this.instance);
    fetcher1.start();
    fetcher2.start();
    fetcher1.join();
    fetcher2.join();

    // We expect that both should have failed to find the entity.
    assertFalse(didFindEndpoint(fetcher1));
    assertFalse(didFindEndpoint(fetcher2));
  }

  public void testPauseUnpause() {
    this.manager.pause();
    try {
      this.manager.pause();
      fail();
    } catch (final AssertionError e) {
      // expected assertion
    }

    this.manager.unpause();
    try {
      this.manager.unpause();
      fail();
    } catch (final AssertionError e) {
      // expected assertion
    }
  }
  
  @Test
  public void testSingleInvoke() throws Exception {
    byte[] resultObject = new byte[0];
    EntityException resultException = null;
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    InFlightMessage result = this.manager.invokeAction(entityID, descriptor, Collections.<Acks>emptySet(), null, false, true, false, new byte[0]);
    // We are waiting for no ACKs so this should be available since the send will trigger the delivery.
    byte[] last = result.get();
    assertTrue(resultObject == last);
  }
  
  
  @Test
  public void testSingleInvokeTimeout() throws Exception {
    byte[] resultObject = new byte[0];
    EntityException resultException = null;
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, false);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    InFlightMessage result = this.manager.invokeAction(entityID, descriptor, Collections.<Acks>emptySet(), null, false, true, false, new byte[0]);
    // We are waiting for no ACKs so this should be available since the send will trigger the delivery.
    long start = System.currentTimeMillis();
    try {
      result.getWithTimeout(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
      assertThat(System.currentTimeMillis() - start, Matchers.greaterThanOrEqualTo(1000L));
      //  expected
    }
    start = System.currentTimeMillis();
    try {
      result.getWithTimeout(2, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
      assertThat(System.currentTimeMillis() - start, Matchers.greaterThanOrEqualTo(2000L));
      //  expected
    }
  }  

  @Test
  public void testCreate() throws Exception {
    long version = 1;
    byte[] config = new byte[0];
    byte[] resultObject = new byte[0];
    EntityException resultException = null;
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    this.manager.createEntity(entityID, version, config);
    // We are waiting for no ACKs so this should be available since the send will trigger the delivery.
  }

  @Test
  public void testRequestAcks() throws Exception {
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, null, null, false);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    final EnumSet<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.RECEIVED);
    final ClientEntityManager mgr = manager;
    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {
        mgr.invokeAction(entityID, descriptor, requestedAcks, null, false, true, false, new byte[0]);
      }
      
    });
    t.start();
    
    t.join(1000);
    assertThat(t.isAlive(), is(true));
    
    this.manager.received(message.getTransactionID());
    
    byte[] resultObject = new byte[0];
    EntityException resultException = null;
    message.explicitComplete(resultObject, resultException);
    
    t.join();
    assertThat(t.isAlive(), is(false));
  }

  private boolean didFindEndpoint(TestFetcher fetcher) throws Exception {
    boolean didFind = false;
    try {
      EntityClientEndpoint<EntityMessage, EntityResponse> endpoint = fetcher.getResult();
      didFind = true;
      endpoint.close();
    } catch (EntityNotFoundException e) {
      // This is how we flag something as not found.
      didFind = false;
    }
    return didFind;
  }


  private static class TestFetcher extends Thread {
    private final ClientEntityManager manager;
    private final EntityID entity;
    private final long version;
    private boolean interrupt = false;
    private final ClientInstanceID instance;
    private EntityClientEndpoint<EntityMessage, EntityResponse> result;
    private Exception exception;
    
    public TestFetcher(ClientEntityManager manager, EntityID entity, long version, ClientInstanceID instance) {
      this.manager = manager;
      this.entity = entity;
      this.version = version;
      this.instance = instance;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      try {
        if (this.interrupt) {
          this.interrupt();
        }
        this.result = this.manager.fetchEntity(entity, version, instance, mock(MessageCodec.class), mock(Runnable.class));
        if (this.interrupt) {
          Assert.assertTrue(this.isInterrupted());
        }
      } catch (Exception t) {
        this.exception = t;
      }
    }
    public EntityClientEndpoint<EntityMessage, EntityResponse> getResult() throws Exception {
      if (null != this.exception) {
        throw this.exception;
      }
      return this.result;
    }
    
    public void interruptAtStart() {
      interrupt = true;
    }
    
    public void close() {
      result.close();
    }
    
    public Future<Void> release() {
      return result.release();
    }
  }
  
  
  private static class TestRequestBatchMessage implements NetworkVoltronEntityMessage {
    private final ClientEntityManager clientEntityManager;
    private final byte[] resultObject;
    private final EntityException resultException;
    private final boolean autoComplete;
    private TransactionID transactionID;
    private EntityDescriptor descriptor;
    private EntityID entityID;
    private byte[] extendedData;
    private boolean requiresReplication;
    private Type type;
    
    public TestRequestBatchMessage(ClientEntityManager clientEntityManager, byte[] resultObject, EntityException resultException, boolean autoComplete) {
      this.clientEntityManager = clientEntityManager;
      this.resultObject = resultObject;
      this.resultException = resultException;
      this.autoComplete = autoComplete;
    }
    public void explicitComplete(byte[] explicitResult, EntityException resultException) {
      Assert.assertFalse(this.autoComplete);
      if (null != explicitResult) {
        this.clientEntityManager.complete(this.transactionID, explicitResult);
      } else {
        if (null != resultException) {
          this.clientEntityManager.failed(this.transactionID, resultException);
        } else {
          this.clientEntityManager.complete(this.transactionID);
        }
      }
    }
    @Override
    public TransactionID getTransactionID() {
      return this.transactionID;
    }

    @Override
    public EntityID getEntityID() {
      return this.entityID;
    }

    @Override
    public Set<Acks> getRequestedAcks() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean doesRequestReceived() {
      return true;
    }
    
    @Override
    public TCMessageType getMessageType() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void hydrate() throws IOException, UnknownNameException {
      throw new UnsupportedOperationException();
    }
    @Override
    public void dehydrate() {
      throw new UnsupportedOperationException();
    }
    boolean sent = false;
    @Override
    public boolean send() {
      assertFalse(sent);
      sent = true;
      if (this.autoComplete) {
        if (null != this.resultObject) {
          this.clientEntityManager.complete(this.transactionID, this.resultObject);
        } else {
          if (null != this.resultException) {
            this.clientEntityManager.failed(this.transactionID, this.resultException);
          } else {
            this.clientEntityManager.complete(this.transactionID);
          }
        }
        this.clientEntityManager.retired(this.transactionID);
      }
      return sent;
    }
    
    @Override
    public MessageChannel getChannel() {
      throw new UnsupportedOperationException();
    }
    @Override
    public NodeID getSourceNodeID() {
      throw new UnsupportedOperationException();
    }
    @Override
    public NodeID getDestinationNodeID() {
      throw new UnsupportedOperationException();
    }
    @Override
    public SessionID getLocalSessionID() {
      throw new UnsupportedOperationException();
    }
    @Override
    public int getTotalLength() {
      throw new UnsupportedOperationException();
    }
    @Override
    public ClientID getSource() {
      throw new UnsupportedOperationException();
    }
    @Override
    public EntityDescriptor getEntityDescriptor() {
      return this.descriptor;
    }
    @Override
    public boolean doesRequireReplication() {
      return this.requiresReplication;
    }
    @Override
    public Type getVoltronType() {
      return type;
    }
    @Override
    public byte[] getExtendedData() {
      return this.extendedData;
    }
    @Override
    public TransactionID getOldestTransactionOnClient() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setContents(ClientID clientID, TransactionID transactionID, EntityID eid, EntityDescriptor entityDescriptor, 
            Type type, boolean requiresReplication, byte[] extendedData, TransactionID oldestTransactionPending, Set<Acks> acks) {
      this.transactionID = transactionID;
      Assert.assertNotNull(eid);
      this.entityID = eid;
      this.descriptor = entityDescriptor;
      this.extendedData = extendedData;
      this.requiresReplication = requiresReplication;
      this.type = type;
    }

    @Override
    public void setMessageCodecSupplier(MessageCodecSupplier supplier) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EntityMessage getEntityMessage() {
      throw new UnsupportedOperationException();
    }
  }
  
  private static class FakeSink implements Sink<Object> {
    
    private final EventHandler<Object> handle;

    public FakeSink(EventHandler<Object> handle) {
      this.handle = handle;
    }

    @Override
    public void addToSink(Object context) {
      try {
        handle.handleEvent(context);
      } catch (EventHandlerException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
