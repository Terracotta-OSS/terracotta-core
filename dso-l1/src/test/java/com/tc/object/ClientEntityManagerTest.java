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
import com.tc.async.api.SpecializedEventContext;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import org.junit.Assert;
import org.junit.Test;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;

import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage.Acks;
import com.tc.exception.TCNotRunningException;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.stats.Stats;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class ClientEntityManagerTest extends TestCase {
  private ClientMessageChannel channel;
  private ClientEntityManager manager;
  private StageManager stageMgr;
  
  private EntityID entityID;
  private EntityDescriptor entityDescriptor;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void setUp() throws Exception {
    this.channel = mock(ClientMessageChannel.class);
    this.stageMgr = mock(StageManager.class);
    when(this.stageMgr.createStage(any(String.class), any(Class.class), any(EventHandler.class), anyInt(), anyInt())).then(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Stage stage = mock(Stage.class);
        when(stage.getSink()).thenReturn(new FakeSink((EventHandler)invocation.getArguments()[2]));
        return stage;
      }
    });
    this.manager = new ClientEntityManagerImpl(this.channel, stageMgr);
    
    String entityClassName = "Class Name";
    String entityInstanceName = "Instance Name";
    this.entityID = new EntityID(entityClassName, entityInstanceName);
    this.entityDescriptor = new EntityDescriptor(this.entityID, new ClientInstanceID(1), 1);
  }
  
  public void testResponseSinkFlush() throws Exception {
    
  }

  // Test that a simple lookup will succeed.
  public void testSimpleLookupSuccess() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
    // Set the target for success.
    final byte[] resultObject = new byte[0];
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

  // Test that a simple lookup can fail.
  public void testSimpleLookupFailure() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
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
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
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
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
    // Set the target for success.
    final byte[] resultObject = new byte[0];
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
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
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
      // Expected.
      didRelease = false;
    }
    assertFalse(didRelease);
  }

  // That that we can shut down while in a paused state without locking up.
  public void testShutdownWhilePaused() throws Exception {
    // We will create a runnable which will attempt to fetch the entity (and we will get this stuck in "WAITING" on pause).
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
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
    } catch (TCNotRunningException e) {
      // Expected.
    } catch (Throwable t) {
      // Unexpected.
      fail();
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
    
    TestFetcher fetcher1 = new TestFetcher(this.manager, this.entityDescriptor);
    TestFetcher fetcher2 = new TestFetcher(this.manager, this.entityDescriptor);
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
    InvokeFuture<byte[]> result = this.manager.invokeAction(entityDescriptor, Collections.<Acks>emptySet(), false, new byte[0]);
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
    InvokeFuture<byte[]> result = this.manager.invokeAction(entityDescriptor, Collections.<Acks>emptySet(), false, new byte[0]);
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
    InvokeFuture<byte[]> waiter = this.manager.createEntity(entityID, version, Collections.<Acks>emptySet(), config);
    // We are waiting for no ACKs so this should be available since the send will trigger the delivery.
    waiter.get();
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
        mgr.invokeAction(entityDescriptor, requestedAcks, false, new byte[0]);
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

  private boolean didFindEndpoint(TestFetcher fetcher) {
    boolean didFind = false;
    try {
      EntityClientEndpoint<EntityMessage, EntityResponse> endpoint = fetcher.getResult();
      didFind = true;
      endpoint.close();
    } catch (EntityNotFoundException e) {
      // This is how we flag something as not found.
      didFind = false;
    } catch (Throwable e) {
      // No exception expected.
      fail();
    }
    return didFind;
  }


  private static class TestFetcher extends Thread {
    private final ClientEntityManager manager;
    private final EntityDescriptor entityDescriptor;
    private EntityClientEndpoint<EntityMessage, EntityResponse> result;
    private Throwable exception;
    
    public TestFetcher(ClientEntityManager manager, EntityDescriptor entityDescriptor) {
      this.manager = manager;
      this.entityDescriptor = entityDescriptor;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      try {
        this.result = this.manager.fetchEntity(this.entityDescriptor, mock(MessageCodec.class), mock(Runnable.class));
      } catch (Throwable t) {
        this.exception = t;
      }
    }
    public EntityClientEndpoint<EntityMessage, EntityResponse> getResult() throws Throwable {
      if (null != this.exception) {
        throw this.exception;
      }
      return this.result;
    }
    
    public void close() {
      result.close();
    }
  }
  
  
  private static class TestRequestBatchMessage implements NetworkVoltronEntityMessage {
    private final ClientEntityManager clientEntityManager;
    private final byte[] resultObject;
    private final EntityException resultException;
    private final boolean autoComplete;
    private TransactionID transactionID;
    
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
    public void send() {
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
      }
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
      throw new UnsupportedOperationException();
    }
    @Override
    public boolean doesRequireReplication() {
      throw new UnsupportedOperationException();
    }
    @Override
    public Type getVoltronType() {
      throw new UnsupportedOperationException();
    }
    @Override
    public byte[] getExtendedData() {
      throw new UnsupportedOperationException();
    }
    @Override
    public TransactionID getOldestTransactionOnClient() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setContents(ClientID clientID, TransactionID transactionID, EntityDescriptor entityDescriptor, Type type, boolean requiresReplication, byte[] extendedData, TransactionID oldestTransactionPending) {
      this.transactionID = transactionID;
    }
  }
  
  private static class FakeSink implements Sink<Object> {
    
    private final EventHandler<Object> handle;

    public FakeSink(EventHandler<Object> handle) {
      this.handle = handle;
    }

    @Override
    public void addSingleThreaded(Object context) {
      try {
        handle.handleEvent(context);
      } catch (EventHandlerException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void addMultiThreaded(Object context) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addSpecialized(SpecializedEventContext specialized) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public void clear() {
// NOOP
    }

    @Override
    public void setClosed(boolean closed) {
//  NOOP
    }

    @Override
    public void enableStatsCollection(boolean enable) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isStatsCollectionEnabled() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stats getStats(long frequency) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resetStats() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
  }
}
