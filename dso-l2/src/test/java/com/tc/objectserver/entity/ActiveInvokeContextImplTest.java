package com.tc.objectserver.entity;

import org.junit.jupiter.api.Test;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.EntityResponse;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ActiveInvokeContextImplTest {

  @Test
  public void testValid() {
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(
      new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)),
      1,
      1,
      2);
    assertThat(ctx.isValidClientInformation(), is(true));
  }

  @Test
  public void testInvalid() {
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), 1, 1, 2);
    assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), 1, -1, -1);
    assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), 1, -1, 2);
    assertThat(ctx.isValidClientInformation(), is(true));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), 1, 1, -1);
    assertThat(ctx.isValidClientInformation(), is(false));
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
      fail();
    } catch (IllegalStateException state) {
      // expected
    }
  }
}