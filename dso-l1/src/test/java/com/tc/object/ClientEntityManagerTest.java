/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.test.categories.CheckShorts;

import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Category(CheckShorts.class)
public class ClientEntityManagerTest extends TestCase {
  private ClientMessageChannel channel;
  private ClientEntityManager manager;
  
  private EntityID entityID;
  private EntityDescriptor entityDescriptor;

  @Override
  public void setUp() throws Exception {
    this.channel = mock(ClientMessageChannel.class);
    this.manager = new ClientEntityManagerImpl(this.channel);
    
    String entityClassName = "Class Name";
    String entityInstanceName = "Instance Name";
    this.entityID = new EntityID(entityClassName, entityInstanceName);
    this.entityDescriptor = new EntityDescriptor(this.entityID, new ClientInstanceID(1), 1);
  }

  // Test that a simple lookup will succeed.
  public void testSimpleLookupSuccess() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
    // Set the target for success.
    byte[] resultObject = new byte[0];
    Exception resultException = null;
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    
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
    byte[] resultObject = null;
    Exception resultException = new RuntimeException("Entity not found!");
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we couldn't find the entity.
    assertFalse(didFindEndpoint(fetcher));
  }

  // Test pause will block progress but it the lookup will complete (failing to find the entity) after unpause.
  public void testLookupStalledByPause() throws Exception {
    Exception resultException = new RuntimeException("Entity not found!");
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, null, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    
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
    byte[] resultObject = new byte[0];
    Exception resultException = null;
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we found the entity.
    assertTrue(didFindEndpoint(fetcher));
    
    // Now, release it.
    this.manager.releaseEntity(entityDescriptor);
  }

  // Test fetch+release on failure.
  public void testFetchReleaseOnFailure() throws Exception {
    // We will create a runnable which will attempt to fetch the entity.
    TestFetcher fetcher = new TestFetcher(this.manager, this.entityDescriptor);
    
    // Set the target for failure.
    byte[] resultObject = null;
    Exception resultException = new RuntimeException("Entity not found!");
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    
    // Now we can start the lookup thread.
    fetcher.start();
    // Join on the thread.
    fetcher.join();
    
    // We expect that we couldn't find the entity.
    assertFalse(didFindEndpoint(fetcher));
    
    // Now, release it and expect to see the exception thrown, directly (since we are accessing the manage, directly).
    boolean didRelease = false;
    try {
      this.manager.releaseEntity(entityDescriptor);
      didRelease = true;
    } catch (TCObjectNotFoundException e) {
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
    byte[] resultObject = null;
    Exception resultException = new RuntimeException("Entity not found!");
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    
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
    Exception resultException = null;
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    Future<byte[]> result = this.manager.invokeAction(entityDescriptor, Collections.emptySet(), false, new byte[0]);
    // We are waiting for no ACKs so this should be available since the send will trigger the delivery.
    byte[] last = result.get();
    assertTrue(resultObject == last);
  }

  @Test
  public void testCreate() throws Exception {
    long version = 1;
    byte[] config = new byte[0];
    byte[] resultObject = new byte[0];
    Exception resultException = null;
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, resultObject, resultException, true);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    Future<Void> waiter = this.manager.createEntity(entityID, version, Collections.emptySet(), config);
    // We are waiting for no ACKs so this should be available since the send will trigger the delivery.
    waiter.get();
  }

  @Test
  public void testRequestAcks() throws Exception {
    TestRequestBatchMessage message = new TestRequestBatchMessage(this.manager, null, null, false);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE)).thenReturn(message);
    EnumSet<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.RECEIVED);
    Thread t = new Thread(() -> { this.manager.invokeAction(entityDescriptor, requestedAcks, false, new byte[0]);});
    t.start();
    
    t.join(1000);
    assertThat(t.isAlive(), is(true));
    
    this.manager.received(message.getTransactionID());
    
    byte[] resultObject = new byte[0];
    Exception resultException = null;
    message.explicitComplete(resultObject, resultException);
    
    t.join();
    assertThat(t.isAlive(), is(false));
  }

  private boolean didFindEndpoint(TestFetcher fetcher) {
    EntityClientEndpoint endpoint = null;
    try {
      endpoint = fetcher.getResult();
    } catch (Throwable e) {
      // No exception expected.
      fail();
    }
    if (null != endpoint) {
      endpoint.close();
    }
    return (null != endpoint);
  }


  private static class TestFetcher extends Thread {
    private final ClientEntityManager manager;
    private final EntityDescriptor entityDescriptor;
    private EntityClientEndpoint result;
    private Throwable exception;
    
    public TestFetcher(ClientEntityManager manager, EntityDescriptor entityDescriptor) {
      this.manager = manager;
      this.entityDescriptor = entityDescriptor;
    }
    @Override
    public void run() {
      try {
        this.result = this.manager.fetchEntity(this.entityDescriptor, mock(Runnable.class));
      } catch (Throwable t) {
        this.exception = t;
      }
    }
    public EntityClientEndpoint getResult() throws Throwable {
      if (null != this.exception) {
        throw this.exception;
      }
      return this.result;
    }
  }
  
  
  private static class TestRequestBatchMessage implements NetworkVoltronEntityMessage {
    private final ClientEntityManager clientEntityManager;
    private final byte[] resultObject;
    private final Exception resultException;
    private final boolean autoComplete;
    private TransactionID transactionID;
    
    public TestRequestBatchMessage(ClientEntityManager clientEntityManager, byte[] resultObject, Exception resultException, boolean autoComplete) {
      this.clientEntityManager = clientEntityManager;
      this.resultObject = resultObject;
      this.resultException = resultException;
      this.autoComplete = autoComplete;
    }
    public void explicitComplete(byte[] explicitResult, Exception resultException) {
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
    @Override
    public void send() {
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
    public NodeID getSource() {
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
    public Type getType() {
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
}
