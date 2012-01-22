/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.expression.ast;

public class ExpressionParserTokenManager implements ExpressionParserConstants {
  public static java.io.PrintStream debugStream = System.out;

  public static void setDebugStream(java.io.PrintStream ds) {
    debugStream = ds;
  }

  private static final int jjStopStringLiteralDfa_5(int pos, long active0, long active1) {
    switch (pos) {
      case 0:
        if ((active0 & 0x7dff800L) != 0L) {
          jjmatchedKind = 28;
          return 22;
        }
        if ((active0 & 0x10L) != 0L)
          return 1;
        if ((active0 & 0x200000L) != 0L) {
          jjmatchedKind = 28;
          return 5;
        }
        return -1;
      case 1:
        if ((active0 & 0x7fff800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 1;
          return 22;
        }
        return -1;
      case 2:
        if ((active0 & 0x1000000L) != 0L) {
          jjmatchedKind = 27;
          jjmatchedPos = 2;
          return -1;
        }
        if ((active0 & 0x6fff800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 2;
          return 22;
        }
        return -1;
      case 3:
        if ((active0 & 0x1000000L) != 0L) {
          if (jjmatchedPos < 2) {
            jjmatchedKind = 27;
            jjmatchedPos = 2;
          }
          return -1;
        }
        if ((active0 & 0x6ff9800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 3;
          return 22;
        }
        return -1;
      case 4:
        if ((active0 & 0x65f8800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 4;
          return 22;
        }
        return -1;
      case 5:
        if ((active0 & 0x6578800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 5;
          return 22;
        }
        return -1;
      case 6:
        if ((active0 & 0x6168800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 6;
          return 22;
        }
        return -1;
      case 7:
        if ((active0 & 0x6160800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 7;
          return 22;
        }
        return -1;
      case 8:
        if ((active0 & 0x2160800L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 8;
          return 22;
        }
        return -1;
      case 9:
        if ((active0 & 0x160000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 9;
          return 22;
        }
        return -1;
      case 10:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 10;
          return 22;
        }
        return -1;
      case 11:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 11;
          return 22;
        }
        return -1;
      case 12:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 12;
          return 22;
        }
        return -1;
      case 13:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 13;
          return 22;
        }
        return -1;
      case 14:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 14;
          return 22;
        }
        return -1;
      case 15:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 15;
          return 22;
        }
        return -1;
      case 16:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 16;
          return 22;
        }
        return -1;
      case 17:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 17;
          return 22;
        }
        return -1;
      case 18:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 18;
          return 22;
        }
        return -1;
      case 19:
        if ((active0 & 0x40000L) != 0L) {
          jjmatchedKind = 28;
          jjmatchedPos = 19;
          return 22;
        }
        return -1;
      default :
        return -1;
    }
  }

  private static final int jjStartNfa_5(int pos, long active0, long active1) {
    return jjMoveNfa_5(jjStopStringLiteralDfa_5(pos, active0, active1), pos + 1);
  }

  static private final int jjStopAtPos(int pos, int kind) {
    jjmatchedKind = kind;
    jjmatchedPos = pos;
    return pos + 1;
  }

