/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;

public class URLEncoder {

  // Not static - the set may differ ( it's better than adding
  // an extra check for "/", "+", etc
  private BitSet       safeChars = null;
  private C2BConverter c2b       = null;
  private ByteChunk    bb        = null;

  private String       encoding  = "UTF8";

  public URLEncoder() {
    initSafeChars();
  }

  public void setEncoding(String s) {
    encoding = s;
  }

  public void addSafeCharacter(char c) {
    safeChars.set(c);
  }

  /**
   * URL Encode string, using a specified encoding.
   * 
   * @param buf The writer
   * @param s string to be encoded
   * @throws IOException If an I/O error occurs
   */
  public void urlEncode(Writer buf, String s) throws IOException {
    if (c2b == null) {
      bb = new ByteChunk(16); // small enough.
      c2b = new C2BConverter(bb, encoding);
    }

    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (safeChars.get(c)) {
        buf.write((char) c);
      } else {
        c2b.convert((char) c);

        // "surrogate" - UTF is _not_ 16 bit, but 21 !!!!
        // ( while UCS is 31 ). Amazing...
        if (c >= 0xD800 && c <= 0xDBFF) {
          if ((i + 1) < s.length()) {
            int d = s.charAt(i + 1);
            if (d >= 0xDC00 && d <= 0xDFFF) {
              c2b.convert((char) d);
              i++;
            }
          }
        }

        c2b.flushBuffer();

        urlEncode(buf, bb.getBuffer(), bb.getOffset(), bb.getLength());
        bb.recycle();
      }
    }
  }

  /**
     */
  public void urlEncode(Writer buf, byte bytes[], int off, int len) throws IOException {
    for (int j = off; j < len; j++) {
      buf.write('%');
      char ch = Character.forDigit((bytes[j] >> 4) & 0xF, 16);
      buf.write(ch);
      ch = Character.forDigit(bytes[j] & 0xF, 16);
      buf.write(ch);
    }
  }

  /**
   * Utility function to re-encode the URL. Still has problems with charset, since UEncoder mostly ignores it.
   */
  public String encodeURL(String uri) {
    String outUri = null;
    try {
      // XXX optimize - recycle, etc
      CharArrayWriter out = new CharArrayWriter();
      urlEncode(out, uri);
      outUri = out.toString();
    } catch (IOException iex) {
      //
    }
    return outUri;
  }

  // -------------------- Internal implementation --------------------

  private void initSafeChars() {
    safeChars = new BitSet(128);
    int i;
    for (i = 'a'; i <= 'z'; i++) {
      safeChars.set(i);
    }
    for (i = 'A'; i <= 'Z'; i++) {
      safeChars.set(i);
    }
    for (i = '0'; i <= '9'; i++) {
      safeChars.set(i);
    }
    // safe
    safeChars.set('$');
    safeChars.set('-');
    safeChars.set('_');
    safeChars.set('.');

    // Dangerous: someone may treat this as " "
    // RFC1738 does allow it, it's not reserved
    // safeChars.set('+');
    // extra
    safeChars.set('!');
    safeChars.set('*');
    safeChars.set('\'');
    safeChars.set('(');
    safeChars.set(')');
    safeChars.set(',');
  }
}
