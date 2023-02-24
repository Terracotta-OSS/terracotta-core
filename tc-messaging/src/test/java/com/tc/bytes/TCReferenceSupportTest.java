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
package com.tc.bytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TCReferenceSupportTest {
  
  public TCReferenceSupportTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testClose() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(512);
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(512);
    Collection<TCByteBuffer> items = new ArrayList<>();
    Collections.addAll(items, buf1, buf2);
    TCReference ref = TCReferenceSupport.createReference(items, null);
    Iterator<TCByteBuffer> it = ref.iterator();
    it.next();
    it.next(); // has two buffers
    Assert.assertFalse(it.hasNext());
    ref.close();
  }

  @Test
  public void testDuplicates() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(512);
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(512);
    Collection<TCByteBuffer> items = new ArrayList<>();
    Collections.addAll(items, buf1, buf2);
    TCReference ref = TCReferenceSupport.createReference(items, null);
    TCReference dup = ref.duplicate();
    ref.close();
    Iterator<TCByteBuffer> it = dup.iterator();
    it.next();
    it.next(); // has two buffers
    Assert.assertFalse(it.hasNext());
    dup.close();
  } 
  
  @Test
  public void testArrayCollection() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(512);
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(512);
    TCByteBuffer[] bufs = new TCByteBuffer[] {buf1, buf2};
    TCReference ref = TCReferenceSupport.createReference(Arrays.asList(bufs), null);

    TCReference dup = ref.duplicate();
    ref.close();
    Iterator<TCByteBuffer> it = dup.iterator();
    it.next();
    it.next(); // has two buffers
    Assert.assertFalse(it.hasNext());
    dup.close();
  }  
  
  
  @Test
  public void testDoubleClose() {
    TCByteBuffer buf1 = mock(TCByteBuffer.class);
    when(buf1.hasRemaining()).thenReturn(Boolean.TRUE);
    when(buf1.duplicate()).thenReturn(buf1);
    when(buf1.slice()).thenReturn(buf1);
    when(buf1.asReadOnlyBuffer()).thenReturn(buf1);
    TCByteBuffer[] bufs = new TCByteBuffer[] {buf1};
    TCReference ref = TCReferenceSupport.createReference(Arrays.asList(bufs), null);

    TCReference dup = ref.duplicate();
    ref.close();
    ref.close();
    verify(buf1, never()).reInit();
    Iterator<TCByteBuffer> it = dup.iterator();
    it.next();
    dup.close();
    verify(buf1).reInit();
  }
  
  @Test
  public void testReferenceDroppedReferenceGC() throws Exception {
    LinkedList<TCByteBuffer> returns = new LinkedList<>();
    TCByteBuffer buf1 = mock(TCByteBuffer.class);
    TCByteBuffer buf2 = mock(TCByteBuffer.class);
    when(buf1.hasRemaining()).thenReturn(Boolean.TRUE);
    when(buf1.duplicate()).thenReturn(buf1);
    when(buf1.slice()).thenReturn(buf1);
    when(buf1.reInit()).thenReturn(buf1);
    when(buf1.asReadOnlyBuffer()).thenReturn(buf1);
    when(buf2.hasRemaining()).thenReturn(Boolean.TRUE);
    when(buf2.duplicate()).thenReturn(buf2);
    when(buf2.slice()).thenReturn(buf2);
    when(buf2.reInit()).thenReturn(buf2);
    when(buf2.asReadOnlyBuffer()).thenReturn(buf2);
    TCByteBuffer[] bufs = new TCByteBuffer[] {buf1, buf2};
    TCReferenceSupport.startMonitoringReferences();
    TCReference ref = TCReferenceSupport.createReference(Arrays.asList(bufs), returns::add);
    TCReference dup = ref.duplicate();
    
    ref = null;
    dup = null;
    
    int count = 0;
    System.gc();
    while (count < 2) {
      System.gc();
      count += TCReferenceSupport.checkReferences();
    }
    verify(buf2).reInit();
  }  
  
  
  @Test
  public void testTruncate() throws Exception {
    LinkedList<TCByteBuffer> returns = new LinkedList<>();
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(512);
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(1024);

    TCByteBuffer[] bufs = new TCByteBuffer[] {buf1, buf2};
    TCReference ref = TCReferenceSupport.createReference(Arrays.asList(bufs), returns::add);
    TCReference dup = ref.duplicate();
    dup.iterator().next().position(32);
    TCReference truncate = dup.truncate(312);
    
    Assert.assertEquals(312, truncate.available());
    Iterator<TCByteBuffer> sec = truncate.iterator();
    Assert.assertEquals(312, sec.next().remaining());
    Assert.assertFalse(sec.next().hasRemaining());
    
    TCReference trimmed = truncate.duplicate();
    sec = trimmed.iterator();
    Assert.assertEquals(312, sec.next().limit());
    Assert.assertFalse(sec.hasNext());
  }  
}
