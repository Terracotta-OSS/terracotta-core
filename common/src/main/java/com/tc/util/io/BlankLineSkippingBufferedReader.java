/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class BlankLineSkippingBufferedReader extends BufferedReader {

  public BlankLineSkippingBufferedReader(Reader in, int sz) {
    super(in, sz);
  }

  public BlankLineSkippingBufferedReader(Reader in) {
    super(in);
  }

  public String readLine() throws IOException {
    String out;

    do {
      out = super.readLine();
    } while (out != null && out.trim().length() == 0);

    return out;
  }

}
