/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package com.tc.bytes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for recent changes to TCReferenceSupport:
 * - RefRef.iterator() manual implementation
 * - RefRef.available() and hasRemaining()
 * - Ref.available() and hasRemaining()
 */
public class TCReferenceSupportChangesTest {

  // ========== RefRef.iterator() Tests ==========

  @Test
  public void testRefRefIterator_EmptyAggregate() {
    try (TCReference aggregate = TCReferenceSupport.createAggregateReference(Collections.emptyList())) {
      Iterator<TCByteBuffer> it = aggregate.iterator();
      Assert.assertFalse("Empty aggregate should have no elements", it.hasNext());
    }
  }

  @Test
  public void testRefRefIterator_SingleReferenceWithSingleBuffer() {
    TCByteBuffer buf = TCByteBufferFactory.getInstance(100);
    buf.put(new byte[50]); // Write 50 bytes
    buf.flip();

    try (TCReference ref = TCReferenceSupport.createReference(null, buf); TCReference aggregate = TCReferenceSupport.createAggregateReference(ref)) {

      Iterator<TCByteBuffer> it = aggregate.iterator();
      Assert.assertTrue("Should have one buffer", it.hasNext());
      TCByteBuffer retrieved = it.next();
      Assert.assertEquals("Buffer should have 50 bytes", 50, retrieved.remaining());
      Assert.assertFalse("Should have no more buffers", it.hasNext());

    }
  }

