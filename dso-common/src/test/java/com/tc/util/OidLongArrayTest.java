/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