  static private final int jjStartNfaWithStates_5(int pos, int kind, int state) {
    jjmatchedKind = kind;
    jjmatchedPos = pos;
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      return pos + 1;
    }
    return jjMoveNfa_5(state, pos + 1);
  }

  static private final int jjMoveStringLiteralDfa0_5() {
    switch (curChar) {
      case 33:
        return jjStopAtPos(0, 10);
      case 40:
        return jjStopAtPos(0, 86);
      case 41:
        return jjStopAtPos(0, 87);
      case 44:
        return jjStopAtPos(0, 3);
      case 46:
        return jjStartNfaWithStates_5(0, 4, 1);
      case 97:
        return jjMoveStringLiteralDfa1_5(0x200000L);
      case 99:
        return jjMoveStringLiteralDfa1_5(0x181000L);
      case 101:
        return jjMoveStringLiteralDfa1_5(0x800L);
      case 103:
        return jjMoveStringLiteralDfa1_5(0x4000L);
      case 104:
        return jjMoveStringLiteralDfa1_5(0x6008000L);
      case 105:
        return jjMoveStringLiteralDfa1_5(0x1000000L);
      case 115:
        return jjMoveStringLiteralDfa1_5(0x42000L);
      case 116:
        return jjMoveStringLiteralDfa1_5(0xc00000L);
      case 119:
        return jjMoveStringLiteralDfa1_5(0x30000L);
      default :
        return jjMoveNfa_5(0, 0);
    }
  }

  static private final int jjMoveStringLiteralDfa1_5(long active0) {
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(0, active0, 0L);
      return 1;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa2_5(active0, 0x6409000L);
      case 101:
        return jjMoveStringLiteralDfa2_5(active0, 0x6000L);
      case 102:
        return jjMoveStringLiteralDfa2_5(active0, 0x1180000L);
      case 104:
        return jjMoveStringLiteralDfa2_5(active0, 0x800000L);
      case 105:
        return jjMoveStringLiteralDfa2_5(active0, 0x30000L);
      case 114:
        return jjMoveStringLiteralDfa2_5(active0, 0x200000L);
      case 116:
        return jjMoveStringLiteralDfa2_5(active0, 0x40000L);
      case 120:
        return jjMoveStringLiteralDfa2_5(active0, 0x800L);
      default :
        break;
    }
    return jjStartNfa_5(0, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa2_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(0, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(1, active0, 0L);
      return 2;
    }
    switch (curChar) {
      case 40:
        return jjMoveStringLiteralDfa3_5(active0, 0x1000000L);
      case 97:
        return jjMoveStringLiteralDfa3_5(active0, 0x40000L);
      case 101:
        return jjMoveStringLiteralDfa3_5(active0, 0x800L);
      case 103:
        return jjMoveStringLiteralDfa3_5(active0, 0x200000L);
      case 105:
        return jjMoveStringLiteralDfa3_5(active0, 0x800000L);
      case 108:
        return jjMoveStringLiteralDfa3_5(active0, 0x181000L);
      case 110:
        return jjMoveStringLiteralDfa3_5(active0, 0x8000L);
      case 114:
        return jjMoveStringLiteralDfa3_5(active0, 0x400000L);
      case 115:
        return jjMoveStringLiteralDfa3_5(active0, 0x6000000L);
      case 116:
        return jjMoveStringLiteralDfa3_5(active0, 0x36000L);
      default :
        break;
    }
    return jjStartNfa_5(1, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa3_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(1, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(2, active0, 0L);
      return 3;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x2000L) != 0L)
          return jjStopAtPos(3, 13);
        else if ((active0 & 0x4000L) != 0L)
          return jjStopAtPos(3, 14);
        break;
      case 41:
        if ((active0 & 0x1000000L) != 0L)
          return jjStopAtPos(3, 24);
        break;
      case 99:
        return jjMoveStringLiteralDfa4_5(active0, 0x800L);
      case 100:
        return jjMoveStringLiteralDfa4_5(active0, 0x8000L);
      case 102:
        return jjMoveStringLiteralDfa4_5(active0, 0x4000000L);
      case 103:
        return jjMoveStringLiteralDfa4_5(active0, 0x400000L);
      case 104:
        return jjMoveStringLiteralDfa4_5(active0, 0x30000L);
      case 108:
        return jjMoveStringLiteralDfa4_5(active0, 0x1000L);
      case 109:
        return jjMoveStringLiteralDfa4_5(active0, 0x2000000L);
      case 111:
        return jjMoveStringLiteralDfa4_5(active0, 0x180000L);
      case 115:
        return jjMoveStringLiteralDfa4_5(active0, 0xa00000L);
      case 116:
        return jjMoveStringLiteralDfa4_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(2, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa4_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(2, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(3, active0, 0L);
      return 4;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x1000L) != 0L)
          return jjStopAtPos(4, 12);
        else if ((active0 & 0x200000L) != 0L)
          return jjStopAtPos(4, 21);
        else if ((active0 & 0x800000L) != 0L)
          return jjStopAtPos(4, 23);
        break;
      case 101:
        return jjMoveStringLiteralDfa5_5(active0, 0x2400000L);
      case 105:
        return jjMoveStringLiteralDfa5_5(active0, 0x4070000L);
      case 108:
        return jjMoveStringLiteralDfa5_5(active0, 0x8000L);
      case 117:
        return jjMoveStringLiteralDfa5_5(active0, 0x800L);
      case 119:
        return jjMoveStringLiteralDfa5_5(active0, 0x180000L);
      default :
        break;
    }
    return jjStartNfa_5(3, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa5_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(3, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(4, active0, 0L);
      return 5;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x80000L) != 0L)
          return jjStopAtPos(5, 19);
        break;
      case 98:
        return jjMoveStringLiteralDfa6_5(active0, 0x100000L);
      case 99:
        return jjMoveStringLiteralDfa6_5(active0, 0x40000L);
      case 101:
        return jjMoveStringLiteralDfa6_5(active0, 0x4008000L);
      case 110:
        return jjMoveStringLiteralDfa6_5(active0, 0x30000L);
      case 116:
        return jjMoveStringLiteralDfa6_5(active0, 0x2400800L);
      default :
        break;
    }
    return jjStartNfa_5(4, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa6_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(4, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(5, active0, 0L);
      return 6;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x10000L) != 0L)
          return jjStopAtPos(6, 16);
        else if ((active0 & 0x400000L) != 0L)
          return jjStopAtPos(6, 22);
        break;
      case 99:
        return jjMoveStringLiteralDfa7_5(active0, 0x20000L);
      case 101:
        return jjMoveStringLiteralDfa7_5(active0, 0x100000L);
      case 104:
        return jjMoveStringLiteralDfa7_5(active0, 0x2000000L);
      case 105:
        return jjMoveStringLiteralDfa7_5(active0, 0x40800L);
      case 108:
        return jjMoveStringLiteralDfa7_5(active0, 0x4000000L);
      case 114:
        return jjMoveStringLiteralDfa7_5(active0, 0x8000L);
      default :
        break;
    }
    return jjStartNfa_5(5, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa7_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(5, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(6, active0, 0L);
      return 7;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x8000L) != 0L)
          return jjStopAtPos(7, 15);
        break;
      case 100:
        return jjMoveStringLiteralDfa8_5(active0, 0x4000000L);
      case 108:
        return jjMoveStringLiteralDfa8_5(active0, 0x100000L);
      case 110:
        return jjMoveStringLiteralDfa8_5(active0, 0x40000L);
      case 111:
        return jjMoveStringLiteralDfa8_5(active0, 0x2020800L);
      default :
        break;
    }
    return jjStartNfa_5(6, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa8_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(6, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(7, active0, 0L);
      return 8;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x4000000L) != 0L)
          return jjStopAtPos(8, 26);
        break;
      case 100:
        return jjMoveStringLiteralDfa9_5(active0, 0x2020000L);
      case 105:
        return jjMoveStringLiteralDfa9_5(active0, 0x40000L);
      case 110:
        return jjMoveStringLiteralDfa9_5(active0, 0x800L);
      case 111:
        return jjMoveStringLiteralDfa9_5(active0, 0x100000L);
      default :
        break;
    }
    return jjStartNfa_5(7, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa9_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(7, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(8, active0, 0L);
      return 9;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x800L) != 0L)
          return jjStopAtPos(9, 11);
        else if ((active0 & 0x2000000L) != 0L)
          return jjStopAtPos(9, 25);
        break;
      case 101:
        return jjMoveStringLiteralDfa10_5(active0, 0x20000L);
      case 116:
        return jjMoveStringLiteralDfa10_5(active0, 0x40000L);
      case 119:
        return jjMoveStringLiteralDfa10_5(active0, 0x100000L);
      default :
        break;
    }
    return jjStartNfa_5(8, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa10_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(8, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(9, active0, 0L);
      return 10;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x20000L) != 0L)
          return jjStopAtPos(10, 17);
        else if ((active0 & 0x100000L) != 0L)
          return jjStopAtPos(10, 20);
        break;
      case 105:
        return jjMoveStringLiteralDfa11_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(9, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa11_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(9, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(10, active0, 0L);
      return 11;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa12_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(10, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa12_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(10, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(11, active0, 0L);
      return 12;
    }
    switch (curChar) {
      case 108:
        return jjMoveStringLiteralDfa13_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(11, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa13_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(11, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(12, active0, 0L);
      return 13;
    }
    switch (curChar) {
      case 105:
        return jjMoveStringLiteralDfa14_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(12, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa14_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(12, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(13, active0, 0L);
      return 14;
    }
    switch (curChar) {
      case 122:
        return jjMoveStringLiteralDfa15_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(13, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa15_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(13, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(14, active0, 0L);
      return 15;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa16_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(14, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa16_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(14, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(15, active0, 0L);
      return 16;
    }
    switch (curChar) {
      case 116:
        return jjMoveStringLiteralDfa17_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(15, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa17_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(15, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(16, active0, 0L);
      return 17;
    }
    switch (curChar) {
      case 105:
        return jjMoveStringLiteralDfa18_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(16, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa18_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(16, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(17, active0, 0L);
      return 18;
    }
    switch (curChar) {
      case 111:
        return jjMoveStringLiteralDfa19_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(17, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa19_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(17, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(18, active0, 0L);
      return 19;
    }
    switch (curChar) {
      case 110:
        return jjMoveStringLiteralDfa20_5(active0, 0x40000L);
      default :
        break;
    }
    return jjStartNfa_5(18, active0, 0L);
  }

  static private final int jjMoveStringLiteralDfa20_5(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_5(18, old0, 0L);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_5(19, active0, 0L);
      return 20;
    }
    switch (curChar) {
      case 40:
        if ((active0 & 0x40000L) != 0L)
          return jjStopAtPos(20, 18);
        break;
      default :
        break;
    }
    return jjStartNfa_5(19, active0, 0L);
  }

  static private final void jjCheckNAdd(int state) {
    if (jjrounds[state] != jjround) {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
    }
  }

  static private final void jjAddStates(int start, int end) {
    do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
    } while (start++ != end);
  }

  static private final void jjCheckNAddTwoStates(int state1, int state2) {
    jjCheckNAdd(state1);
    jjCheckNAdd(state2);
  }

  static private final void jjCheckNAddStates(int start, int end) {
    do {
      jjCheckNAdd(jjnextStates[start]);
    } while (start++ != end);
  }

  static private final void jjCheckNAddStates(int start) {
    jjCheckNAdd(jjnextStates[start]);
    jjCheckNAdd(jjnextStates[start + 1]);
  }

  static private final int jjMoveNfa_5(int startState, int curPos) {
    int[] nextStates;
    int startsAt = 0;
    jjnewStateCnt = 22;
    int i = 1;
    jjstateSet[0] = startState;
    int j, kind = 0x7fffffff;
    for (; ;) {
      if (++jjround == 0x7fffffff)
        ReInitRounds();
      if (curChar < 64) {
        long l = 1L << curChar;
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 5:
              if ((0x3ff401000000000L & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAdd(21);
              } else if (curChar == 40) {
                if (kind > 27)
                  kind = 27;
              }
              if ((0x3ff401000000000L & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              break;
            case 22:
              if ((0x3ff401000000000L & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAdd(21);
              } else if (curChar == 40) {
                if (kind > 27)
                  kind = 27;
              }
              if ((0x3ff401000000000L & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              break;
            case 1:
              if ((0x3ff401000000000L & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAdd(21);
              } else if (curChar == 40) {
                if (kind > 27)
                  kind = 27;
              }
              if ((0x3ff401000000000L & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              if (curChar == 46) {
                if (kind > 7)
                  kind = 7;
              }
              break;
            case 0:
              if ((0x3ff401000000000L & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAddStates(0, 2);
              } else if (curChar == 38) {
                if (kind > 8)
                  kind = 8;
              }
              if (curChar == 38)
                jjstateSet[jjnewStateCnt++] = 2;
              else if (curChar == 46)
                jjstateSet[jjnewStateCnt++] = 1;
              break;
            case 2:
              if (curChar == 38 && kind > 8)
                kind = 8;
              break;
            case 3:
              if (curChar == 38)
                jjstateSet[jjnewStateCnt++] = 2;
              break;
            case 10:
              if (curChar == 38 && kind > 8)
                kind = 8;
              break;
            case 18:
              if ((0x3ff401000000000L & l) == 0L)
                break;
              if (kind > 28)
                kind = 28;
              jjCheckNAddStates(0, 2);
              break;
            case 19:
              if ((0x3ff401000000000L & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              break;
            case 20:
              if (curChar == 40)
                kind = 27;
              break;
            case 21:
              if ((0x3ff401000000000L & l) == 0L)
                break;
              if (kind > 28)
                kind = 28;
              jjCheckNAdd(21);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else if (curChar < 128) {
        long l = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 5:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAdd(21);
              }
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              if (curChar == 110)
                jjstateSet[jjnewStateCnt++] = 4;
              break;
            case 22:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAdd(21);
              }
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              break;
            case 1:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAdd(21);
              }
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              break;
            case 0:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 28)
                  kind = 28;
                jjCheckNAddStates(0, 2);
              } else if (curChar == 124) {
                if (kind > 9)
                  kind = 9;
              }
              if (curChar == 79)
                jjstateSet[jjnewStateCnt++] = 15;
              else if (curChar == 111)
                jjstateSet[jjnewStateCnt++] = 13;
              else if (curChar == 124)
                jjstateSet[jjnewStateCnt++] = 11;
              else if (curChar == 65)
                jjstateSet[jjnewStateCnt++] = 8;
              else if (curChar == 97)
                jjstateSet[jjnewStateCnt++] = 5;
              break;
            case 4:
              if (curChar == 100 && kind > 8)
                kind = 8;
              break;
            case 6:
              if (curChar == 97)
                jjstateSet[jjnewStateCnt++] = 5;
              break;
            case 7:
              if (curChar == 68 && kind > 8)
                kind = 8;
              break;
            case 8:
              if (curChar == 78)
                jjstateSet[jjnewStateCnt++] = 7;
              break;
            case 9:
              if (curChar == 65)
                jjstateSet[jjnewStateCnt++] = 8;
              break;
            case 11:
              if (curChar == 124 && kind > 9)
                kind = 9;
              break;
            case 12:
              if (curChar == 124)
                jjstateSet[jjnewStateCnt++] = 11;
              break;
            case 13:
              if (curChar == 114 && kind > 9)
                kind = 9;
              break;
            case 14:
              if (curChar == 111)
                jjstateSet[jjnewStateCnt++] = 13;
              break;
            case 15:
              if (curChar == 82 && kind > 9)
                kind = 9;
              break;
            case 16:
              if (curChar == 79)
                jjstateSet[jjnewStateCnt++] = 15;
              break;
            case 17:
              if (curChar == 124 && kind > 9)
                kind = 9;
              break;
            case 18:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 28)
                kind = 28;
              jjCheckNAddStates(0, 2);
              break;
            case 19:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddTwoStates(19, 20);
              break;
            case 21:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 28)
                kind = 28;
              jjCheckNAdd(21);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else {
        int i2 = (curChar & 0xff) >> 6;
        long l2 = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            default :
              break;
          }
        } while (i != startsAt);
      }
      if (kind != 0x7fffffff) {
        jjmatchedKind = kind;
        jjmatchedPos = curPos;
        kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 22 - (jjnewStateCnt = startsAt)))
        return curPos;
      try {
        curChar = input_stream.readChar();
      }
      catch (java.io.IOException e) {
        return curPos;
      }
    }
  }

  private static final int jjStopStringLiteralDfa_0(int pos, long active0, long active1) {
    switch (pos) {
      default :
        return -1;
    }
  }

  private static final int jjStartNfa_0(int pos, long active0, long active1) {
    return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0, active1), pos + 1);
  }

  static private final int jjStartNfaWithStates_0(int pos, int kind, int state) {
    jjmatchedKind = kind;
    jjmatchedPos = pos;
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      return pos + 1;
    }
    return jjMoveNfa_0(state, pos + 1);
  }

  static private final int jjMoveStringLiteralDfa0_0() {
    switch (curChar) {
      case 41:
        return jjStopAtPos(0, 85);
      case 44:
        return jjStopAtPos(0, 3);
      case 46:
        return jjStartNfaWithStates_0(0, 4, 1);
      default :
        return jjMoveNfa_0(0, 0);
    }
  }

  static private final int jjMoveNfa_0(int startState, int curPos) {
    int[] nextStates;
    int startsAt = 0;
    jjnewStateCnt = 11;
    int i = 1;
    jjstateSet[0] = startState;
    int j, kind = 0x7fffffff;
    for (; ;) {
      if (++jjround == 0x7fffffff)
        ReInitRounds();
      if (curChar < 64) {
        long l = 1L << curChar;
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 0:
              if ((0x3ff081800000000L & l) != 0L) {
                if (kind > 82)
                  kind = 82;
                jjCheckNAddStates(3, 7);
              } else if (curChar == 46)
                jjstateSet[jjnewStateCnt++] = 1;
              break;
            case 1:
              if (curChar == 46 && kind > 7)
                kind = 7;
              break;
            case 2:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 82)
                kind = 82;
              jjCheckNAddStates(3, 7);
              break;
            case 3:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 82)
                kind = 82;
              jjCheckNAddTwoStates(3, 4);
              break;
            case 4:
              if (curChar == 46)
                jjCheckNAdd(5);
              break;
            case 5:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 82)
                kind = 82;
              jjCheckNAddTwoStates(4, 5);
              break;
            case 6:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 83)
                kind = 83;
              jjCheckNAddStates(8, 10);
              break;
            case 7:
              if (curChar == 46)
                jjCheckNAdd(8);
              break;
            case 8:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 83)
                kind = 83;
              jjCheckNAddStates(11, 13);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else if (curChar < 128) {
        long l = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 0:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 82)
                kind = 82;
              jjCheckNAddStates(3, 7);
              break;
            case 3:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 82)
                kind = 82;
              jjCheckNAddTwoStates(3, 4);
              break;
            case 5:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 82)
                kind = 82;
              jjCheckNAddTwoStates(4, 5);
              break;
            case 6:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 83)
                kind = 83;
              jjCheckNAddStates(8, 10);
              break;
            case 8:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 83)
                kind = 83;
              jjCheckNAddStates(11, 13);
              break;
            case 9:
              if (curChar != 93)
                break;
              kind = 83;
              jjCheckNAdd(10);
              break;
            case 10:
              if (curChar == 91)
                jjstateSet[jjnewStateCnt++] = 9;
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else {
        int i2 = (curChar & 0xff) >> 6;
        long l2 = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            default :
              break;
          }
        } while (i != startsAt);
      }
      if (kind != 0x7fffffff) {
        jjmatchedKind = kind;
        jjmatchedPos = curPos;
        kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 11 - (jjnewStateCnt = startsAt)))
        return curPos;
      try {
        curChar = input_stream.readChar();
      }
      catch (java.io.IOException e) {
        return curPos;
      }
    }
  }

  private static final int jjStopStringLiteralDfa_4(int pos, long active0) {
    switch (pos) {
      case 0:
        if ((active0 & 0x10L) != 0L)
          return 12;
        if ((active0 & 0x7e0000000L) != 0L) {
          jjmatchedKind = 37;
          return 13;
        }
        return -1;
      case 1:
        if ((active0 & 0x7e0000000L) != 0L) {
          jjmatchedKind = 37;
          jjmatchedPos = 1;
          return 13;
        }
        return -1;
      case 2:
        if ((active0 & 0x7e0000000L) != 0L) {
          jjmatchedKind = 37;
          jjmatchedPos = 2;
          return 13;
        }
        return -1;
      case 3:
        if ((active0 & 0x7e0000000L) != 0L) {
          jjmatchedKind = 37;
          jjmatchedPos = 3;
          return 13;
        }
        return -1;
      case 4:
        if ((active0 & 0x3e0000000L) != 0L) {
          jjmatchedKind = 37;
          jjmatchedPos = 4;
          return 13;
        }
        if ((active0 & 0x400000000L) != 0L)
          return 13;
        return -1;
      case 5:
        if ((active0 & 0x180000000L) != 0L)
          return 13;
        if ((active0 & 0x260000000L) != 0L) {
          jjmatchedKind = 37;
          jjmatchedPos = 5;
          return 13;
        }
        return -1;
      case 6:
        if ((active0 & 0x240000000L) != 0L) {
          jjmatchedKind = 37;
          jjmatchedPos = 6;
          return 13;
        }
        if ((active0 & 0x20000000L) != 0L)
          return 13;
        return -1;
      case 7:
        if ((active0 & 0x200000000L) != 0L)
          return 13;
        if ((active0 & 0x40000000L) != 0L) {
          jjmatchedKind = 37;
          jjmatchedPos = 7;
          return 13;
        }
        return -1;
      default :
        return -1;
    }
  }

  private static final int jjStartNfa_4(int pos, long active0) {
    return jjMoveNfa_4(jjStopStringLiteralDfa_4(pos, active0), pos + 1);
  }

  static private final int jjStartNfaWithStates_4(int pos, int kind, int state) {
    jjmatchedKind = kind;
    jjmatchedPos = pos;
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      return pos + 1;
    }
    return jjMoveNfa_4(state, pos + 1);
  }

  static private final int jjMoveStringLiteralDfa0_4() {
    switch (curChar) {
      case 33:
        return jjStopAtPos(0, 35);
      case 41:
        return jjStopAtPos(0, 41);
      case 44:
        return jjStopAtPos(0, 3);
      case 46:
        return jjStartNfaWithStates_4(0, 4, 12);
      case 97:
        return jjMoveStringLiteralDfa1_4(0x200000000L);
      case 102:
        return jjMoveStringLiteralDfa1_4(0x400000000L);
      case 112:
        return jjMoveStringLiteralDfa1_4(0xe0000000L);
      case 115:
        return jjMoveStringLiteralDfa1_4(0x100000000L);
      default :
        return jjMoveNfa_4(0, 0);
    }
  }

  static private final int jjMoveStringLiteralDfa1_4(long active0) {
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(0, active0);
      return 1;
    }
    switch (curChar) {
      case 98:
        return jjMoveStringLiteralDfa2_4(active0, 0x200000000L);
      case 105:
        return jjMoveStringLiteralDfa2_4(active0, 0x400000000L);
      case 114:
        return jjMoveStringLiteralDfa2_4(active0, 0x60000000L);
      case 116:
        return jjMoveStringLiteralDfa2_4(active0, 0x100000000L);
      case 117:
        return jjMoveStringLiteralDfa2_4(active0, 0x80000000L);
      default :
        break;
    }
    return jjStartNfa_4(0, active0);
  }

  static private final int jjMoveStringLiteralDfa2_4(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_4(0, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(1, active0);
      return 2;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa3_4(active0, 0x100000000L);
      case 98:
        return jjMoveStringLiteralDfa3_4(active0, 0x80000000L);
      case 105:
        return jjMoveStringLiteralDfa3_4(active0, 0x20000000L);
      case 110:
        return jjMoveStringLiteralDfa3_4(active0, 0x400000000L);
      case 111:
        return jjMoveStringLiteralDfa3_4(active0, 0x40000000L);
      case 115:
        return jjMoveStringLiteralDfa3_4(active0, 0x200000000L);
      default :
        break;
    }
    return jjStartNfa_4(1, active0);
  }

  static private final int jjMoveStringLiteralDfa3_4(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_4(1, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(2, active0);
      return 3;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa4_4(active0, 0x400000000L);
      case 108:
        return jjMoveStringLiteralDfa4_4(active0, 0x80000000L);
      case 116:
        return jjMoveStringLiteralDfa4_4(active0, 0x340000000L);
      case 118:
        return jjMoveStringLiteralDfa4_4(active0, 0x20000000L);
      default :
        break;
    }
    return jjStartNfa_4(2, active0);
  }

  static private final int jjMoveStringLiteralDfa4_4(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_4(2, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(3, active0);
      return 4;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa5_4(active0, 0x20000000L);
      case 101:
        return jjMoveStringLiteralDfa5_4(active0, 0x40000000L);
      case 105:
        return jjMoveStringLiteralDfa5_4(active0, 0x180000000L);
      case 108:
        if ((active0 & 0x400000000L) != 0L)
          return jjStartNfaWithStates_4(4, 34, 13);
        break;
      case 114:
        return jjMoveStringLiteralDfa5_4(active0, 0x200000000L);
      default :
        break;
    }
    return jjStartNfa_4(3, active0);
  }

  static private final int jjMoveStringLiteralDfa5_4(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_4(3, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(4, active0);
      return 5;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa6_4(active0, 0x200000000L);
      case 99:
        if ((active0 & 0x80000000L) != 0L)
          return jjStartNfaWithStates_4(5, 31, 13);
        else if ((active0 & 0x100000000L) != 0L)
          return jjStartNfaWithStates_4(5, 32, 13);
        return jjMoveStringLiteralDfa6_4(active0, 0x40000000L);
      case 116:
        return jjMoveStringLiteralDfa6_4(active0, 0x20000000L);
      default :
        break;
    }
    return jjStartNfa_4(4, active0);
  }

  static private final int jjMoveStringLiteralDfa6_4(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_4(4, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(5, active0);
      return 6;
    }
    switch (curChar) {
      case 99:
        return jjMoveStringLiteralDfa7_4(active0, 0x200000000L);
      case 101:
        if ((active0 & 0x20000000L) != 0L)
          return jjStartNfaWithStates_4(6, 29, 13);
        break;
      case 116:
        return jjMoveStringLiteralDfa7_4(active0, 0x40000000L);
      default :
        break;
    }
    return jjStartNfa_4(5, active0);
  }

  static private final int jjMoveStringLiteralDfa7_4(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_4(5, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(6, active0);
      return 7;
    }
    switch (curChar) {
      case 101:
        return jjMoveStringLiteralDfa8_4(active0, 0x40000000L);
      case 116:
        if ((active0 & 0x200000000L) != 0L)
          return jjStartNfaWithStates_4(7, 33, 13);
        break;
      default :
        break;
    }
    return jjStartNfa_4(6, active0);
  }

  static private final int jjMoveStringLiteralDfa8_4(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_4(6, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_4(7, active0);
      return 8;
    }
    switch (curChar) {
      case 100:
        if ((active0 & 0x40000000L) != 0L)
          return jjStartNfaWithStates_4(8, 30, 13);
        break;
      default :
        break;
    }
    return jjStartNfa_4(7, active0);
  }

  static private final int jjMoveNfa_4(int startState, int curPos) {
    int[] nextStates;
    int startsAt = 0;
    jjnewStateCnt = 13;
    int i = 1;
    jjstateSet[0] = startState;
    int j, kind = 0x7fffffff;
    for (; ;) {
      if (++jjround == 0x7fffffff)
        ReInitRounds();
      if (curChar < 64) {
        long l = 1L << curChar;
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 13:
              if ((0x3ff0c1800000000L & l) != 0L) {
                if (kind > 37)
                  kind = 37;
                jjCheckNAddTwoStates(4, 5);
              } else if (curChar == 46)
                jjCheckNAddStates(14, 16);
              break;
            case 12:
              if (curChar == 46) {
                if (kind > 37)
                  kind = 37;
                jjCheckNAddTwoStates(4, 5);
              }
              if (curChar == 46) {
                if (kind > 7)
                  kind = 7;
              }
              break;
            case 0:
              if ((0x3ff0c1800000000L & l) != 0L) {
                if (kind > 37)
                  kind = 37;
                jjCheckNAddTwoStates(4, 5);
              } else if (curChar == 46)
                jjCheckNAddTwoStates(12, 6);
              break;
            case 1:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 36)
                kind = 36;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 2:
              if (curChar == 46)
                jjCheckNAdd(3);
              break;
            case 3:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 36)
                kind = 36;
              jjCheckNAddTwoStates(2, 3);
              break;
            case 4:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 37)
                kind = 37;
              jjCheckNAddTwoStates(4, 5);
              break;
            case 5:
              if (curChar == 46)
                jjCheckNAddStates(14, 16);
              break;
            case 6:
              if (curChar != 46)
                break;
              if (kind > 37)
                kind = 37;
              jjCheckNAddTwoStates(4, 5);
              break;
            case 7:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 37)
                kind = 37;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 8:
              if (curChar == 46)
                jjCheckNAddStates(17, 19);
              break;
            case 9:
              if (curChar == 46)
                jjCheckNAdd(10);
              break;
            case 10:
              if (curChar != 46)
                break;
              if (kind > 37)
                kind = 37;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 11:
              if (curChar == 46)
                jjCheckNAddTwoStates(12, 6);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else if (curChar < 128) {
        long l = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 13:
            case 4:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 37)
                kind = 37;
              jjCheckNAddTwoStates(4, 5);
              break;
            case 0:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 37)
                  kind = 37;
                jjCheckNAddTwoStates(4, 5);
              } else if (curChar == 64)
                jjCheckNAdd(1);
              break;
            case 1:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 36)
                kind = 36;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 3:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 36)
                kind = 36;
              jjCheckNAddTwoStates(2, 3);
              break;
            case 7:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 37)
                kind = 37;
              jjAddStates(20, 21);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else {
        int i2 = (curChar & 0xff) >> 6;
        long l2 = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            default :
              break;
          }
        } while (i != startsAt);
      }
      if (kind != 0x7fffffff) {
        jjmatchedKind = kind;
        jjmatchedPos = curPos;
        kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 13 - (jjnewStateCnt = startsAt)))
        return curPos;
      try {
        curChar = input_stream.readChar();
      }
      catch (java.io.IOException e) {
        return curPos;
      }
    }
  }

  private static final int jjStopStringLiteralDfa_2(int pos, long active0, long active1) {
    switch (pos) {
      case 0:
        if ((active0 & 0xf000000000000000L) != 0L || (active1 & 0x7L) != 0L) {
          jjmatchedKind = 70;
          return 23;
        }
        if ((active0 & 0x10L) != 0L)
          return 5;
        return -1;
      case 1:
        if ((active0 & 0xf000000000000000L) != 0L || (active1 & 0x7L) != 0L) {
          jjmatchedKind = 70;
          jjmatchedPos = 1;
          return 23;
        }
        return -1;
      case 2:
        if ((active0 & 0xf000000000000000L) != 0L || (active1 & 0x7L) != 0L) {
          jjmatchedKind = 70;
          jjmatchedPos = 2;
          return 23;
        }
        return -1;
      case 3:
        if ((active0 & 0xf000000000000000L) != 0L || (active1 & 0x7L) != 0L) {
          jjmatchedKind = 70;
          jjmatchedPos = 3;
          return 23;
        }
        return -1;
      case 4:
        if ((active0 & 0xf000000000000000L) != 0L || (active1 & 0x5L) != 0L) {
          jjmatchedKind = 70;
          jjmatchedPos = 4;
          return 23;
        }
        if ((active1 & 0x2L) != 0L)
          return 23;
        return -1;
      case 5:
        if ((active0 & 0x3000000000000000L) != 0L || (active1 & 0x5L) != 0L) {
          jjmatchedKind = 70;
          jjmatchedPos = 5;
          return 23;
        }
        if ((active0 & 0xc000000000000000L) != 0L)
          return 23;
        return -1;
      case 6:
        if ((active0 & 0x2000000000000000L) != 0L || (active1 & 0x5L) != 0L) {
          jjmatchedKind = 70;
          jjmatchedPos = 6;
          return 23;
        }
        if ((active0 & 0x1000000000000000L) != 0L)
          return 23;
        return -1;
      case 7:
        if ((active0 & 0x2000000000000000L) != 0L || (active1 & 0x4L) != 0L) {
          jjmatchedKind = 70;
          jjmatchedPos = 7;
          return 23;
        }
        if ((active1 & 0x1L) != 0L)
          return 23;
        return -1;
      default :
        return -1;
    }
  }

  private static final int jjStartNfa_2(int pos, long active0, long active1) {
    return jjMoveNfa_2(jjStopStringLiteralDfa_2(pos, active0, active1), pos + 1);
  }

  static private final int jjStartNfaWithStates_2(int pos, int kind, int state) {
    jjmatchedKind = kind;
    jjmatchedPos = pos;
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      return pos + 1;
    }
    return jjMoveNfa_2(state, pos + 1);
  }

  static private final int jjMoveStringLiteralDfa0_2() {
    switch (curChar) {
      case 33:
        return jjStopAtPos(0, 67);
      case 41:
        return jjStopAtPos(0, 74);
      case 44:
        return jjStopAtPos(0, 3);
      case 46:
        return jjStartNfaWithStates_2(0, 4, 5);
      case 97:
        return jjMoveStringLiteralDfa1_2(0x0L, 0x1L);
      case 102:
        return jjMoveStringLiteralDfa1_2(0x0L, 0x2L);
      case 112:
        return jjMoveStringLiteralDfa1_2(0x7000000000000000L, 0x0L);
      case 115:
        return jjMoveStringLiteralDfa1_2(0x8000000000000000L, 0x0L);
      case 116:
        return jjMoveStringLiteralDfa1_2(0x0L, 0x4L);
      default :
        return jjMoveNfa_2(0, 0);
    }
  }

  static private final int jjMoveStringLiteralDfa1_2(long active0, long active1) {
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(0, active0, active1);
      return 1;
    }
    switch (curChar) {
      case 98:
        return jjMoveStringLiteralDfa2_2(active0, 0L, active1, 0x1L);
      case 105:
        return jjMoveStringLiteralDfa2_2(active0, 0L, active1, 0x2L);
      case 114:
        return jjMoveStringLiteralDfa2_2(active0, 0x3000000000000000L, active1, 0x4L);
      case 116:
        return jjMoveStringLiteralDfa2_2(active0, 0x8000000000000000L, active1, 0L);
      case 117:
        return jjMoveStringLiteralDfa2_2(active0, 0x4000000000000000L, active1, 0L);
      default :
        break;
    }
    return jjStartNfa_2(0, active0, active1);
  }

  static private final int jjMoveStringLiteralDfa2_2(long old0, long active0, long old1, long active1) {
    if (((active0 &= old0) | (active1 &= old1)) == 0L)
      return jjStartNfa_2(0, old0, old1);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(1, active0, active1);
      return 2;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa3_2(active0, 0x8000000000000000L, active1, 0x4L);
      case 98:
        return jjMoveStringLiteralDfa3_2(active0, 0x4000000000000000L, active1, 0L);
      case 105:
        return jjMoveStringLiteralDfa3_2(active0, 0x1000000000000000L, active1, 0L);
      case 110:
        return jjMoveStringLiteralDfa3_2(active0, 0L, active1, 0x2L);
      case 111:
        return jjMoveStringLiteralDfa3_2(active0, 0x2000000000000000L, active1, 0L);
      case 115:
        return jjMoveStringLiteralDfa3_2(active0, 0L, active1, 0x1L);
      default :
        break;
    }
    return jjStartNfa_2(1, active0, active1);
  }

  static private final int jjMoveStringLiteralDfa3_2(long old0, long active0, long old1, long active1) {
    if (((active0 &= old0) | (active1 &= old1)) == 0L)
      return jjStartNfa_2(1, old0, old1);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(2, active0, active1);
      return 3;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa4_2(active0, 0L, active1, 0x2L);
      case 108:
        return jjMoveStringLiteralDfa4_2(active0, 0x4000000000000000L, active1, 0L);
      case 110:
        return jjMoveStringLiteralDfa4_2(active0, 0L, active1, 0x4L);
      case 116:
        return jjMoveStringLiteralDfa4_2(active0, 0xa000000000000000L, active1, 0x1L);
      case 118:
        return jjMoveStringLiteralDfa4_2(active0, 0x1000000000000000L, active1, 0L);
      default :
        break;
    }
    return jjStartNfa_2(2, active0, active1);
  }

  static private final int jjMoveStringLiteralDfa4_2(long old0, long active0, long old1, long active1) {
    if (((active0 &= old0) | (active1 &= old1)) == 0L)
      return jjStartNfa_2(2, old0, old1);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(3, active0, active1);
      return 4;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa5_2(active0, 0x1000000000000000L, active1, 0L);
      case 101:
        return jjMoveStringLiteralDfa5_2(active0, 0x2000000000000000L, active1, 0L);
      case 105:
        return jjMoveStringLiteralDfa5_2(active0, 0xc000000000000000L, active1, 0L);
      case 108:
        if ((active1 & 0x2L) != 0L)
          return jjStartNfaWithStates_2(4, 65, 23);
        break;
      case 114:
        return jjMoveStringLiteralDfa5_2(active0, 0L, active1, 0x1L);
      case 115:
        return jjMoveStringLiteralDfa5_2(active0, 0L, active1, 0x4L);
      default :
        break;
    }
    return jjStartNfa_2(3, active0, active1);
  }

  static private final int jjMoveStringLiteralDfa5_2(long old0, long active0, long old1, long active1) {
    if (((active0 &= old0) | (active1 &= old1)) == 0L)
      return jjStartNfa_2(3, old0, old1);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(4, active0, active1);
      return 5;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa6_2(active0, 0L, active1, 0x1L);
      case 99:
        if ((active0 & 0x4000000000000000L) != 0L)
          return jjStartNfaWithStates_2(5, 62, 23);
        else if ((active0 & 0x8000000000000000L) != 0L)
          return jjStartNfaWithStates_2(5, 63, 23);
        return jjMoveStringLiteralDfa6_2(active0, 0x2000000000000000L, active1, 0L);
      case 105:
        return jjMoveStringLiteralDfa6_2(active0, 0L, active1, 0x4L);
      case 116:
        return jjMoveStringLiteralDfa6_2(active0, 0x1000000000000000L, active1, 0L);
      default :
        break;
    }
    return jjStartNfa_2(4, active0, active1);
  }

  static private final int jjMoveStringLiteralDfa6_2(long old0, long active0, long old1, long active1) {
    if (((active0 &= old0) | (active1 &= old1)) == 0L)
      return jjStartNfa_2(4, old0, old1);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(5, active0, active1);
      return 6;
    }
    switch (curChar) {
      case 99:
        return jjMoveStringLiteralDfa7_2(active0, 0L, active1, 0x1L);
      case 101:
        if ((active0 & 0x1000000000000000L) != 0L)
          return jjStartNfaWithStates_2(6, 60, 23);
        return jjMoveStringLiteralDfa7_2(active0, 0L, active1, 0x4L);
      case 116:
        return jjMoveStringLiteralDfa7_2(active0, 0x2000000000000000L, active1, 0L);
      default :
        break;
    }
    return jjStartNfa_2(5, active0, active1);
  }

  static private final int jjMoveStringLiteralDfa7_2(long old0, long active0, long old1, long active1) {
    if (((active0 &= old0) | (active1 &= old1)) == 0L)
      return jjStartNfa_2(5, old0, old1);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(6, active0, active1);
      return 7;
    }
    switch (curChar) {
      case 101:
        return jjMoveStringLiteralDfa8_2(active0, 0x2000000000000000L, active1, 0L);
      case 110:
        return jjMoveStringLiteralDfa8_2(active0, 0L, active1, 0x4L);
      case 116:
        if ((active1 & 0x1L) != 0L)
          return jjStartNfaWithStates_2(7, 64, 23);
        break;
      default :
        break;
    }
    return jjStartNfa_2(6, active0, active1);
  }

  static private final int jjMoveStringLiteralDfa8_2(long old0, long active0, long old1, long active1) {
    if (((active0 &= old0) | (active1 &= old1)) == 0L)
      return jjStartNfa_2(6, old0, old1);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_2(7, active0, active1);
      return 8;
    }
    switch (curChar) {
      case 100:
        if ((active0 & 0x2000000000000000L) != 0L)
          return jjStartNfaWithStates_2(8, 61, 23);
        break;
      case 116:
        if ((active1 & 0x4L) != 0L)
          return jjStartNfaWithStates_2(8, 66, 23);
        break;
      default :
        break;
    }
    return jjStartNfa_2(7, active0, active1);
  }

  static private final int jjMoveNfa_2(int startState, int curPos) {
    int[] nextStates;
    int startsAt = 0;
    jjnewStateCnt = 23;
    int i = 1;
    jjstateSet[0] = startState;
    int j, kind = 0x7fffffff;
    for (; ;) {
      if (++jjround == 0x7fffffff)
        ReInitRounds();
      if (curChar < 64) {
        long l = 1L << curChar;
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 5:
              if (curChar == 46)
                jjCheckNAddStates(22, 24);
              if (curChar == 46) {
                if (kind > 70)
                  kind = 70;
                jjCheckNAddTwoStates(7, 8);
              }
              if (curChar == 46) {
                if (kind > 7)
                  kind = 7;
              }
              break;
            case 23:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(22, 24);
              else if (curChar == 46)
                jjCheckNAddStates(25, 27);
              if ((0x3ff0c1800000000L & l) != 0L) {
                if (kind > 70)
                  kind = 70;
                jjCheckNAddTwoStates(7, 8);
              } else if (curChar == 46)
                jjCheckNAddStates(28, 30);
              break;
            case 0:
              if ((0x3ff0c1800000000L & l) != 0L) {
                if (kind > 70)
                  kind = 70;
                jjCheckNAddStates(31, 35);
              } else if (curChar == 46)
                jjCheckNAddStates(36, 38);
              break;
            case 1:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 68)
                kind = 68;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 2:
              if (curChar == 46)
                jjCheckNAdd(3);
              break;
            case 3:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 68)
                kind = 68;
              jjCheckNAddTwoStates(2, 3);
              break;
            case 4:
              if (curChar == 46)
                jjCheckNAddStates(36, 38);
              break;
            case 6:
              if (curChar != 46)
                break;
              if (kind > 70)
                kind = 70;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 7:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 70)
                kind = 70;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 8:
              if (curChar == 46)
                jjCheckNAddStates(25, 27);
              break;
            case 9:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 70)
                kind = 70;
              jjCheckNAddTwoStates(9, 10);
              break;
            case 10:
              if (curChar == 46)
                jjCheckNAddStates(39, 41);
              break;
            case 11:
              if (curChar == 46)
                jjCheckNAdd(12);
              break;
            case 12:
              if (curChar != 46)
                break;
              if (kind > 70)
                kind = 70;
              jjCheckNAddTwoStates(9, 10);
              break;
            case 13:
              if (curChar == 46)
                jjCheckNAddStates(22, 24);
              break;
            case 14:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(22, 24);
              break;
            case 17:
              if (curChar == 46)
                jjCheckNAddStates(28, 30);
              break;
            case 18:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(42, 44);
              break;
            case 19:
              if (curChar == 46)
                jjCheckNAddStates(45, 47);
              break;
            case 20:
              if (curChar == 46)
                jjCheckNAdd(21);
              break;
            case 21:
              if (curChar == 46)
                jjCheckNAddStates(42, 44);
              break;
            case 22:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 70)
                kind = 70;
              jjCheckNAddStates(31, 35);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else if (curChar < 128) {
        long l = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 23:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(22, 24);
              else if (curChar == 91)
                jjstateSet[jjnewStateCnt++] = 15;
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 70)
                  kind = 70;
                jjCheckNAddTwoStates(7, 8);
              }
              break;
            case 0:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 70)
                  kind = 70;
                jjCheckNAddStates(31, 35);
              } else if (curChar == 64)
                jjCheckNAdd(1);
              break;
            case 1:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 68)
                kind = 68;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 3:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 68)
                kind = 68;
              jjCheckNAddTwoStates(2, 3);
              break;
            case 7:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 70)
                kind = 70;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 9:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 70)
                kind = 70;
              jjAddStates(48, 49);
              break;
            case 14:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(22, 24);
              break;
            case 15:
              if (curChar != 93)
                break;
              kind = 71;
              jjCheckNAdd(16);
              break;
            case 16:
              if (curChar == 91)
                jjstateSet[jjnewStateCnt++] = 15;
              break;
            case 18:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(42, 44);
              break;
            case 22:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 70)
                kind = 70;
              jjCheckNAddStates(31, 35);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else {
        int i2 = (curChar & 0xff) >> 6;
        long l2 = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            default :
              break;
          }
        } while (i != startsAt);
      }
      if (kind != 0x7fffffff) {
        jjmatchedKind = kind;
        jjmatchedPos = curPos;
        kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 23 - (jjnewStateCnt = startsAt)))
        return curPos;
      try {
        curChar = input_stream.readChar();
      }
      catch (java.io.IOException e) {
        return curPos;
      }
    }
  }

  private static final int jjStopStringLiteralDfa_3(int pos, long active0) {
    switch (pos) {
      case 0:
        if ((active0 & 0x10L) != 0L)
          return 5;
        if ((active0 & 0x7fc0000000000L) != 0L) {
          jjmatchedKind = 54;
          return 23;
        }
        return -1;
      case 1:
        if ((active0 & 0x7fc0000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 1;
          return 23;
        }
        return -1;
      case 2:
        if ((active0 & 0x7fc0000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 2;
          return 23;
        }
        return -1;
      case 3:
        if ((active0 & 0x7fc0000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 3;
          return 23;
        }
        return -1;
      case 4:
        if ((active0 & 0x77c0000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 4;
          return 23;
        }
        if ((active0 & 0x800000000000L) != 0L)
          return 23;
        return -1;
      case 5:
        if ((active0 & 0x2580000000000L) != 0L) {
          if (jjmatchedPos != 5) {
            jjmatchedKind = 54;
            jjmatchedPos = 5;
          }
          return 23;
        }
        if ((active0 & 0x5240000000000L) != 0L)
          return 23;
        return -1;
      case 6:
        if ((active0 & 0x6480000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 6;
          return 23;
        }
        if ((active0 & 0x100000000000L) != 0L)
          return 23;
        return -1;
      case 7:
        if ((active0 & 0x6080000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 7;
          return 23;
        }
        if ((active0 & 0x400000000000L) != 0L)
          return 23;
        return -1;
      case 8:
        if ((active0 & 0x6000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 8;
          return 23;
        }
        if ((active0 & 0x80000000000L) != 0L)
          return 23;
        return -1;
      case 9:
        if ((active0 & 0x6000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 9;
          return 23;
        }
        return -1;
      case 10:
        if ((active0 & 0x6000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 10;
          return 23;
        }
        return -1;
      case 11:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 11;
          return 23;
        }
        if ((active0 & 0x2000000000000L) != 0L)
          return 23;
        return -1;
      case 12:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 12;
          return 23;
        }
        return -1;
      case 13:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 13;
          return 23;
        }
        return -1;
      case 14:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 14;
          return 23;
        }
        return -1;
      case 15:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 15;
          return 23;
        }
        return -1;
      case 16:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 16;
          return 23;
        }
        return -1;
      case 17:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 17;
          return 23;
        }
        return -1;
      case 18:
        if ((active0 & 0x4000000000000L) != 0L) {
          jjmatchedKind = 54;
          jjmatchedPos = 18;
          return 23;
        }
        return -1;
      default :
        return -1;
    }
  }

  private static final int jjStartNfa_3(int pos, long active0) {
    return jjMoveNfa_3(jjStopStringLiteralDfa_3(pos, active0), pos + 1);
  }

  static private final int jjStartNfaWithStates_3(int pos, int kind, int state) {
    jjmatchedKind = kind;
    jjmatchedPos = pos;
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      return pos + 1;
    }
    return jjMoveNfa_3(state, pos + 1);
  }

  static private final int jjMoveStringLiteralDfa0_3() {
    switch (curChar) {
      case 33:
        return jjStopAtPos(0, 51);
      case 40:
        return jjStopAtPos(0, 57);
      case 41:
        return jjStopAtPos(0, 58);
      case 44:
        return jjStopAtPos(0, 3);
      case 46:
        return jjStartNfaWithStates_3(0, 4, 5);
      case 97:
        return jjMoveStringLiteralDfa1_3(0x400000000000L);
      case 102:
        return jjMoveStringLiteralDfa1_3(0x800000000000L);
      case 110:
        return jjMoveStringLiteralDfa1_3(0x1000000000000L);
      case 112:
        return jjMoveStringLiteralDfa1_3(0x1c0000000000L);
      case 115:
        return jjMoveStringLiteralDfa1_3(0x6200000000000L);
      default :
        return jjMoveNfa_3(0, 0);
    }
  }

  static private final int jjMoveStringLiteralDfa1_3(long active0) {
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(0, active0);
      return 1;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa2_3(active0, 0x1000000000000L);
      case 98:
        return jjMoveStringLiteralDfa2_3(active0, 0x400000000000L);
      case 105:
        return jjMoveStringLiteralDfa2_3(active0, 0x800000000000L);
      case 114:
        return jjMoveStringLiteralDfa2_3(active0, 0x180000000000L);
      case 116:
        return jjMoveStringLiteralDfa2_3(active0, 0x4200000000000L);
      case 117:
        return jjMoveStringLiteralDfa2_3(active0, 0x40000000000L);
      case 121:
        return jjMoveStringLiteralDfa2_3(active0, 0x2000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(0, active0);
  }

  static private final int jjMoveStringLiteralDfa2_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(0, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(1, active0);
      return 2;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa3_3(active0, 0x4200000000000L);
      case 98:
        return jjMoveStringLiteralDfa3_3(active0, 0x40000000000L);
      case 105:
        return jjMoveStringLiteralDfa3_3(active0, 0x100000000000L);
      case 110:
        return jjMoveStringLiteralDfa3_3(active0, 0x2800000000000L);
      case 111:
        return jjMoveStringLiteralDfa3_3(active0, 0x80000000000L);
      case 115:
        return jjMoveStringLiteralDfa3_3(active0, 0x400000000000L);
      case 116:
        return jjMoveStringLiteralDfa3_3(active0, 0x1000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(1, active0);
  }

  static private final int jjMoveStringLiteralDfa3_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(1, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(2, active0);
      return 3;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa4_3(active0, 0x800000000000L);
      case 99:
        return jjMoveStringLiteralDfa4_3(active0, 0x2000000000000L);
      case 105:
        return jjMoveStringLiteralDfa4_3(active0, 0x1000000000000L);
      case 108:
        return jjMoveStringLiteralDfa4_3(active0, 0x40000000000L);
      case 116:
        return jjMoveStringLiteralDfa4_3(active0, 0x4680000000000L);
      case 118:
        return jjMoveStringLiteralDfa4_3(active0, 0x100000000000L);
      default :
        break;
    }
    return jjStartNfa_3(2, active0);
  }

  static private final int jjMoveStringLiteralDfa4_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(2, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(3, active0);
      return 4;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa5_3(active0, 0x100000000000L);
      case 101:
        return jjMoveStringLiteralDfa5_3(active0, 0x80000000000L);
      case 104:
        return jjMoveStringLiteralDfa5_3(active0, 0x2000000000000L);
      case 105:
        return jjMoveStringLiteralDfa5_3(active0, 0x4240000000000L);
      case 108:
        if ((active0 & 0x800000000000L) != 0L)
          return jjStartNfaWithStates_3(4, 47, 23);
        break;
      case 114:
        return jjMoveStringLiteralDfa5_3(active0, 0x400000000000L);
      case 118:
        return jjMoveStringLiteralDfa5_3(active0, 0x1000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(3, active0);
  }

  static private final int jjMoveStringLiteralDfa5_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(3, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(4, active0);
      return 5;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa6_3(active0, 0x400000000000L);
      case 99:
        if ((active0 & 0x40000000000L) != 0L)
          return jjStartNfaWithStates_3(5, 42, 23);
        else if ((active0 & 0x200000000000L) != 0L) {
          jjmatchedKind = 45;
          jjmatchedPos = 5;
        }
        return jjMoveStringLiteralDfa6_3(active0, 0x4080000000000L);
      case 101:
        if ((active0 & 0x1000000000000L) != 0L)
          return jjStartNfaWithStates_3(5, 48, 23);
        break;
      case 114:
        return jjMoveStringLiteralDfa6_3(active0, 0x2000000000000L);
      case 116:
        return jjMoveStringLiteralDfa6_3(active0, 0x100000000000L);
      default :
        break;
    }
    return jjStartNfa_3(4, active0);
  }

  static private final int jjMoveStringLiteralDfa6_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(4, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(5, active0);
      return 6;
    }
    switch (curChar) {
      case 99:
        return jjMoveStringLiteralDfa7_3(active0, 0x400000000000L);
      case 101:
        if ((active0 & 0x100000000000L) != 0L)
          return jjStartNfaWithStates_3(6, 44, 23);
        break;
      case 105:
        return jjMoveStringLiteralDfa7_3(active0, 0x4000000000000L);
      case 111:
        return jjMoveStringLiteralDfa7_3(active0, 0x2000000000000L);
      case 116:
        return jjMoveStringLiteralDfa7_3(active0, 0x80000000000L);
      default :
        break;
    }
    return jjStartNfa_3(5, active0);
  }

  static private final int jjMoveStringLiteralDfa7_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(5, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(6, active0);
      return 7;
    }
    switch (curChar) {
      case 101:
        return jjMoveStringLiteralDfa8_3(active0, 0x80000000000L);
      case 110:
        return jjMoveStringLiteralDfa8_3(active0, 0x6000000000000L);
      case 116:
        if ((active0 & 0x400000000000L) != 0L)
          return jjStartNfaWithStates_3(7, 46, 23);
        break;
      default :
        break;
    }
    return jjStartNfa_3(6, active0);
  }

  static private final int jjMoveStringLiteralDfa8_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(6, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(7, active0);
      return 8;
    }
    switch (curChar) {
      case 100:
        if ((active0 & 0x80000000000L) != 0L)
          return jjStartNfaWithStates_3(8, 43, 23);
        break;
      case 105:
        return jjMoveStringLiteralDfa9_3(active0, 0x6000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(7, active0);
  }

  static private final int jjMoveStringLiteralDfa9_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(7, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(8, active0);
      return 9;
    }
    switch (curChar) {
      case 116:
        return jjMoveStringLiteralDfa10_3(active0, 0x4000000000000L);
      case 122:
        return jjMoveStringLiteralDfa10_3(active0, 0x2000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(8, active0);
  }

  static private final int jjMoveStringLiteralDfa10_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(8, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(9, active0);
      return 10;
    }
    switch (curChar) {
      case 101:
        return jjMoveStringLiteralDfa11_3(active0, 0x2000000000000L);
      case 105:
        return jjMoveStringLiteralDfa11_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(9, active0);
  }

  static private final int jjMoveStringLiteralDfa11_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(9, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(10, active0);
      return 11;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa12_3(active0, 0x4000000000000L);
      case 100:
        if ((active0 & 0x2000000000000L) != 0L)
          return jjStartNfaWithStates_3(11, 49, 23);
        break;
      default :
        break;
    }
    return jjStartNfa_3(10, active0);
  }

  static private final int jjMoveStringLiteralDfa12_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(10, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(11, active0);
      return 12;
    }
    switch (curChar) {
      case 108:
        return jjMoveStringLiteralDfa13_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(11, active0);
  }

  static private final int jjMoveStringLiteralDfa13_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(11, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(12, active0);
      return 13;
    }
    switch (curChar) {
      case 105:
        return jjMoveStringLiteralDfa14_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(12, active0);
  }

  static private final int jjMoveStringLiteralDfa14_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(12, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(13, active0);
      return 14;
    }
    switch (curChar) {
      case 122:
        return jjMoveStringLiteralDfa15_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(13, active0);
  }

  static private final int jjMoveStringLiteralDfa15_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(13, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(14, active0);
      return 15;
    }
    switch (curChar) {
      case 97:
        return jjMoveStringLiteralDfa16_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(14, active0);
  }

  static private final int jjMoveStringLiteralDfa16_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(14, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(15, active0);
      return 16;
    }
    switch (curChar) {
      case 116:
        return jjMoveStringLiteralDfa17_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(15, active0);
  }

  static private final int jjMoveStringLiteralDfa17_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(15, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(16, active0);
      return 17;
    }
    switch (curChar) {
      case 105:
        return jjMoveStringLiteralDfa18_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(16, active0);
  }

  static private final int jjMoveStringLiteralDfa18_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(16, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(17, active0);
      return 18;
    }
    switch (curChar) {
      case 111:
        return jjMoveStringLiteralDfa19_3(active0, 0x4000000000000L);
      default :
        break;
    }
    return jjStartNfa_3(17, active0);
  }

  static private final int jjMoveStringLiteralDfa19_3(long old0, long active0) {
    if (((active0 &= old0)) == 0L)
      return jjStartNfa_3(17, old0);
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      jjStopStringLiteralDfa_3(18, active0);
      return 19;
    }
    switch (curChar) {
      case 110:
        if ((active0 & 0x4000000000000L) != 0L)
          return jjStartNfaWithStates_3(19, 50, 23);
        break;
      default :
        break;
    }
    return jjStartNfa_3(18, active0);
  }

  static private final int jjMoveNfa_3(int startState, int curPos) {
    int[] nextStates;
    int startsAt = 0;
    jjnewStateCnt = 23;
    int i = 1;
    jjstateSet[0] = startState;
    int j, kind = 0x7fffffff;
    for (; ;) {
      if (++jjround == 0x7fffffff)
        ReInitRounds();
      if (curChar < 64) {
        long l = 1L << curChar;
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 5:
              if (curChar == 46)
                jjCheckNAddStates(22, 24);
              if (curChar == 46) {
                if (kind > 54)
                  kind = 54;
                jjCheckNAddTwoStates(7, 8);
              }
              if (curChar == 46) {
                if (kind > 7)
                  kind = 7;
              }
              break;
            case 23:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(22, 24);
              else if (curChar == 46)
                jjCheckNAddStates(25, 27);
              if ((0x3ff0c1800000000L & l) != 0L) {
                if (kind > 54)
                  kind = 54;
                jjCheckNAddTwoStates(7, 8);
              } else if (curChar == 46)
                jjCheckNAddStates(28, 30);
              break;
            case 0:
              if ((0x3ff0c1800000000L & l) != 0L) {
                if (kind > 54)
                  kind = 54;
                jjCheckNAddStates(31, 35);
              } else if (curChar == 46)
                jjCheckNAddStates(36, 38);
              break;
            case 1:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 52)
                kind = 52;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 2:
              if (curChar == 46)
                jjCheckNAdd(3);
              break;
            case 3:
              if ((0x3ff081800000000L & l) == 0L)
                break;
              if (kind > 52)
                kind = 52;
              jjCheckNAddTwoStates(2, 3);
              break;
            case 4:
              if (curChar == 46)
                jjCheckNAddStates(36, 38);
              break;
            case 6:
              if (curChar != 46)
                break;
              if (kind > 54)
                kind = 54;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 7:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 54)
                kind = 54;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 8:
              if (curChar == 46)
                jjCheckNAddStates(25, 27);
              break;
            case 9:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 54)
                kind = 54;
              jjCheckNAddTwoStates(9, 10);
              break;
            case 10:
              if (curChar == 46)
                jjCheckNAddStates(39, 41);
              break;
            case 11:
              if (curChar == 46)
                jjCheckNAdd(12);
              break;
            case 12:
              if (curChar != 46)
                break;
              if (kind > 54)
                kind = 54;
              jjCheckNAddTwoStates(9, 10);
              break;
            case 13:
              if (curChar == 46)
                jjCheckNAddStates(22, 24);
              break;
            case 14:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(22, 24);
              break;
            case 17:
              if (curChar == 46)
                jjCheckNAddStates(28, 30);
              break;
            case 18:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(42, 44);
              break;
            case 19:
              if (curChar == 46)
                jjCheckNAddStates(45, 47);
              break;
            case 20:
              if (curChar == 46)
                jjCheckNAdd(21);
              break;
            case 21:
              if (curChar == 46)
                jjCheckNAddStates(42, 44);
              break;
            case 22:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 54)
                kind = 54;
              jjCheckNAddStates(31, 35);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else if (curChar < 128) {
        long l = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 23:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(22, 24);
              else if (curChar == 91)
                jjstateSet[jjnewStateCnt++] = 15;
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 54)
                  kind = 54;
                jjCheckNAddTwoStates(7, 8);
              }
              break;
            case 0:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 54)
                  kind = 54;
                jjCheckNAddStates(31, 35);
              } else if (curChar == 64)
                jjCheckNAdd(1);
              break;
            case 1:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 52)
                kind = 52;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 3:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 52)
                kind = 52;
              jjCheckNAddTwoStates(2, 3);
              break;
            case 7:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 54)
                kind = 54;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 9:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 54)
                kind = 54;
              jjAddStates(48, 49);
              break;
            case 14:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(22, 24);
              break;
            case 15:
              if (curChar != 93)
                break;
              kind = 55;
              jjCheckNAdd(16);
              break;
            case 16:
              if (curChar == 91)
                jjstateSet[jjnewStateCnt++] = 15;
              break;
            case 18:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(42, 44);
              break;
            case 22:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 54)
                kind = 54;
              jjCheckNAddStates(31, 35);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else {
        int i2 = (curChar & 0xff) >> 6;
        long l2 = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            default :
              break;
          }
        } while (i != startsAt);
      }
      if (kind != 0x7fffffff) {
        jjmatchedKind = kind;
        jjmatchedPos = curPos;
        kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 23 - (jjnewStateCnt = startsAt)))
        return curPos;
      try {
        curChar = input_stream.readChar();
      }
      catch (java.io.IOException e) {
        return curPos;
      }
    }
  }

  private static final int jjStopStringLiteralDfa_1(int pos, long active0, long active1) {
    switch (pos) {
      default :
        return -1;
    }
  }

  private static final int jjStartNfa_1(int pos, long active0, long active1) {
    return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0, active1), pos + 1);
  }

  static private final int jjStartNfaWithStates_1(int pos, int kind, int state) {
    jjmatchedKind = kind;
    jjmatchedPos = pos;
    try {
      curChar = input_stream.readChar();
    }
    catch (java.io.IOException e) {
      return pos + 1;
    }
    return jjMoveNfa_1(state, pos + 1);
  }

  static private final int jjMoveStringLiteralDfa0_1() {
    switch (curChar) {
      case 33:
        return jjStopAtPos(0, 80);
      case 44:
        return jjStopAtPos(0, 3);
      case 46:
        return jjStartNfaWithStates_1(0, 4, 5);
      default :
        return jjMoveNfa_1(0, 0);
    }
  }

  static private final int jjMoveNfa_1(int startState, int curPos) {
    int[] nextStates;
    int startsAt = 0;
    jjnewStateCnt = 23;
    int i = 1;
    jjstateSet[0] = startState;
    int j, kind = 0x7fffffff;
    for (; ;) {
      if (++jjround == 0x7fffffff)
        ReInitRounds();
      if (curChar < 64) {
        long l = 1L << curChar;
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 5:
              if (curChar == 46)
                jjCheckNAddStates(22, 24);
              if (curChar == 46) {
                if (kind > 76)
                  kind = 76;
                jjCheckNAddTwoStates(7, 8);
              }
              if (curChar == 46) {
                if (kind > 7)
                  kind = 7;
              }
              break;
            case 0:
              if ((0x3ff0c1800000000L & l) != 0L) {
                if (kind > 76)
                  kind = 76;
                jjCheckNAddStates(31, 35);
              } else if (curChar == 46)
                jjCheckNAddStates(36, 38);
              break;
            case 1:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 78)
                kind = 78;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 2:
              if (curChar == 46)
                jjstateSet[jjnewStateCnt++] = 3;
              break;
            case 3:
              if (curChar != 46)
                break;
              if (kind > 78)
                kind = 78;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 4:
              if (curChar == 46)
                jjCheckNAddStates(36, 38);
              break;
            case 6:
              if (curChar != 46)
                break;
              if (kind > 76)
                kind = 76;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 7:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 76)
                kind = 76;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 8:
              if (curChar == 46)
                jjCheckNAddStates(25, 27);
              break;
            case 9:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 76)
                kind = 76;
              jjCheckNAddTwoStates(9, 10);
              break;
            case 10:
              if (curChar == 46)
                jjCheckNAddStates(39, 41);
              break;
            case 11:
              if (curChar == 46)
                jjCheckNAdd(12);
              break;
            case 12:
              if (curChar != 46)
                break;
              if (kind > 76)
                kind = 76;
              jjCheckNAddTwoStates(9, 10);
              break;
            case 13:
              if (curChar == 46)
                jjCheckNAddStates(22, 24);
              break;
            case 14:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(22, 24);
              break;
            case 17:
              if (curChar == 46)
                jjCheckNAddStates(28, 30);
              break;
            case 18:
              if ((0x3ff0c1800000000L & l) != 0L)
                jjCheckNAddStates(42, 44);
              break;
            case 19:
              if (curChar == 46)
                jjCheckNAddStates(45, 47);
              break;
            case 20:
              if (curChar == 46)
                jjCheckNAdd(21);
              break;
            case 21:
              if (curChar == 46)
                jjCheckNAddStates(42, 44);
              break;
            case 22:
              if ((0x3ff0c1800000000L & l) == 0L)
                break;
              if (kind > 76)
                kind = 76;
              jjCheckNAddStates(31, 35);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else if (curChar < 128) {
        long l = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            case 0:
              if ((0x7fffffe87fffffeL & l) != 0L) {
                if (kind > 76)
                  kind = 76;
                jjCheckNAddStates(31, 35);
              } else if (curChar == 64)
                jjCheckNAddTwoStates(1, 2);
              break;
            case 1:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 78)
                kind = 78;
              jjCheckNAddTwoStates(1, 2);
              break;
            case 7:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 76)
                kind = 76;
              jjCheckNAddTwoStates(7, 8);
              break;
            case 9:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 76)
                kind = 76;
              jjAddStates(48, 49);
              break;
            case 14:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(22, 24);
              break;
            case 15:
              if (curChar != 93)
                break;
              kind = 77;
              jjCheckNAdd(16);
              break;
            case 16:
              if (curChar == 91)
                jjstateSet[jjnewStateCnt++] = 15;
              break;
            case 18:
              if ((0x7fffffe87fffffeL & l) != 0L)
                jjCheckNAddStates(42, 44);
              break;
            case 22:
              if ((0x7fffffe87fffffeL & l) == 0L)
                break;
              if (kind > 76)
                kind = 76;
              jjCheckNAddStates(31, 35);
              break;
            default :
              break;
          }
        } while (i != startsAt);
      } else {
        int i2 = (curChar & 0xff) >> 6;
        long l2 = 1L << (curChar & 077);
        MatchLoop:
        do {
          switch (jjstateSet[--i]) {
            default :
              break;
          }
        } while (i != startsAt);
      }
      if (kind != 0x7fffffff) {
        jjmatchedKind = kind;
        jjmatchedPos = curPos;
        kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 23 - (jjnewStateCnt = startsAt)))
        return curPos;
      try {
        curChar = input_stream.readChar();
      }
      catch (java.io.IOException e) {
        return curPos;
      }
    }
  }

  static final int[] jjnextStates = {
          19, 20, 21, 3, 4, 6, 7, 10, 6, 7, 10, 7, 8, 10, 6, 7,
          9, 7, 9, 10, 7, 8, 14, 16, 17, 6, 9, 11, 13, 18, 20, 7,
          14, 16, 17, 8, 5, 6, 13, 9, 11, 12, 18, 16, 19, 18, 20, 21,
          9, 10,
  };
  public static final String[] jjstrLiteralImages = {
          "", null, null, "\54", "\56", null, null, null, null, null, "\41",
          "\145\170\145\143\165\164\151\157\156\50", "\143\141\154\154\50", "\163\145\164\50", "\147\145\164\50",
          "\150\141\156\144\154\145\162\50", "\167\151\164\150\151\156\50", "\167\151\164\150\151\156\143\157\144\145\50",
          "\163\164\141\164\151\143\151\156\151\164\151\141\154\151\172\141\164\151\157\156\50", "\143\146\154\157\167\50", "\143\146\154\157\167\142\145\154\157\167\50",
          "\141\162\147\163\50", "\164\141\162\147\145\164\50", "\164\150\151\163\50", "\151\146\50\51",
          "\150\141\163\155\145\164\150\157\144\50", "\150\141\163\146\151\145\154\144\50", null, null,
          "\160\162\151\166\141\164\145", "\160\162\157\164\145\143\164\145\144", "\160\165\142\154\151\143",
          "\163\164\141\164\151\143", "\141\142\163\164\162\141\143\164", "\146\151\156\141\154", "\41", null, null,
          null, null, null, "\51", "\160\165\142\154\151\143",
          "\160\162\157\164\145\143\164\145\144", "\160\162\151\166\141\164\145", "\163\164\141\164\151\143",
          "\141\142\163\164\162\141\143\164", "\146\151\156\141\154", "\156\141\164\151\166\145",
          "\163\171\156\143\150\162\157\156\151\172\145\144",
          "\163\164\141\164\151\143\151\156\151\164\151\141\154\151\172\141\164\151\157\156", "\41", null, null, null, null, null, "\50", "\51", null,
          "\160\162\151\166\141\164\145", "\160\162\157\164\145\143\164\145\144", "\160\165\142\154\151\143",
          "\163\164\141\164\151\143", "\141\142\163\164\162\141\143\164", "\146\151\156\141\154",
          "\164\162\141\156\163\151\145\156\164", "\41", null, null, null, null, null, null, "\51", null, null, null, null, null,
          "\41", null, null, null, null, "\51", "\50", "\51",};
  public static final String[] lexStateNames = {
          "IN_ARGS",
          "PARAMETERS",
          "FIELD",
          "METHOD",
          "CLASS",
          "DEFAULT",
  };
  public static final int[] jjnewLexState = {
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3, 3, 2, 2, 4, 4, 3, 4, -1, -1, 0, 4, 4, -1,
          3, 2, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 5, -1, -1, -1, -1, -1, -1, -1, -1,
          -1, -1, -1, -1, -1, -1, -1, -1, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 5,
          -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 5, -1, -1,
  };
  static final long[] jjtoToken = {
          0xf6dffe3fffffff99L, 0xed74dfL,
  };
  static final long[] jjtoSkip = {
          0x6L, 0x0L,
  };
  static protected SimpleCharStream input_stream;
  static private final int[] jjrounds = new int[23];
  static private final int[] jjstateSet = new int[46];
  static protected char curChar;

  public ExpressionParserTokenManager(SimpleCharStream stream) {
    if (input_stream != null)
      throw new TokenMgrError("ERROR: Second call to constructor of static lexer. You must use ReInit() to initialize the static variables.", TokenMgrError.STATIC_LEXER_ERROR);
    input_stream = stream;
  }

  public ExpressionParserTokenManager(SimpleCharStream stream, int lexState) {
    this(stream);
    SwitchTo(lexState);
  }

  static public void ReInit(SimpleCharStream stream) {
    jjmatchedPos = jjnewStateCnt = 0;
    curLexState = defaultLexState;
    input_stream = stream;
    ReInitRounds();
  }

  static private final void ReInitRounds() {
    int i;
    jjround = 0x80000001;
    for (i = 23; i-- > 0;)
      jjrounds[i] = 0x80000000;
  }

  static public void ReInit(SimpleCharStream stream, int lexState) {
    ReInit(stream);
    SwitchTo(lexState);
  }

  static public void SwitchTo(int lexState) {
    if (lexState >= 6 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
    else
      curLexState = lexState;
  }

  static protected Token jjFillToken() {
    Token t = Token.newToken(jjmatchedKind);
    t.kind = jjmatchedKind;
    String im = jjstrLiteralImages[jjmatchedKind];
    t.image = (im == null) ? input_stream.GetImage() : im;
    t.beginLine = input_stream.getBeginLine();
    t.beginColumn = input_stream.getBeginColumn();
    t.endLine = input_stream.getEndLine();
    t.endColumn = input_stream.getEndColumn();
    return t;
  }

  static int curLexState = 5;
  static int defaultLexState = 5;
  static int jjnewStateCnt;
  static int jjround;
  static int jjmatchedPos;
  static int jjmatchedKind;

  public static Token getNextToken() {
    int kind;
    Token specialToken = null;
    Token matchedToken;
    int curPos = 0;

    EOFLoop :
    for (; ;) {
      try {
        curChar = input_stream.BeginToken();
      }
      catch (java.io.IOException e) {
        jjmatchedKind = 0;
        matchedToken = jjFillToken();
        return matchedToken;
      }

      switch (curLexState) {
        case 0:
          try {
            input_stream.backup(0);
            while (curChar <= 32 && (0x100000200L & (1L << curChar)) != 0L)
              curChar = input_stream.BeginToken();
          }
          catch (java.io.IOException e1) {
            continue EOFLoop;
          }
          jjmatchedKind = 0x7fffffff;
          jjmatchedPos = 0;
          curPos = jjMoveStringLiteralDfa0_0();
          break;
        case 1:
          try {
            input_stream.backup(0);
            while (curChar <= 32 && (0x100000200L & (1L << curChar)) != 0L)
              curChar = input_stream.BeginToken();
          }
          catch (java.io.IOException e1) {
            continue EOFLoop;
          }
          jjmatchedKind = 0x7fffffff;
          jjmatchedPos = 0;
          curPos = jjMoveStringLiteralDfa0_1();
          break;
        case 2:
          try {
            input_stream.backup(0);
            while (curChar <= 32 && (0x100000200L & (1L << curChar)) != 0L)
              curChar = input_stream.BeginToken();
          }
          catch (java.io.IOException e1) {
            continue EOFLoop;
          }
          jjmatchedKind = 0x7fffffff;
          jjmatchedPos = 0;
          curPos = jjMoveStringLiteralDfa0_2();
          break;
        case 3:
          try {
            input_stream.backup(0);
            while (curChar <= 32 && (0x100000200L & (1L << curChar)) != 0L)
              curChar = input_stream.BeginToken();
          }
          catch (java.io.IOException e1) {
            continue EOFLoop;
          }
          jjmatchedKind = 0x7fffffff;
          jjmatchedPos = 0;
          curPos = jjMoveStringLiteralDfa0_3();
          break;
        case 4:
          try {
            input_stream.backup(0);
            while (curChar <= 32 && (0x100000200L & (1L << curChar)) != 0L)
              curChar = input_stream.BeginToken();
          }
          catch (java.io.IOException e1) {
            continue EOFLoop;
          }
          jjmatchedKind = 0x7fffffff;
          jjmatchedPos = 0;
          curPos = jjMoveStringLiteralDfa0_4();
          break;
        case 5:
          try {
            input_stream.backup(0);
            while (curChar <= 32 && (0x100000200L & (1L << curChar)) != 0L)
              curChar = input_stream.BeginToken();
          }
          catch (java.io.IOException e1) {
            continue EOFLoop;
          }
          jjmatchedKind = 0x7fffffff;
          jjmatchedPos = 0;
          curPos = jjMoveStringLiteralDfa0_5();
          break;
      }
      if (jjmatchedKind != 0x7fffffff) {
        if (jjmatchedPos + 1 < curPos)
          input_stream.backup(curPos - jjmatchedPos - 1);
        if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L) {
          matchedToken = jjFillToken();
          if (jjnewLexState[jjmatchedKind] != -1)
            curLexState = jjnewLexState[jjmatchedKind];
          return matchedToken;
        } else {
          if (jjnewLexState[jjmatchedKind] != -1)
            curLexState = jjnewLexState[jjmatchedKind];
          continue EOFLoop;
        }
      }
      int error_line = input_stream.getEndLine();
      int error_column = input_stream.getEndColumn();
      String error_after = null;
      boolean EOFSeen = false;
      try {
        input_stream.readChar();
        input_stream.backup(1);
      }
      catch (java.io.IOException e1) {
        EOFSeen = true;
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
        if (curChar == '\n' || curChar == '\r') {
          error_line++;
          error_column = 0;
        } else
          error_column++;
      }
      if (!EOFSeen) {
        input_stream.backup(1);
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
      }
      throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
    }
  }

}
