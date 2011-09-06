/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;



import junit.framework.TestCase;

public class StringCompressionUtilTest extends TestCase {
 
  public void helpTestRoundtripString(String string) {
    byte[] uncompressedBin = StringCompressionUtil.stringToUncompressedBin(string);
    CompressedData compressed = StringCompressionUtil.compressBin(uncompressedBin);
    char[] packed = StringCompressionUtil.packCompressed(compressed);
    
    assertEquals(true, StringCompressionUtil.isCompressed(packed));
    
    CompressedData compressed2 = StringCompressionUtil.unpackCompressed(packed);
    assertEquals(compressed.getCompressedSize(), compressed2.getCompressedSize());
    assertEquals(compressed.getUncompressedSize(), compressed2.getUncompressedSize());
    assertBytesEqual(compressed.getCompressedData(), compressed2.getCompressedData(), compressed.getCompressedSize());
    
    byte[] uncompressed2 = StringCompressionUtil.uncompressBin(compressed2);
    assertBytesEqual(uncompressedBin, uncompressed2, uncompressedBin.length);
  }
  
  private void assertBytesEqual(byte[] b1, byte[] b2, int length) {
    assertTrue(b1.length >= length);
    assertTrue(b2.length >= length);
    for(int b=0; b<length; b++) {
      assertEquals(b1[b], b2[b]);
    }    
  }
  
  public void testEmpty() {
    helpTestRoundtripString("");
  }
  
  public void testSimple() {
    helpTestRoundtripString("abcd");
  }
  
  public void testUnicode() {
    helpTestRoundtripString("\u7aba");
  }  
  
  public void testBig() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<9999; i++){
      sb.append("foo");
      sb.append(i);
    }
    String bigStr = sb.toString();
    helpTestRoundtripString(bigStr);
  }
  
  public void testConvertTwoBytesToExpectedChar() throws Exception {
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) -1, '\uFFFF');
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) 0x7F, '\uFF7F');
    helpTestConvertTwoBytesToExpectedChar((byte)1,(byte) 1, '\u0101');
    helpTestConvertTwoBytesToExpectedChar((byte)0,(byte) 0, '\u0000');
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) 0, '\uFF00');
    helpTestConvertTwoBytesToExpectedChar((byte)0x80,(byte) 0x80, '\u8080');
  }
  
  private void helpTestConvertTwoBytesToExpectedChar(byte firstByte, byte secondByte, char expected){
    assertEquals(expected, StringCompressionUtil.encodeTwoBytesAsChar(firstByte, secondByte));
    byte[] bytes = new byte[]{firstByte, secondByte};
    char[] result = StringCompressionUtil.packCompressed(new CompressedData(bytes, 1));
    assertEquals(4, result.length);
    CompressedData out = StringCompressionUtil.unpackCompressed(result);
    assertBytesEqual(bytes, out.getCompressedData(), bytes.length);
    assertEquals(1, out.getUncompressedSize());
  }
  
  public void testIntToChars() throws Exception {
    helpTestIntToChars(0x80803080, '\u8080', '\u3080');
    helpTestIntToChars(0xFFFFFFFF, '\uFFFF', '\uFFFF');
    helpTestIntToChars(0x0000FFFF, '\u0000', '\uFFFF');
    helpTestIntToChars(0x00000000, '\u0000', '\u0000');
  }
  
  private void helpTestIntToChars(int convertToChars, char expectedHigh, char expectedLow) {
    final int offset = 3;
    final char[] chars = new char[offset + 2];
    StringCompressionUtil.intToChars(convertToChars, chars, offset);
    assertEquals(expectedHigh, chars[offset + 0]);
    assertEquals(expectedLow, chars[offset + 1]);
  }
  
  public void testCharsToInt() throws Exception {
    helpTestCharsToInt('\u0000', '\u0000', 0x00000000);
    helpTestCharsToInt('\u0FAB', '\u0F00', 0x0FAB0F00);
  }

  private void helpTestCharsToInt(char highChar, char lowChar, int expected) {
    final int offset = 3;
    final char[] chars = new char[offset + 2];
    chars[offset] = highChar;
    chars[offset + 1] = lowChar;
    int actual = StringCompressionUtil.charsToInt(chars, offset);
    assertEquals(expected, actual);
  }

  public void testDoNotDecompressAlreadyDecompressedString() throws Exception {
    String test = "foo";
    char[] testChars = new char[test.length()];
    test.getChars(0, test.length(), testChars, 0);
    assertNull(StringCompressionUtil.unpackAndDecompress(testChars));
  }
  
  public void testDoNotDecompressEmptyString() throws Exception {
    String test = "";
    char[] testChars = new char[test.length()];
    test.getChars(0, test.length(), testChars, 0);
    assertNull(StringCompressionUtil.unpackAndDecompress(testChars));
  }  
  
  public void testIsOdd() {
    assertEquals(1, StringCompressionUtil.isOdd(5));
    assertEquals(0, StringCompressionUtil.isOdd(4));
    assertEquals(1, StringCompressionUtil.isOdd(1));
    assertEquals(0, StringCompressionUtil.isOdd(0));
    assertEquals(1, StringCompressionUtil.isOdd(-1));
    assertEquals(0, StringCompressionUtil.isOdd(-2));
  }
  
  public void testAppendByteToLowBitsOfInt() throws Exception {
    assertEquals(0x000000FF, StringCompressionUtil.appendByteToLowBitsOfInt((byte)0xFF, 0x00000000));
    assertEquals(0xFFFF00FF, StringCompressionUtil.appendByteToLowBitsOfInt((byte)0xFF, 0xFFFF0000));
    assertEquals(0xFFFF00FF, StringCompressionUtil.appendByteToLowBitsOfInt((byte)0xFF, 0xFFFF00EE));
    assertEquals(0xFFFFFFFF, StringCompressionUtil.appendByteToLowBitsOfInt((byte)0xFF, 0xFFFFFF00));
    
  }
  
}
