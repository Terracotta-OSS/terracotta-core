/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Matchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.entity.ConcurrencyStrategy;

/**
 *
 * @author mscott
 */
public class RequestProcessorTest {
  
  public RequestProcessorTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of scheduleRequest method, of class RequestProcessor.
   */
  @Test
  public void testBasicScheduleRequest() {
    System.out.println("scheduleRequest");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    ConcurrencyStrategy strategy = mock(ConcurrencyStrategy.class);
    when(strategy.concurrencyKey(Matchers.any())).thenReturn(ConcurrencyStrategy.UNIVERSAL_KEY);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    when(request.getPayload()).thenReturn(new byte[0]);
    Sink<Runnable> dump = mock(Sink.class);
    RequestProcessor instance = new RequestProcessor(new NoReplicationBroker(), dump);
    int expResult = ConcurrencyStrategy.UNIVERSAL_KEY;
    int result = instance.scheduleRequest(entity, strategy, request);
    assertEquals(expResult, result);
    
    verify(dump).addMultiThreaded(Matchers.any());
    verify(strategy).concurrencyKey(Matchers.any());
  }
  
  @Test
  public void testConcurrencyStrategy() {
    System.out.println("concurrency");
    EntityID testid = new EntityID("MockEntity", "foo");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ConcurrencyStrategy strategy = mock(ConcurrencyStrategy.class);
    when(strategy.concurrencyKey(Matchers.any())).thenAnswer(new Answer<Integer>() {

      @Override
      public Integer answer(InvocationOnMock invocation) throws Throwable {
        byte[] payload = (byte[])invocation.getArguments()[0];
        return arrayToInt(payload);
      }
      
    });
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    int key = Math.abs((int)(Math.random() * Integer.MAX_VALUE));
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    when(request.getPayload()).thenReturn(intToArray(key));
    Sink dump = mock(Sink.class);
    RequestProcessor instance = new RequestProcessor(new NoReplicationBroker(), dump);
    int expResult = key;
    int result = instance.scheduleRequest(entity, strategy, request);
    assertEquals(expResult, result);

    verify(dump).addMultiThreaded(Matchers.argThat(new MultiThreadedEventMatcher(testid, key)));
  }

  @Test
  public void testUniversalKey() {
    System.out.println("univeral key");
    EntityID testid = new EntityID("MockEntity", "foo");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ConcurrencyStrategy strategy = mock(ConcurrencyStrategy.class);
    when(strategy.concurrencyKey(Matchers.any())).thenReturn(ConcurrencyStrategy.UNIVERSAL_KEY);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    Sink dump = mock(Sink.class);

    RequestProcessor instance = new RequestProcessor(new NoReplicationBroker(), dump);
    int expResult = ConcurrencyStrategy.UNIVERSAL_KEY;
    int result = instance.scheduleRequest(entity, strategy, request);
    assertEquals(expResult, result);

    verify(dump).addMultiThreaded(Matchers.argThat(new MultiThreadedEventMatcher(testid, expResult)));
  }

  @Test
  public void testManagementKey() {
    System.out.println("management key");
    EntityID testid = new EntityID("MockEntity", "foo");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ConcurrencyStrategy strategy = mock(ConcurrencyStrategy.class);
    when(strategy.concurrencyKey(Matchers.any())).thenReturn(ConcurrencyStrategy.UNIVERSAL_KEY);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    when(request.getAction()).thenReturn(ServerEntityAction.CREATE_ENTITY);
    Sink dump = mock(Sink.class);

    RequestProcessor instance = new RequestProcessor(new NoReplicationBroker(), dump);
    int expResult = ConcurrencyStrategy.MANAGEMENT_KEY;
    int result = instance.scheduleRequest(entity, strategy, request);
    assertEquals(expResult, result);

    verify(dump).addMultiThreaded(Matchers.argThat(new MultiThreadedEventMatcher(testid, expResult)));
  }
  
  @Test
  public void testReplicationCall() {
    System.out.println("replication");
    EntityID testid = new EntityID("MockEntity", "foo");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ConcurrencyStrategy strategy = mock(ConcurrencyStrategy.class);
    when(strategy.concurrencyKey(Matchers.any())).thenReturn(ConcurrencyStrategy.UNIVERSAL_KEY);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.requiresReplication()).thenReturn(Boolean.TRUE);
    Sink dump = mock(Sink.class);
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    
    RequestProcessor instance = new RequestProcessor(broker, dump);
    int expResult = ConcurrencyStrategy.UNIVERSAL_KEY;
    int result = instance.scheduleRequest(entity, strategy, request);
    assertEquals(expResult, result);
//  assume args from mocked request are passed.  just testing execution
    verify(broker).replicateMessage(Matchers.eq(testid), Matchers.any(), Matchers.eq(expResult), Matchers.any(), Matchers.any(), Matchers.any());
  }
  
  private static byte[] intToArray(int val) {
    byte[] four = new byte[4];
    for (int x=0;x<four.length;x++) {
      four[x] = (byte)((val >> ((3 - x) * Byte.SIZE)) & 0x00ff);
    }
    return four;
  }
  
  private static int arrayToInt(byte[] val) {
    int end = 0;
    for (int x=0;x<val.length;x++) {
      end <<= Byte.SIZE;
      end |= (val[x] & 0x00ff);
    }
    return end;
  }  
  
  private class MultiThreadedEventMatcher extends BaseMatcher<ServerEntityRequest> {
    
    private final int rawKey;
    private final EntityID entity;

    public MultiThreadedEventMatcher(EntityID id, int rawKey) {
      entity = id;
      this.rawKey = rawKey;
    }
    
    @Override
    public boolean matches(Object item) {
      if (item instanceof MultiThreadedEventContext) {
        Object schedulingKey = ((MultiThreadedEventContext)item).getSchedulingKey();
        return (rawKey == ConcurrencyStrategy.UNIVERSAL_KEY)
            ? (null == schedulingKey)
            : schedulingKey.equals(entity.hashCode() ^ rawKey);
      }
      return false;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("my matcher");
    }
    
    
    
  }
  
}
