/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Utilities to compress/decompress a String.  
 * 
 * The general flow for compression is:
 * - stringToUncompressedBin
 * - compressBin
 * - packCompressed
 * 
 * The general flow for decompression is:
 * - unpackCompressed
 * - uncompressBin
 * - uncompressedBinToString
 * 
 * And there are some helpers for us by tests and the JavaLangStringAdapter:
 * - compressAndPack - packages up the compress and pack steps
 * - isCompressed - to check whether we've already decompressed
 * - unpackAndDecompress - packages up the unpackCompressed+uncompressBin steps
 */
public class StringCompressionUtil {

  static final char COMPRESSION_FLAG = '\uC0FF'; //get it? "cough"!  illegal in UTF-8
  static final byte ODD_MARKER = (byte)0xC0;
  
  /**
   * Convert String to uncompressed byte[] using UTF-8
   * @param string Starting string
   * @return Uncompressed byte[] in UTF-8
   */
  public static byte[] stringToUncompressedBin(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch(UnsupportedEncodingException e) {
      throw new AssertionError(e.getMessage());
    }
  }
  
  /**
   * Convert uncompressed byte[] to compressed byte[]
   * @param uncompressed Uncompressed byte[]
   * @return The compressed byte[] and size
   */
  public static CompressedData compressBin(byte[] uncompressed) {
    TCByteArrayOutputStream byteArrayOS = new TCByteArrayOutputStream(4096);
    // Stride is 512 bytes by default, should I increase ?
    DeflaterOutputStream dos = new DeflaterOutputStream(byteArrayOS);
    try {
      dos.write(uncompressed);
      dos.close();
      byte[] compressed = byteArrayOS.getInternalArray();
      return new CompressedData(compressed, byteArrayOS.size(), uncompressed.length);
    } catch(IOException e) {
      throw new AssertionError(e.getMessage());
    }
  }

  public static char[] packCompressed(CompressedData compressedData) {
    byte[] bytes = compressedData.getCompressedData();
    int compressedLength = compressedData.getCompressedSize();
    int uncompressedByteLength = compressedData.getUncompressedSize();
    
    int remainder = isOdd(compressedLength);
    // size = COMPRESSION_FLAG + <int for uncompressed size> + <data> + <extra if odd>
    char[] result = new char[1 + 2 + (compressedLength/2) + remainder]; 
    
    // Write initial compression flag magic value
    result[0] = COMPRESSION_FLAG;
    intToChars(uncompressedByteLength, result, 1);
    
    int charIndex = 3;
    int startByte = 0;
    if(remainder==1) {
      result[charIndex++] = encodeTwoBytesAsChar(ODD_MARKER, bytes[0]);
      startByte = 1;
    }
    
    for (int i=startByte; i< compressedLength; i=i+2){
      result[charIndex++] = encodeTwoBytesAsChar(bytes[i], getLowByte(bytes, compressedLength, i));
    }
    return result;
  }

  static char encodeTwoBytesAsChar(int highByte, byte lowByte){
    highByte = highByte << 8; //shift first byte up 8 bits, making room for the second byte
    highByte = appendByteToLowBitsOfInt(lowByte, highByte);
    return (char) highByte; //cast to char (drop all but low 16 bits)
  }
  
  private static byte getLowByte(byte[] bytes, int compressedLength, int i) {
    byte lowByte = 0x00;
    if (i+1 < compressedLength){
      lowByte = bytes[i+1];
    }
    return lowByte;
  }
  
  static int appendByteToLowBitsOfInt(byte lowByte, int anInt){
    int bitmask = 0x000000FF & lowByte; //zero out all bits in this bitmask to the left of the 8 bits coming from the byte
    anInt = anInt | bitmask; //now paste those 8 bits into the low bits of "anInt" (whose low 8 bits should be zeroes due to the previous shift)
    return anInt;
  }
  
  static void intToChars(int value, char[] chars, int offset) {
    chars[offset] = (char) (value >> 16);
    chars[offset+1] = (char) (value & 0x0000FFFF);
  }
  
  static int charsToInt(char[] chars, int offset) {
    int high = (chars[offset] << 16);
    return high + chars[offset+1];
  }
  
  static int isOdd(int num) {
    return (num & 0x1);
  }
  
  public static CompressedData unpackCompressed(char[] chars) {
    // skip the first char since it is just the compression flag
    
    // read the second and third char as the length
    int uncompressedByteLength = charsToInt(chars, 1);
    
    byte[] bytes = null;
    int byteIndex = 0;
    for(int i=3; i<chars.length; i++) {
      // Read the high byte
      int anInt = chars[i];
      byte highByte = (byte)(anInt>>8);
      
      if(byteIndex == 0) {
        // First byte only - choose size by checking padding
        int byteLength = (chars.length-3) * 2;
        if(highByte == ODD_MARKER) {
          // If odd - skip and reduce length by 1
          bytes = new byte[byteLength-1];
        } else {
          // If even, save the byte
          bytes = new byte[byteLength];
          bytes[byteIndex++] = highByte;
        }
      } else {
        bytes[byteIndex++] = highByte;
      }
      
      // Read the low byte
      if (byteIndex < bytes.length){
        bytes[byteIndex++] = (byte)(anInt);
      }
    }
    
    if(bytes == null) {
      bytes = new byte[0];
    }
    
    return new CompressedData(bytes, bytes.length, uncompressedByteLength);
  }
  
  public static byte[] uncompressBin(CompressedData data) {
    int uncompressedLength = data.getUncompressedSize();
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(data.getCompressedData());
      InflaterInputStream iis = new InflaterInputStream(bais);
      byte uncompressed[] = new byte[uncompressedLength];
      int read;
      int offset = 0;
      while (uncompressedLength > 0 && (read = iis.read(uncompressed, offset, uncompressedLength)) != -1) {
        offset += read;
        uncompressedLength -= read;
      }
      iis.close();
      return uncompressed;
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
  
  public static String uncompressedBinToString(byte[] uncompressedBytes) {
    try {
      return new String(uncompressedBytes, "UTF-8");
    } catch(UnsupportedEncodingException e) {
      throw new AssertionError(e.getMessage());
    }
  }

  
  /**
   * Helper method to check whether a char[] is a packed compressed char[]
   * @param compressedString Possibly compressed char[]
   * @return True if packed and compressed, false otherwise
   */
  public static boolean isCompressed(char[] compressedString) {
    //Give it the "cough" test, heh heh
    return (compressedString.length > 0 && COMPRESSION_FLAG == compressedString[0]);
  }

  /**
   * Start with compressed data packed into a char[].  If compressed, unpack from char[]
   * to byte[], then decompress to byte[].
   * @param compressedString Compressed string data
   * @return Original byte[] data in UTF-8 from string 
   *   OR null to indicate that string is already compressed
   */
  public static byte[] unpackAndDecompress(char[] compressedString){
    if (isCompressed(compressedString)){
      CompressedData data = unpackCompressed(compressedString);
      return uncompressBin(data);
    }
    return null;
  }
  


}
