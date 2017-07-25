package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

public class ActiveInvokeContextImplTest {

  @Test
  public void testValid() {
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(
      new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)),
      1,
      2);
    Assert.assertThat(ctx.isValidClientInformation(), is(true));
  }

  @Test
  public void testInvalid() {
    ActiveInvokeContextImpl ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), 1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(), -1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), -1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(true));
    ctx = new ActiveInvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), 1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
  }

}