  @Test
  public void testRefRefIterator_MultipleReferencesWithMultipleBuffers() {
    // Create first reference with 2 buffers
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[50]).flip();
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.put(new byte[75]).flip();
    TCReference ref2;
    // Create second reference with 1 buffer
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1, buf2)) {
      // Create second reference with 1 buffer
      TCByteBuffer buf3 = TCByteBufferFactory.getInstance(100);
      buf3.put(new byte[25]).flip();
      ref2 = TCReferenceSupport.createReference(null, buf3);
      // Iterate and verify
      try ( // Create aggregate
              TCReference aggregate = TCReferenceSupport.createAggregateReference(ref1, ref2)) {
        // Iterate and verify
        Iterator<TCByteBuffer> it = aggregate.iterator();
        List<Integer> sizes = new ArrayList<>();
        while (it.hasNext()) {
          sizes.add(it.next().remaining());
        } Assert.assertEquals("Should have 3 buffers", 3, sizes.size());
        Assert.assertEquals("First buffer should be 50 bytes", Integer.valueOf(50), sizes.get(0));
        Assert.assertEquals("Second buffer should be 75 bytes", Integer.valueOf(75), sizes.get(1));
        Assert.assertEquals("Third buffer should be 25 bytes", Integer.valueOf(25), sizes.get(2));
      }
    }
    ref2.close();
  }

  @Test
  public void testRefRefIterator_SkipsEmptyBuffers() {
    // Create reference with empty buffer
    TCByteBuffer emptyBuf = TCByteBufferFactory.getInstance(100);
    emptyBuf.flip(); // Make it empty (position=0, limit=0)

    TCByteBuffer nonEmptyBuf = TCByteBufferFactory.getInstance(100);
    nonEmptyBuf.put(new byte[50]).flip();

    try (TCReference ref = TCReferenceSupport.createReference(null, emptyBuf, nonEmptyBuf); TCReference aggregate = TCReferenceSupport.createAggregateReference(ref)) {

      Iterator<TCByteBuffer> it = aggregate.iterator();
      Assert.assertTrue("Should have one non-empty buffer", it.hasNext());
      Assert.assertEquals("Should get the non-empty buffer", 50, it.next().remaining());
      Assert.assertFalse("Should have no more buffers", it.hasNext());

    }
  }

  @Test(expected = NoSuchElementException.class)
  public void testRefRefIterator_ThrowsNoSuchElementException() {
    TCByteBuffer buf = TCByteBufferFactory.getInstance(100);
    buf.put(new byte[50]).flip();
    TCReference ref = TCReferenceSupport.createReference(null, buf);
    try (TCReference aggregate = TCReferenceSupport.createAggregateReference(ref)) {
      Iterator<TCByteBuffer> it = aggregate.iterator();
      it.next(); // Get first element
      it.next(); // Should throw NoSuchElementException
    } finally {
      ref.close();
    }
  }

  @Test
  public void testRefRefIterator_MultipleIterations() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[50]).flip();
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.put(new byte[75]).flip();

    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2); TCReference aggregate = TCReferenceSupport.createAggregateReference(ref)) {

      // First iteration
      int count1 = 0;
      for (TCByteBuffer b : aggregate) {
        count1++;
      }

      // Second iteration
      int count2 = 0;
      for (TCByteBuffer b : aggregate) {
        count2++;
      }

      Assert.assertEquals("First iteration should find 2 buffers", 2, count1);
      Assert.assertEquals("Second iteration should find 2 buffers", 2, count2);

    }
  }

  @Test
  public void testRefRefIterator_NestedAggregates() {
    // Create inner aggregate
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[10]).flip();
    TCReference ref2;
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1); TCReference innerAggregate = TCReferenceSupport.createAggregateReference(ref1)) {
      // Create outer aggregate containing inner aggregate
      TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
      buf2.put(new byte[20]).flip();
      ref2 = TCReferenceSupport.createReference(null, buf2);
      // Iterate
      try (TCReference outerAggregate = TCReferenceSupport.createAggregateReference(innerAggregate, ref2)) {
        // Iterate
        List<Integer> sizes = new ArrayList<>();
        for (TCByteBuffer b : outerAggregate) {
          sizes.add(b.remaining());
        } Assert.assertEquals("Should have 2 buffers", 2, sizes.size());
        Assert.assertEquals("First buffer should be 10 bytes", Integer.valueOf(10), sizes.get(0));
        Assert.assertEquals("Second buffer should be 20 bytes", Integer.valueOf(20), sizes.get(1));
      }
    }
    ref2.close();
  }

  // ========== RefRef.available() Tests ==========

  @Test
  public void testRefRefAvailable_EmptyAggregate() {
    try (TCReference aggregate = TCReferenceSupport.createAggregateReference(Collections.emptyList())) {
      Assert.assertEquals("Empty aggregate should have 0 bytes available", 0L, aggregate.available());
    }
  }

  @Test
  public void testRefRefAvailable_SingleReference() {
    TCByteBuffer buf = TCByteBufferFactory.getInstance(100);
    buf.put(new byte[50]).flip();
    try (TCReference ref = TCReferenceSupport.createReference(null, buf); TCReference aggregate = TCReferenceSupport.createAggregateReference(ref)) {

      Assert.assertEquals("Should have 50 bytes available", 50L, aggregate.available());

    }
  }

  @Test
  public void testRefRefAvailable_MultipleReferencesWithVaryingSizes() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[25]).flip();
    TCReference ref2;
    TCReference ref3;
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1)) {
      TCByteBuffer buf2 = TCByteBufferFactory.getInstance(200);
      buf2.put(new byte[150]).flip();
      ref2 = TCReferenceSupport.createReference(null, buf2);
      TCByteBuffer buf3 = TCByteBufferFactory.getInstance(50);
      buf3.put(new byte[30]).flip();
      ref3 = TCReferenceSupport.createReference(null, buf3);
      try (TCReference aggregate = TCReferenceSupport.createAggregateReference(ref1, ref2, ref3)) {
        Assert.assertEquals("Should have 205 bytes total", 205L, aggregate.available());
      }
    }
    ref2.close();
    ref3.close();
  }

  @Test
  public void testRefRefAvailable_LargeTotals() {
    // Create references with large buffers
    List<TCReference> refs = new ArrayList<>();
    long expectedTotal = 0;

    for (int i = 0; i < 10; i++) {
      TCByteBuffer buf = TCByteBufferFactory.getInstance(1024 * 1024); // 1MB
      buf.position(buf.capacity());
      buf.flip();
      refs.add(TCReferenceSupport.createReference(null, buf));
      expectedTotal += buf.remaining();
    }

    try (TCReference aggregate = TCReferenceSupport.createAggregateReference(refs)) {
      Assert.assertEquals("Should have correct total", expectedTotal, aggregate.available());
    }
    refs.forEach(TCReference::close);
  }

  // ========== RefRef.hasRemaining() Tests ==========

  @Test
  public void testRefRefHasRemaining_AllReferencesEmpty() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.flip(); // Empty
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.flip(); // Empty

    TCReference ref2;
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1)) {
      ref2 = TCReferenceSupport.createReference(null, buf2);
      try (TCReference aggregate = TCReferenceSupport.createAggregateReference(ref1, ref2)) {
        Assert.assertFalse("Empty aggregate should have no remaining", aggregate.hasRemaining());
      }
    }
    ref2.close();
  }

  @Test
  public void testRefRefHasRemaining_FirstReferenceHasRemaining() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[50]).flip();
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.flip(); // Empty

    TCReference ref2;
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1)) {
      ref2 = TCReferenceSupport.createReference(null, buf2);
      try (TCReference aggregate = TCReferenceSupport.createAggregateReference(ref1, ref2)) {
        Assert.assertTrue("Should have remaining data", aggregate.hasRemaining());
      }
    }
    ref2.close();
  }

  @Test
  public void testRefRefHasRemaining_LastReferenceHasRemaining() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.flip(); // Empty
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.put(new byte[50]).flip();

    TCReference ref2;
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1)) {
      ref2 = TCReferenceSupport.createReference(null, buf2);
      try (TCReference aggregate = TCReferenceSupport.createAggregateReference(ref1, ref2)) {
        Assert.assertTrue("Should have remaining data", aggregate.hasRemaining());
      }
    }
    ref2.close();
  }

  @Test
  public void testRefRefHasRemaining_MiddleReferenceHasRemaining() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.flip(); // Empty
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.put(new byte[50]).flip();
    TCByteBuffer buf3 = TCByteBufferFactory.getInstance(100);
    buf3.flip(); // Empty

    TCReference ref2;
    TCReference ref3;
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1)) {
      ref2 = TCReferenceSupport.createReference(null, buf2);
      ref3 = TCReferenceSupport.createReference(null, buf3);
      try (TCReference aggregate = TCReferenceSupport.createAggregateReference(ref1, ref2, ref3)) {
        Assert.assertTrue("Should have remaining data", aggregate.hasRemaining());
      }
    }
    ref2.close();
    ref3.close();
  }

  // ========== Ref.available() Tests ==========

  @Test
  public void testRefAvailable_EmptyBufferList() {
    try (TCReference ref = TCReferenceSupport.createReference(Collections.emptyList(), null)) {
      Assert.assertEquals("Empty reference should have 0 bytes", 0L, ref.available());
    }
  }

  @Test
  public void testRefAvailable_SingleBuffer() {
    TCByteBuffer buf = TCByteBufferFactory.getInstance(100);
    buf.put(new byte[75]).flip();
    try (TCReference ref = TCReferenceSupport.createReference(null, buf)) {
      Assert.assertEquals("Should have 75 bytes available", 75L, ref.available());
    }
  }

  @Test
  public void testRefAvailable_MultipleBuffers() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[50]).flip();
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(200);
    buf2.put(new byte[150]).flip();
    TCByteBuffer buf3 = TCByteBufferFactory.getInstance(50);
    buf3.put(new byte[25]).flip();

    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2, buf3)) {
      Assert.assertEquals("Should have 225 bytes total", 225L, ref.available());
    }
  }

  @Test
  public void testRefAvailable_BuffersWithDifferentPositionsAndLimits() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[100]).flip();
    buf1.position(25); // Skip 25 bytes

    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(200);
    buf2.put(new byte[200]).flip();
    buf2.limit(150); // Limit to 150 bytes
    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2)) {
      Assert.assertEquals("Should have 75 + 150 = 225 bytes", 225L, ref.available());
    }
  }

  // ========== Ref.hasRemaining() Tests ==========

  @Test
  public void testRefHasRemaining_AllBuffersEmpty() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.flip(); // Empty
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.flip(); // Empty
    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2)) {
      Assert.assertFalse("Empty buffers should have no remaining", ref.hasRemaining());
    }
  }

  @Test
  public void testRefHasRemaining_FirstBufferHasRemaining() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[50]).flip();
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.flip(); // Empty
    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2)) {
      Assert.assertTrue("Should have remaining data", ref.hasRemaining());
    }
  }

  @Test
  public void testRefHasRemaining_LastBufferHasRemaining() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.flip(); // Empty
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.put(new byte[50]).flip();

    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2)) {
      Assert.assertTrue("Should have remaining data", ref.hasRemaining());
    }
  }

  @Test
  public void testRefHasRemaining_MixedEmptyAndNonEmpty() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.flip(); // Empty
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.put(new byte[50]).flip();
    TCByteBuffer buf3 = TCByteBufferFactory.getInstance(100);
    buf3.flip(); // Empty
    TCByteBuffer buf4 = TCByteBufferFactory.getInstance(100);
    buf4.put(new byte[25]).flip();

    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2, buf3, buf4)) {
      Assert.assertTrue("Should have remaining data", ref.hasRemaining());
    }
  }

  @Test
  public void testRefHasRemaining_SingleByteRemaining() {
    TCByteBuffer buf = TCByteBufferFactory.getInstance(100);
    buf.put((byte) 42).flip();

    try (TCReference ref = TCReferenceSupport.createReference(null, buf)) {
      Assert.assertTrue("Should detect single byte remaining", ref.hasRemaining());
      Assert.assertEquals("Should have 1 byte available", 1L, ref.available());
    }
  }

  // ========== Integration Tests ==========

  @Test
  public void testAvailableAndHasRemainingConsistency() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[50]).flip();
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.flip(); // Empty
    // available() and hasRemaining() should be consistent
    try (TCReference ref = TCReferenceSupport.createReference(null, buf1, buf2)) {
      // available() and hasRemaining() should be consistent
      if (ref.available() > 0) {
        Assert.assertTrue("hasRemaining should be true when available > 0", ref.hasRemaining());
      } else {
        Assert.assertFalse("hasRemaining should be false when available == 0", ref.hasRemaining());
      }
    }
  }

  @Test
  public void testAggregateAvailableMatchesIteratorSum() {
    TCByteBuffer buf1 = TCByteBufferFactory.getInstance(100);
    buf1.put(new byte[50]).flip();
    TCByteBuffer buf2 = TCByteBufferFactory.getInstance(100);
    buf2.put(new byte[75]).flip();

    TCReference ref2;
    try (TCReference ref1 = TCReferenceSupport.createReference(null, buf1)) {
      ref2 = TCReferenceSupport.createReference(null, buf2);
      try (TCReference aggregate = TCReferenceSupport.createAggregateReference(ref1, ref2)) {
        long availableFromMethod = aggregate.available();
        long sumFromIterator = 0;
        for (TCByteBuffer b : aggregate) {
          sumFromIterator += b.remaining();
        } Assert.assertEquals("available() should match iterator sum", sumFromIterator, availableFromMethod);
      }
    }
    ref2.close();
  }
}
