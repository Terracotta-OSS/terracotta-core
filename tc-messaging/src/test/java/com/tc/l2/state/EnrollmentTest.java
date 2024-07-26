/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.l2.state;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ServerID;
import com.tc.util.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnrollmentTest {

  @SuppressWarnings("resource")
  @Test
  public void testSerialization() throws Exception {
    Enrollment e = new Enrollment(new ServerID("30001", new byte[] { 54, -125, 34, -4 }), true, new long[] {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE});
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    e.serializeTo(bo);
    System.err.println("Written : " + e);
    try (TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.accessBuffers())) {
      Enrollment e1 = new Enrollment();
      e1.deserializeFrom(bi);
      System.err.println("Read : " + e1);

      assertEquals(e, e1);
    }
  }
  
  @SuppressWarnings("resource")
  @Test
  public void testCompare() throws Exception {
    Enrollment e1 = new Enrollment(new ServerID("30001", new byte[] { 54, -125, 34, -4 }), true, new long[] {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE});
    Enrollment e2 = new Enrollment(new ServerID("30001", new byte[] { 54, -125, 34, -4 }), true, new long[] {Long.MIN_VALUE, -1, -1, 1, Long.MAX_VALUE});
    Assert.assertTrue(e1.wins(e2));
  }
}
