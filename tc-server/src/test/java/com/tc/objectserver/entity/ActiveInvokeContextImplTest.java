/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.EntityResponse;

public class ActiveInvokeContextImplTest {

  @Test
  public void testValid() {
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(
      new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)),
      1,
      1,
      2);
    Assert.assertThat(ctx.isValidClientInformation(), is(true));
  }

  @Test
  public void testInvalid() {
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), 1, 1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), 1, -1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), 1, -1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(true));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), 1, 1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
  }

  @Test
  public void testSendOnClosed() {
    Runnable open = mock(Runnable.class);
    Consumer response = mock(Consumer.class);
    Consumer exception = mock(Consumer.class);
    Runnable close = mock(Runnable.class);
    
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), 1, 1, 2, 
        open, response, exception, close);
    ActiveInvokeChannel chan = ctx.openInvokeChannel();
    verify(open).run();
    chan.sendResponse(mock(EntityResponse.class));
    verify(response).accept(any(EntityResponse.class));
    chan.close();
    verify(close).run();
    try {
      chan.sendResponse(mock(EntityResponse.class));
      Assert.fail();
    } catch (IllegalStateException state) {
      // expected
    }
  }

  @Test
  public void testMultipleActiveInvokeChannels() {
    Runnable open = mock(Runnable.class);
    Consumer response = mock(Consumer.class);
    Consumer exception = mock(Consumer.class);
    Runnable close = mock(Runnable.class);
    
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), 1, 1, 2, 
        open, response, exception, close);
    ActiveInvokeChannel chan1 = ctx.openInvokeChannel();
    verify(open).run();
    ActiveInvokeChannel chan2 = ctx.openInvokeChannel();
    verify(open, times(1)).run();

    chan1.sendResponse(mock(EntityResponse.class));
    verify(response).accept(any(EntityResponse.class));
    chan2.sendResponse(mock(EntityResponse.class));
    verify(response, times(2)).accept(any(EntityResponse.class));

    chan1.close();
    verify(close, never()).run();
//  check closed
    try {
      chan1.sendResponse(mock(EntityResponse.class));
      Assert.fail();
    } catch (IllegalStateException state) {
      // expected
    }
// check still open
    chan2.sendResponse(mock(EntityResponse.class));
    verify(response, times(3)).accept(any(EntityResponse.class));
    
    chan2.close();
    verify(close).run();

    try {
      chan2.sendResponse(mock(EntityResponse.class));
      Assert.fail();
    } catch (IllegalStateException state) {
      // expected
    }
  }

}