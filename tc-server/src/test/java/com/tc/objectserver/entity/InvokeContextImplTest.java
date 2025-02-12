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