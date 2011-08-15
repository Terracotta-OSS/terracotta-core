/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

import java.util.Random;

public class OidLongArrayTest extends TCTestCase {
  Random r = new Random();
  
  private OidLongArray bitSetTest(int arySize) {   
    long oid = 1000;
    OidLongArray bits = new OidLongArray(arySize, oid);
    
    for(int i = 0; i < 100; ++i) {
      int index = r.nextInt(bits.totalBits());
      bits.setBit(index);
      Assert.assertTrue("Failed setBit", bits.isSet(index));
    }
    return bits;
  }
  
  private OidLongArray bitClrTest(int arySize) {
    long oid = 1000;
    byte[] bytes = new byte[arySize*8];
    for(int i = 0; i < bytes.length; ++i) {
      bytes[i] |= 0xff;
    }
    byte[] key = Conversion.long2Bytes(oid);
    OidLongArray bits = new OidLongArray(key, bytes);
    
    for(int i = 0; i < 100; ++i) {
      int index = r.nextInt(bits.totalBits());
      bits.clrBit(index);
      Assert.assertTrue("Failed clrBit", !bits.isSet(index));
    }
    
    return bits;
  }


  public void testBitsOperations() {
    bitSetTest(4);
    bitSetTest(128);
    
    bitClrTest(4);
    bitClrTest(128);
  }
  
  public void testIsZero() {
    OidLongArray bits = bitClrTest(64);
    for(int i = 0; i < bits.length(); ++i) {
      bits.set(i, 0L);
    }
    Assert.assertTrue("Failed isZero", bits.isZero());
  }
  
  public void testIsEnded() {
    OidLongArray bits = new OidLongArray(null, null);
    Assert.assertTrue("Failed isEnded", bits.isEnded());
  }
  
  public void testCopyOutApplyIn() {
    long oid = 1000;
    int arySize = 128;
    int destSize = 4;
    OidLongArray bits = new OidLongArray(arySize, oid);
    for(int i = 0; i < bits.length(); ++i) {
      long j = i;
      bits.set(i,j);
    }
    
    OidLongArray dest = new OidLongArray(destSize, oid);
    int offset = r.nextInt(bits.length() / destSize) * destSize;
    bits.copyOut(dest, offset);
    
    // verify
    long expected = offset;
    for(int i = 0; i < destSize; ++i) {    
      Assert.assertTrue("Failed copyOut"+expected+" got "+dest.get(i), 
                        expected == dest.get(i));
      ++expected;
    }
    
    OidLongArray src = new OidLongArray(destSize, oid);
    for(int i = 0; i < destSize; ++i) {    
      src.set(i, dest.get(i)*2);
    }
    bits.applyIn(src, offset);
    
    // verify
    for(int i = 0; i < destSize; ++i) {
      expected = offset + i;
      expected |= (offset + i) * 2;
      Assert.assertTrue("Failed applyIn expected "+expected+" got "+bits.get(offset+i), 
                        expected == bits.get(offset+i));
    }
  }
  
  public void testKeyToBytes() {
    long oid = 1000;
    OidLongArray bits = new OidLongArray(8, oid);
    byte[] keyBytes = bits.keyToBytes(1);
    Assert.assertEquals(oid + 1, Conversion.bytes2Long(keyBytes));
  }

}
