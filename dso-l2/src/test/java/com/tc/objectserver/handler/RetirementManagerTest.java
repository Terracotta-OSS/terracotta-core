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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.EntityMessage;

import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;

import java.util.List;


public class RetirementManagerTest {
  private RetirementManager retirementManager;


  @Before
  public void setUp() throws Exception {
    this.retirementManager = new RetirementManager();
  }

  @Test
  public void testSimpleRetire() throws Exception {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(1, toRetire.size());
    Assert.assertEquals(request, toRetire.get(0));
  }

  @Test
  public void testSequenceOfRetires() throws Exception {
    int concurrencyKey = 1;
    for (int i = 0; i < 10; ++i) {
      sendNormalMessage(concurrencyKey);
    }
  }

  private void sendNormalMessage(int concurrencyKey) {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(1, toRetire.size());
    Assert.assertEquals(request, toRetire.get(0));
  }

  @Test
  public void testDeferredRetire() throws Exception {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    ServerEntityRequest newRequest = makeRequest();
    EntityMessage newMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, newMessage);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    this.retirementManager.registerWithMessage(newRequest, newMessage, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(newMessage);
    Assert.assertEquals(2, toRetire.size());
  }

  @Test
  public void testSequenceAndDefer() throws Exception {
    int concurrencyKey = 1;
    sendNormalMessage(concurrencyKey);
    
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    ServerEntityRequest newRequest = makeRequest();
    EntityMessage newMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, newMessage);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    this.retirementManager.registerWithMessage(newRequest, newMessage, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(newMessage);
    Assert.assertEquals(2, toRetire.size());
    
    sendNormalMessage(concurrencyKey);
    sendNormalMessage(concurrencyKey);
  }

  @Test
  public void testDeferredWithNonDeferred() throws Exception {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    ServerEntityRequest deferRequest = makeRequest();
    EntityMessage deferMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, deferMessage);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    // Run some other messages.
    ServerEntityRequest request1 = makeRequest();
    EntityMessage message1 = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request1, message1, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(message1);
    Assert.assertEquals(0, toRetire.size());
    ServerEntityRequest request2 = makeRequest();
    EntityMessage message2 = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request2, message2, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(message2);
    Assert.assertEquals(0, toRetire.size());
    
    // Now, run the message which should unblock us.
    this.retirementManager.registerWithMessage(deferRequest, deferMessage, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(deferMessage);
    Assert.assertEquals(4, toRetire.size());
  }


  private ServerEntityRequest makeRequest() {
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    return request;
  }
}
