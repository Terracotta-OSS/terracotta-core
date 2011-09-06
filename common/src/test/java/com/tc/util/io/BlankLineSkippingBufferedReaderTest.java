/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.io;

import com.tc.test.TCTestCase;

import java.io.StringReader;

/**
 * Unit test for {@link BlankLineSkippingBufferedReader}.
 */
public class BlankLineSkippingBufferedReaderTest extends TCTestCase {

  public void test() throws Exception {
    BlankLineSkippingBufferedReader reader = new BlankLineSkippingBufferedReader(
                                                                                 new StringReader(
                                                                                                  "foo\nbar\n\n\nbaz\n\nquux\n"));
    assertEquals("foo", reader.readLine());
    assertEquals("bar", reader.readLine());
    assertEquals("baz", reader.readLine());
    assertEquals("quux", reader.readLine());
  }

}
