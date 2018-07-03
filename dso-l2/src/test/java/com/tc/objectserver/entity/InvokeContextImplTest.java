package com.tc.objectserver.entity;

import org.junit.jupiter.api.Test;
import org.terracotta.entity.ClientSourceId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InvokeContextImplTest {

  @Test
  public void testValid() {
    InvokeContextImpl ctx = new InvokeContextImpl(
      new ClientSourceIdImpl(1),
      1,
      1,
      2);
    assertThat(ctx.isValidClientInformation(), is(true));
  }

  @Test
  public void testInvalid() {
    InvokeContextImpl ctx = new InvokeContextImpl(new ClientSourceIdImpl(), 1, 1, 2);
    assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new InvokeContextImpl(new ClientSourceIdImpl(), 1, -1, -1);
    assertThat(ctx.isValidClientInformation(), is(false));
    ctx = new InvokeContextImpl(new ClientSourceIdImpl(1), 1, -1, 2);
    assertThat(ctx.isValidClientInformation(), is(true));
    ctx = new InvokeContextImpl(new ClientSourceIdImpl(1), 1, 1, -1);
    assertThat(ctx.isValidClientInformation(), is(false));
  }
  
  @Test
  public void testClientSourceIdGen() {
    InvokeContextImpl ctx = new InvokeContextImpl(new ClientSourceIdImpl(), 10, 1, 2);
    final long incoming = 55;
    ClientSourceId sid = ctx.makeClientSourceId(incoming);
    com.tc.util.Assert.assertEquals(incoming, sid.toLong());
  }

}