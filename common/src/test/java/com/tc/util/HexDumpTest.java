/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

import java.io.StringWriter;

/**
 * Unit test for {@link HexDump}.
 */
public class HexDumpTest extends TCTestCase {

  public void testZeroBytes() throws Exception {
    assertEquals("0 bytes: ", HexDump.dump(new byte[0]));
  }

  public void testOneByte() throws Exception {
    assertEquals("1 byte: 9c  .", HexDump.dump(new byte[] { ((byte) 0x9c) }));
  }

  public void testTwoBytes() throws Exception {
    assertEquals("2 bytes: 4142  AB", HexDump.dump(new byte[] { (byte) 'A', (byte) 'B' }));
  }

  public void testThreeBytes() throws Exception {
    assertEquals("3 bytes: 4142 43  ABC", HexDump.dump(new byte[] { (byte) 'A', (byte) 'B', (byte) 'C' }));
  }

  public void testFifteenBytes() throws Exception {
    assertEquals("15 bytes: 4142 4344 4546 4748 494a 4b4c 4d4e 4f  ABCDEFGHIJKLMNO", HexDump.dump(new byte[] {
        (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I',
        (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O' }));
  }

  public void testSixteenBytes() throws Exception {
    assertEquals("16 bytes: 4142 4344 4546 4748 494a 4b4c 4d4e 4f50  ABCDEFGHIJKLMNOP", HexDump.dump(new byte[] {
        (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I',
        (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P' }));
  }

  public void testSeventeenBytes() throws Exception {
    assertEquals("17 bytes:\n" + "00000000: 4142 4344 4546 4748 494a 4b4c 4d4e 4f50  ABCDEFGHIJKLMNOP\n"
                 + "00000010: 51                                       Q\n", HexDump.dump(new byte[] { (byte) 'A',
        (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J',
        (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q' }));
  }

  public void testMoreBytes() throws Exception {
    assertEquals("35 bytes:\n" + "00000000: 4142 4344 4546 4748 494a 4b4c 4d4e 4f50  ABCDEFGHIJKLMNOP\n"
                 + "00000010: 5152 5354 5556 5758 595a 5b5c 5d5e 5f60  QRSTUVWXYZ[\\]^_`\n"
                 + "00000020: 6162 63                                  abc\n", HexDump.dump(new byte[] { (byte) 'A',
        (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J',
        (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S',
        (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) '[', (byte) '\\',
        (byte) ']', (byte) '^', (byte) '_', (byte) '`', (byte) 'a', (byte) 'b', (byte) 'c' }));
  }

  public void testAllValues() throws Exception {
    byte[] myArray = new byte[512];

    for (int i = 0; i < myArray.length; ++i) {
      myArray[i] = (byte) (i % 256);
    }

    assertEquals("512 bytes:\n" + "00000000: 0001 0203 0405 0607 0809 0a0b 0c0d 0e0f  ................\n"
                 + "00000010: 1011 1213 1415 1617 1819 1a1b 1c1d 1e1f  ................\n"
                 + "00000020: 2021 2223 2425 2627 2829 2a2b 2c2d 2e2f   !\"#$%&'()*+,-./\n"
                 + "00000030: 3031 3233 3435 3637 3839 3a3b 3c3d 3e3f  0123456789:;<=>?\n"
                 + "00000040: 4041 4243 4445 4647 4849 4a4b 4c4d 4e4f  @ABCDEFGHIJKLMNO\n"
                 + "00000050: 5051 5253 5455 5657 5859 5a5b 5c5d 5e5f  PQRSTUVWXYZ[\\]^_\n"
                 + "00000060: 6061 6263 6465 6667 6869 6a6b 6c6d 6e6f  `abcdefghijklmno\n"
                 + "00000070: 7071 7273 7475 7677 7879 7a7b 7c7d 7e7f  pqrstuvwxyz{|}~.\n"
                 + "00000080: 8081 8283 8485 8687 8889 8a8b 8c8d 8e8f  ................\n"
                 + "00000090: 9091 9293 9495 9697 9899 9a9b 9c9d 9e9f  ................\n"
                 + "000000a0: a0a1 a2a3 a4a5 a6a7 a8a9 aaab acad aeaf  ................\n"
                 + "000000b0: b0b1 b2b3 b4b5 b6b7 b8b9 babb bcbd bebf  ................\n"
                 + "000000c0: c0c1 c2c3 c4c5 c6c7 c8c9 cacb cccd cecf  ................\n"
                 + "000000d0: d0d1 d2d3 d4d5 d6d7 d8d9 dadb dcdd dedf  ................\n"
                 + "000000e0: e0e1 e2e3 e4e5 e6e7 e8e9 eaeb eced eeef  ................\n"
                 + "000000f0: f0f1 f2f3 f4f5 f6f7 f8f9 fafb fcfd feff  ................\n"
                 + "00000100: 0001 0203 0405 0607 0809 0a0b 0c0d 0e0f  ................\n"
                 + "00000110: 1011 1213 1415 1617 1819 1a1b 1c1d 1e1f  ................\n"
                 + "00000120: 2021 2223 2425 2627 2829 2a2b 2c2d 2e2f   !\"#$%&'()*+,-./\n"
                 + "00000130: 3031 3233 3435 3637 3839 3a3b 3c3d 3e3f  0123456789:;<=>?\n"
                 + "00000140: 4041 4243 4445 4647 4849 4a4b 4c4d 4e4f  @ABCDEFGHIJKLMNO\n"
                 + "00000150: 5051 5253 5455 5657 5859 5a5b 5c5d 5e5f  PQRSTUVWXYZ[\\]^_\n"
                 + "00000160: 6061 6263 6465 6667 6869 6a6b 6c6d 6e6f  `abcdefghijklmno\n"
                 + "00000170: 7071 7273 7475 7677 7879 7a7b 7c7d 7e7f  pqrstuvwxyz{|}~.\n"
                 + "00000180: 8081 8283 8485 8687 8889 8a8b 8c8d 8e8f  ................\n"
                 + "00000190: 9091 9293 9495 9697 9899 9a9b 9c9d 9e9f  ................\n"
                 + "000001a0: a0a1 a2a3 a4a5 a6a7 a8a9 aaab acad aeaf  ................\n"
                 + "000001b0: b0b1 b2b3 b4b5 b6b7 b8b9 babb bcbd bebf  ................\n"
                 + "000001c0: c0c1 c2c3 c4c5 c6c7 c8c9 cacb cccd cecf  ................\n"
                 + "000001d0: d0d1 d2d3 d4d5 d6d7 d8d9 dadb dcdd dedf  ................\n"
                 + "000001e0: e0e1 e2e3 e4e5 e6e7 e8e9 eaeb eced eeef  ................\n"
                 + "000001f0: f0f1 f2f3 f4f5 f6f7 f8f9 fafb fcfd feff  ................\n", HexDump.dump(myArray));
  }

  public void testOffsetAndLength() throws Exception {
    byte[] myArray = new byte[512];

    for (int i = 0; i < myArray.length; ++i) {
      myArray[i] = (byte) (i % 256);
    }

    assertEquals("35 bytes:\n" + "00000000: 4142 4344 4546 4748 494a 4b4c 4d4e 4f50  ABCDEFGHIJKLMNOP\n"
                 + "00000010: 5152 5354 5556 5758 595a 5b5c 5d5e 5f60  QRSTUVWXYZ[\\]^_`\n"
                 + "00000020: 6162 63                                  abc\n", HexDump.dump(myArray, 0x41, 35));

    assertEquals("35 bytes:\n" + "00000000: 4142 4344 4546 4748 494a 4b4c 4d4e 4f50  ABCDEFGHIJKLMNOP\n"
                 + "00000010: 5152 5354 5556 5758 595a 5b5c 5d5e 5f60  QRSTUVWXYZ[\\]^_`\n"
                 + "00000020: 6162 63                                  abc\n", HexDump.dump(myArray, 0x141, 35));
  }

  public void testWriter() throws Exception {
    StringWriter writer = new StringWriter();
    HexDump.dump(new byte[] { (byte) 'A', (byte) 'B', (byte) 'C' }, writer);
    String myString = writer.toString();
    assertEquals("3 bytes: 4142 43  ABC", myString);
  }

  public void testOffsetAndLengthWithWriter() throws Exception {
    byte[] myArray = new byte[512];

    for (int i = 0; i < myArray.length; ++i) {
      myArray[i] = (byte) (i % 256);
    }

    StringWriter writer = new StringWriter();
    HexDump.dump(myArray, 0x41, 35, writer);
    String myString = writer.toString();
    assertEquals("35 bytes:\n" + "00000000: 4142 4344 4546 4748 494a 4b4c 4d4e 4f50  ABCDEFGHIJKLMNOP\n"
                 + "00000010: 5152 5354 5556 5758 595a 5b5c 5d5e 5f60  QRSTUVWXYZ[\\]^_`\n"
                 + "00000020: 6162 63                                  abc\n", myString);

    writer = new StringWriter();
    HexDump.dump(myArray, 0x141, 35, writer);
    myString = writer.toString();
    assertEquals("35 bytes:\n" + "00000000: 4142 4344 4546 4748 494a 4b4c 4d4e 4f50  ABCDEFGHIJKLMNOP\n"
                 + "00000010: 5152 5354 5556 5758 595a 5b5c 5d5e 5f60  QRSTUVWXYZ[\\]^_`\n"
                 + "00000020: 6162 63                                  abc\n", myString);

  }

}
