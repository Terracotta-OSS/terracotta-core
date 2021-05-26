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
package com.tc.objectserver.handler;

import com.tc.object.tx.TransactionID;
import static org.mockito.Mockito.mock;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;

import com.tc.objectserver.api.Retiree;
import com.tc.util.concurrent.ThreadUtil;
import java.util.ArrayList;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;


public class RetirementManagerTest {
  private RetirementManager retirementManager;


  @Before
  public void setUp() throws Exception {
    this.retirementManager = new RetirementManager();
  }

  @Test
  public void testSimpleRetire() throws Exception {
    Retiree request = makeResponse();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    registerWithMessage(request, invokeMessage, concurrencyKey);
    
    List<Retiree> toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage);
    Assert.assertEquals(1, toRetire.size());
    Assert.assertEquals(request, toRetire.get(0));
  }

  @Test
  public void testMultiHold() throws Exception {
    Retiree resp = makeResponse();
    EntityMessage msg = mock(EntityMessage.class);
    registerWithMessage(resp, msg, 9);
    retirementManager.holdMessage(msg);
    retirementManager.holdMessage(msg);
    Assert.assertFalse(retirementManager.releaseMessage(msg));
    Assert.assertEquals(0, retirementManager.testingRetireForCompletion(msg).size());
    Assert.assertTrue(retirementManager.releaseMessage(msg));
    Assert.assertTrue(retirementManager.testingIsRetireable(msg));
  }

  
  @Test
  public void testSequenceOfRetires() throws Exception {
    int concurrencyKey = 1;
    for (int i = 0; i < 10; ++i) {
      sendNormalMessage(concurrencyKey);
    }
  }

  private void sendNormalMessage(int concurrencyKey) {
    Retiree request = makeResponse();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    registerWithMessage(request, invokeMessage, concurrencyKey);
    
    List<Retiree> toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage);
    Assert.assertEquals(1, toRetire.size());
    Assert.assertEquals(request, toRetire.get(0));
  }

  @Test
  public void testDeferredRetire() throws Exception {
    Retiree request = makeResponse();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    registerWithMessage(request, invokeMessage, concurrencyKey);
    
    Retiree newRequest = makeResponse();
    EntityMessage newMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, newMessage);
    
    registerWithMessage(newRequest, newMessage, concurrencyKey);

    List<Retiree> toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    toRetire = this.retirementManager.testingRetireForCompletion(newMessage);
    Assert.assertEquals(2, toRetire.size());
  }

  @Test
  public void testSequenceAndDefer() throws Exception {
    int concurrencyKey = 1;
    sendNormalMessage(concurrencyKey);
    
    Retiree request = makeResponse();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    registerWithMessage(request, invokeMessage, concurrencyKey);
    
    Retiree newRequest = makeResponse();
    EntityMessage newMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, newMessage);
    registerWithMessage(newRequest, newMessage, concurrencyKey);
    
    List<Retiree> toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    toRetire = this.retirementManager.testingRetireForCompletion(newMessage);
    Assert.assertEquals(2, toRetire.size());
    
    sendNormalMessage(concurrencyKey);
    sendNormalMessage(concurrencyKey);
  }

  @Test
  public void testDeferredWithNonDeferred() throws Exception {
    Retiree request = makeResponse();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    registerWithMessage(request, invokeMessage, concurrencyKey);
    
    Retiree deferRequest = makeResponse();
    EntityMessage deferMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, deferMessage);
    registerWithMessage(deferRequest, deferMessage, concurrencyKey);
    
    List<Retiree> toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    // Run some other messages.
    Retiree request1 = makeResponse();
    EntityMessage message1 = mock(EntityMessage.class);
    registerWithMessage(request1, message1, concurrencyKey);
    toRetire = this.retirementManager.testingRetireForCompletion(message1);
    Assert.assertEquals(1, toRetire.size());
    
    Retiree request2 = makeResponse();
    EntityMessage message2 = mock(EntityMessage.class);
    registerWithMessage(request2, message2, concurrencyKey);
    toRetire = this.retirementManager.testingRetireForCompletion(message2);
    Assert.assertEquals(1, toRetire.size());
    
    // Now, run the message which should unblock us.
    toRetire = this.retirementManager.testingRetireForCompletion(deferMessage);
    Assert.assertEquals(2, toRetire.size());
  }

  /**
   *
   * <p>This test tests RetirementManager using EntityMessages with cross concurrency key dependencies</p>
   *
   * <p>Below diagram shows relationship between requests and concurrency keys</p>
   *
   * <pre>
   *                          ---------------------------------
   * Concurrency Key 1  -->  | request1 | request2 | request7
   *                          ---------------------------------
   * Concurrency Key 2  -->  | request3 | request4
   *                          ---------------------------------
   * Concurrency Key 3  -->  | request5 | request6
   *                          ---------------------------------
   * </pre>
   *
   * <p>Deferred requests</p>
   *  <ul>
   *    <li>request1 deferred its retirement to request3</li>
   *    <li>request3 deferred its retirement to request5</li>
   *  </ul>
   *
   *
   * @throws Exception
   */
  @Test
  public void testDeferredMessageWithCrossConcurrencyKeyDependencies() throws Exception {

    int concurrencyKeyOne = 1;
    int concurrencyKeyTwo = 2;
    int concurrencyKeyThree = 3;
    List<Retiree> toRetire;

    Retiree request1 = makeResponse();
    EntityMessage request1Message = mock(EntityMessage.class);
    registerWithMessage(request1, request1Message, concurrencyKeyOne);

    Retiree request2 = makeResponse();
    EntityMessage request2Message = mock(EntityMessage.class);
    registerWithMessage(request2, request2Message, concurrencyKeyOne);
    toRetire = this.retirementManager.testingRetireForCompletion(request2Message);
    // Completing request2 shouldn't cause any messages to be retired as request1 is not retired yet
    Assert.assertEquals(1, toRetire.size());

    Retiree request3 = makeResponse();
    EntityMessage request3Message = mock(EntityMessage.class);
    // request1 retirement deferred until request3 completes now
    this.retirementManager.deferRetirement(request1Message, request3Message);
    registerWithMessage(request3, request3Message, concurrencyKeyTwo);

    toRetire = this.retirementManager.testingRetireForCompletion(request1Message);
    // Completing request1 shouldn't cause any messages to be retired as request3 is not completed yet
    Assert.assertEquals(0, toRetire.size());

    Retiree request4 = makeResponse();
    EntityMessage request4Message = mock(EntityMessage.class);
    registerWithMessage(request4, request4Message, concurrencyKeyTwo);
    toRetire = this.retirementManager.testingRetireForCompletion(request4Message);
    // Completing request4 shouldn't cause any messages to be retired as request3 is not retired yet
    Assert.assertEquals(1, toRetire.size());

    Retiree request5 = makeResponse();
    EntityMessage request5Message = mock(EntityMessage.class);
    // request3 retirement deferred until request5 completes now
    this.retirementManager.deferRetirement(request3Message, request5Message);
    registerWithMessage(request5, request5Message, concurrencyKeyThree);

    toRetire = this.retirementManager.testingRetireForCompletion(request3Message);
    // Completing request3 should retire both request1 and request2 in order but not request3
    Assert.assertEquals(0, toRetire.size());

    Retiree request6 = makeResponse();
    EntityMessage request6Message = mock(EntityMessage.class);
    registerWithMessage(request6, request6Message, concurrencyKeyThree);
    toRetire = this.retirementManager.testingRetireForCompletion(request6Message);
    // Completing request6 shouldn't cause any messages to be retired as request5 is not retired yet
    Assert.assertEquals(1, toRetire.size());

    Retiree request7 = makeResponse();
    EntityMessage request7Message = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(request5Message, request7Message);
    registerWithMessage(request7, request7Message, concurrencyKeyOne);
    toRetire = this.retirementManager.testingRetireForCompletion(request5Message);
    // Completing request5 should retire both request3 and request4 in order
    Assert.assertEquals(0, toRetire.size());

    toRetire = this.retirementManager.testingRetireForCompletion(request7Message);
    // Completing request7 should retire request7, request5 and request6 in order
    // Note that we need to maintain order only for request5 and request6 as they belong to same
    // concurrency key
    Assert.assertEquals(4, toRetire.size());

  }

  @Test
  public void testRetireForCompletionWithUncompletedRequests() throws Exception {
    int concurrencyKeyOne = 1;
    int concurrencyKeyTwo = 2;
    List<Retiree> toRetire;

    Retiree request1 = makeResponse();
    EntityMessage request1Message = mock(EntityMessage.class);
    registerWithMessage(request1, request1Message, concurrencyKeyOne);

    Retiree request2 = makeResponse();
    EntityMessage request2Message = mock(EntityMessage.class);
    registerWithMessage(request2, request2Message, concurrencyKeyOne);

    Retiree request3 = makeResponse();
    EntityMessage request3Message = mock(EntityMessage.class);
    // request1 retirement deferred until request3 completes
    this.retirementManager.deferRetirement(request1Message, request3Message);
    registerWithMessage(request3, request3Message, concurrencyKeyTwo);

    toRetire = this.retirementManager.testingRetireForCompletion(request1Message);
    // request1 completion shouldn't retire any requests as request3 is not completed yet
    Assert.assertEquals(0, toRetire.size());

    toRetire = this.retirementManager.testingRetireForCompletion(request3Message);
    // request3 completion should only retire request1 and request3 as request2 is not completed yet
    Assert.assertEquals(2, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request3, request1));

    toRetire = this.retirementManager.testingRetireForCompletion(request2Message);
    Assert.assertEquals(1, toRetire.size());
    // request2 completion will retire request2 finally
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request2));
  }

  @Test
  public void testRetirementWithMultiMessageDependency() throws Exception {
    final int concurrencyKeyOne = 1;
    final int concurrencyKeyTwo = 2;
    List<Retiree> toRetire;

    Retiree invokeRequest = makeResponse();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    registerWithMessage(invokeRequest, invokeMessage, concurrencyKeyOne);

    Retiree deferRequest1 = makeResponse();
    EntityMessage deferMessage1 = mock(EntityMessage.class);
    // invokeRequest retirement deferred until deferRequest1 completes
    this.retirementManager.deferRetirement(invokeMessage, deferMessage1);
    registerWithMessage(deferRequest1, deferMessage1, concurrencyKeyOne);

    Retiree deferRequest2 = makeResponse();
    EntityMessage deferMessage2 = mock(EntityMessage.class);
    // invokeRequest retirement deferred until deferRequest2 completes
    this.retirementManager.deferRetirement(invokeMessage, deferMessage2);
    registerWithMessage(deferRequest2, deferMessage2, concurrencyKeyTwo);

    toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage);
    // invokeRequest completion shouldn't retire any requests as deferRequest1 and deferRequest2 are not completed yet
    Assert.assertEquals(0, toRetire.size());

    toRetire = this.retirementManager.testingRetireForCompletion(deferMessage2);
    // deferRequest2 completion should retire only deferRequest2 as invokeRequest deferred its retirement to as
    // deferRequest2 as well, which is not completed yet
    Assert.assertEquals(1, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(deferRequest2));

    toRetire = this.retirementManager.testingRetireForCompletion(deferMessage1);
    Assert.assertEquals(2, toRetire.size());
  }
  
  @Test
  public void testChainedRetirement() throws Exception {
    Retiree invokeRequest1 = makeResponse();
    EntityMessage invokeMessage1 = mock(EntityMessage.class);    
    
    Retiree invokeRequest2 = makeResponse();
    EntityMessage invokeMessage2 = mock(EntityMessage.class);

    Retiree invokeRequest3 = makeResponse();
    EntityMessage invokeMessage3 = mock(EntityMessage.class);
    
    registerWithMessage(invokeRequest1, invokeMessage1, 1);
    
    this.retirementManager.deferRetirement(invokeMessage1, invokeMessage2);
    
    registerWithMessage(invokeRequest2, invokeMessage2, 1);

    this.retirementManager.deferRetirement(invokeMessage2, invokeMessage3);
    
    registerWithMessage(invokeRequest3, invokeMessage3, 1);
    
    List<Retiree> retireList = this.retirementManager.testingRetireForCompletion(invokeMessage1);
    
    Assert.assertTrue(retireList.isEmpty());
    
    retireList = this.retirementManager.testingRetireForCompletion(invokeMessage2);

    Assert.assertTrue(retireList.isEmpty());

    retireList = this.retirementManager.testingRetireForCompletion(invokeMessage3);
    
    Assert.assertEquals(3, retireList.size());

    retireList.forEach(r->r.retired());
  }
  
  
  @Test
  public void testChainedRetirementCompletionOrder() throws Exception {
    ExecutorService service = Executors.newCachedThreadPool();
    EntityMessage[] current = new EntityMessage[5];
    Future[] waits = new Future[5];
    List<TransactionID> order = new ArrayList<>();
    for (int x=0;x<current.length;x++) {
      Retiree invokeRequest = makeResponse();
      current[x] = mock(EntityMessage.class);
      when(invokeRequest.getTraceID()).thenReturn("msg-" + x);
      when(invokeRequest.getTransaction()).thenReturn(new TransactionID(x+1));
      CompletableFuture<Void> end = new CompletableFuture<>();
      waits[x] = end;
      when(invokeRequest.retired()).then(in->{
        System.out.println(Thread.currentThread().getName());
        service.submit(()->{
          ThreadUtil.reallySleep(1000);
          System.out.println(Thread.currentThread().getName() + " on " + invokeRequest.getTraceID());
          order.add(invokeRequest.getTransaction());
          end.complete(null);
        });
        return end;
      });
      if (x>0) {
        this.retirementManager.deferRetirement(current[x-1], current[x]);
      }
      this.retirementManager.registerWithMessage(current[x], 1, invokeRequest);
    }

    for (EntityMessage m : current) {
      this.retirementManager.retireMessage(m);
    }
    for (Future m : waits) {
      m.get();
    }
    
    //  decending order
    TransactionID check = null;
    for (TransactionID id : order) {
      Assert.assertTrue(check == null || check.toLong() > id.toLong());
      check = id;
    }
  }
  
  @Test
  public void testCloseBeforeRetire() throws Exception {
    Retiree invokeRequest1 = makeResponse();
    EntityMessage invokeMessage1 = mock(EntityMessage.class);    
    
    registerWithMessage(invokeRequest1, invokeMessage1, 1);
    
    this.retirementManager.holdMessage(invokeMessage1);
    
    boolean readyForRetire = this.retirementManager.releaseMessage(invokeMessage1);
    
    Assert.assertFalse(readyForRetire);

    List<Retiree> retireList = this.retirementManager.testingRetireForCompletion(invokeMessage1);
    
    Assert.assertEquals(1, retireList.size());

    retireList.forEach(r->r.retired());
  }
  
  @Test
  public void testChainedRetirementOnMultipleKeys() throws Exception {
    Retiree invokeRequest1 = makeResponse();
    EntityMessage invokeMessage1 = mock(EntityMessage.class);    
    
    Retiree invokeRequest2 = makeResponse();
    EntityMessage invokeMessage2 = mock(EntityMessage.class);

    Retiree invokeRequest3 = makeResponse();
    EntityMessage invokeMessage3 = mock(EntityMessage.class);
    
    registerWithMessage(invokeRequest1, invokeMessage1, 1);
    
    this.retirementManager.deferRetirement(invokeMessage1, invokeMessage2);
    
    registerWithMessage(invokeRequest2, invokeMessage2, 2);

    this.retirementManager.deferRetirement(invokeMessage2, invokeMessage3);
    
    registerWithMessage(invokeRequest3, invokeMessage3, 3);
    
    List<Retiree> retireList = this.retirementManager.testingRetireForCompletion(invokeMessage1);
    
    Assert.assertTrue(retireList.isEmpty());
    
    retireList = this.retirementManager.testingRetireForCompletion(invokeMessage2);

    Assert.assertTrue(retireList.isEmpty());

    retireList = this.retirementManager.testingRetireForCompletion(invokeMessage3);
    
    Assert.assertEquals(3, retireList.size());

    retireList.forEach(r->r.retired());
  }
  
  @Test
  public void testRetirementWithUniversalKeys() throws Exception {
    final int concurrencyKeyOne = 1;
    List<Retiree> toRetire;

    Retiree invokeRequest1 = makeResponse();
    EntityMessage invokeMessage1 = mock(EntityMessage.class);
    registerWithMessage(invokeRequest1, invokeMessage1, ConcurrencyStrategy.UNIVERSAL_KEY);

    Retiree invokeRequest2 = makeResponse();
    EntityMessage invokeMessage2 = mock(EntityMessage.class);
    registerWithMessage(invokeRequest2, invokeMessage2, ConcurrencyStrategy.UNIVERSAL_KEY);

    Retiree deferRequest = makeResponse();
    EntityMessage deferMessage = mock(EntityMessage.class);
    // invokeRequest2 retirement deferred until deferRequest completes
    this.retirementManager.deferRetirement(invokeMessage2, deferMessage);
    registerWithMessage(deferRequest, deferMessage, concurrencyKeyOne);

    toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage2);
    // invokeRequest2 completion shouldn't retire any requests as deferRequest is not completed yet
    Assert.assertEquals(0, toRetire.size());

    toRetire = this.retirementManager.testingRetireForCompletion(deferMessage);
    // deferRequest completion should retire both deferRequest and as invokeRequest2 as invokeRequest2
    // runs on universal key
    Assert.assertEquals(2, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(deferRequest, invokeRequest2));

    toRetire = this.retirementManager.testingRetireForCompletion(invokeMessage1);
    Assert.assertEquals(1, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(invokeRequest1));
  }
  
  @Test
  public void testSequentialRetire() throws Exception {
    Retiree invokeRequest1 = makeResponse();
    Mockito.doAnswer(invoke->{
      System.out.println("first");
      return CompletableFuture.completedFuture(null);
    }).when(invokeRequest1).retired();
    EntityMessage invokeMessage1 = mock(EntityMessage.class);    
    
    Retiree invokeRequest2 = makeResponse();
    Mockito.doAnswer(invoke->{
      System.out.println("second");
      return CompletableFuture.completedFuture(null);
    }).when(invokeRequest2).retired();
    EntityMessage invokeMessage2 = mock(EntityMessage.class);

    Retiree invokeRequest3 = makeResponse();
    Mockito.doAnswer(invoke->{
      System.out.println("third");
      return CompletableFuture.completedFuture(null);
    }).when(invokeRequest3).retired();
    EntityMessage invokeMessage3 = mock(EntityMessage.class);
    
    registerWithMessage(invokeRequest1, invokeMessage1, 1);
    
    this.retirementManager.deferRetirement(invokeMessage1, invokeMessage2);
    
    registerWithMessage(invokeRequest2, invokeMessage2, 2);

    this.retirementManager.deferRetirement(invokeMessage2, invokeMessage3);    

    registerWithMessage(invokeRequest3, invokeMessage3, 3);
    
    this.retirementManager.retireMessage(invokeMessage1);
    this.retirementManager.retireMessage(invokeMessage2);
    this.retirementManager.retireMessage(invokeMessage3);
  }
  
  private Retiree makeResponse() {
    Retiree request = mock(Retiree.class);
    return request;
  }
  
  
  private void registerWithMessage(Retiree resp, EntityMessage message, int concurrency) {
    try {
      this.retirementManager.registerWithMessage(message, concurrency, resp);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }  
}
