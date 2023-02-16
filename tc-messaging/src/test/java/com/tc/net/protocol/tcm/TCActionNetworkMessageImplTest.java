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
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReferenceSupport;
import com.tc.bytes.TCReference;
import com.tc.net.protocol.TCNetworkHeader;
import java.util.Arrays;
import java.util.function.Supplier;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TCActionNetworkMessageImplTest {
  @Test
  public void testCommit() throws Exception {
    TCNetworkHeader header = mock(TCNetworkHeader.class);
    Supplier<TCReference> payloadSupplier = mock(Supplier.class);
    
    TCActionNetworkMessageImpl network = new TCActionNetworkMessageImpl(header, payloadSupplier);
    assertFalse(network.commit());
    assertTrue(network.load());
    assertTrue(network.commit());
    assertFalse(network.cancel());
  }

  @Test
  public void testCancel() throws Exception {
    TCNetworkHeader header = mock(TCNetworkHeader.class);
    Supplier<TCReference> payloadSupplier = mock(Supplier.class);
    
    TCActionNetworkMessageImpl network = new TCActionNetworkMessageImpl(header, payloadSupplier);
    assertFalse(network.commit());
    assertTrue(network.load());
    assertTrue(network.cancel());
    assertFalse(network.commit());
  }

  @Test
  public void testDoubleCancel() throws Exception {
    TCNetworkHeader header = mock(TCNetworkHeader.class);
    Supplier<TCReference> payloadSupplier = mock(Supplier.class);
    
    TCActionNetworkMessageImpl network = new TCActionNetworkMessageImpl(header, payloadSupplier);
    assertFalse(network.commit());
    assertTrue(network.load());
    assertTrue(network.cancel());
    assertTrue(network.cancel());
  }

  @Test
  public void testCancelBeforeLoad() throws Exception {
    TCNetworkHeader header = mock(TCNetworkHeader.class);
    Supplier<TCReference> payloadSupplier = mock(Supplier.class);
    
    TCActionNetworkMessageImpl network = new TCActionNetworkMessageImpl(header, payloadSupplier);
    assertTrue(network.cancel());
    assertFalse(network.load());
    assertTrue(network.cancel());
  }

  @Test
  public void testCancelAfterCommit() throws Exception {
    TCNetworkHeader header = mock(TCNetworkHeader.class);
    Supplier<TCReference> payloadSupplier = mock(Supplier.class);
    
    TCActionNetworkMessageImpl network = new TCActionNetworkMessageImpl(header, payloadSupplier);
    assertTrue(network.load());
    assertTrue(network.commit());
    assertFalse(network.cancel());
  }
  
  @Test
  public void testCommitThenLoad() throws Exception {
    TCNetworkHeader header = mock(TCNetworkHeader.class);
    Supplier<TCReference> payloadSupplier = mock(Supplier.class);
    
    TCActionNetworkMessageImpl network = new TCActionNetworkMessageImpl(header, payloadSupplier);
    assertFalse(network.commit());
    assertTrue(network.load());
    assertTrue(network.cancel());
  } 
  
  
  @Test
  public void testDoubleLoad() throws Exception {
    TCNetworkHeader header = mock(TCNetworkHeader.class);
    when(header.getDataBuffer()).thenReturn(mock(TCByteBuffer.class));
    Supplier<TCReference> payloadSupplier = mock(Supplier.class);
    when(payloadSupplier.get()).thenReturn(TCReferenceSupport.createReference(Arrays.asList(new TCByteBuffer[] {TCByteBufferFactory.getInstance(0)}), null));
    
    TCActionNetworkMessageImpl network = new TCActionNetworkMessageImpl(header, payloadSupplier);
    assertFalse(network.commit());
    assertTrue(network.load());
    try {
      assertTrue(network.load());
      throw new RuntimeException("should not execute");
    } catch (AssertionError err) {
      // expected
      err.printStackTrace();
    }
  }
}
