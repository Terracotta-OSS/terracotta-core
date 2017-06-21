package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

public class InvokeContextImplTest {

  @Test
  public void testValid() {
    InvokeContextImpl ctx = new InvokeContextImpl(
      new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)),
      1,
      2);
    Assert.assertThat(ctx.isValidClientInformation(), is(true));
  }

  @Test
  public void testInvalid() {
    InvokeContextImpl ctx = new InvokeContextImpl(new ClientDescriptorImpl(), 1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new InvokeContextImpl(new ClientDescriptorImpl(), -1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new InvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), -1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new InvokeContextImpl(new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(2)), 1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
  }

}