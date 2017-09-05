package com.tc.objectserver.entity;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import org.terracotta.entity.ClientSourceId;

public class InvokeContextImplTest {

  @Test
  public void testValid() {
    InvokeContextImpl ctx = new InvokeContextImpl(
      new ClientSourceIdImpl(1),
      1,
      1,
      2);
    Assert.assertThat(ctx.isValidClientInformation(), is(true));
  }

  @Test
  public void testInvalid() {
    InvokeContextImpl ctx = new InvokeContextImpl(new ClientSourceIdImpl(), 1, 1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new InvokeContextImpl(new ClientSourceIdImpl(), 1, -1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new InvokeContextImpl(new ClientSourceIdImpl(1), 1, -1, 2);
    Assert.assertThat(ctx.isValidClientInformation(), is(true));
    ctx = new InvokeContextImpl(new ClientSourceIdImpl(1), 1, 1, -1);
    Assert.assertThat(ctx.isValidClientInformation(), is(false));
  }
  
  @Test
  public void testClientSourceIdGen() {
    InvokeContextImpl ctx = new InvokeContextImpl(new ClientSourceIdImpl(), 10, 1, 2);
    final long incoming = 55;
    ClientSourceId sid = ctx.makeClientSourceId(incoming);
    com.tc.util.Assert.assertEquals(incoming, sid.toLong());
  }

}