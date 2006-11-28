/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public class SequenceGeneratorTest extends TestCase {
  
  public void tests() throws Exception {
    long seq = 0;
    SequenceGenerator sg = new SequenceGenerator();
    assertEquals(seq, sg.getNextSequence());
    assertEquals(seq, sg.getCurrentSequence());
    
    assertEquals(++seq, sg.getNextSequence());
    assertEquals(seq, sg.getCurrentSequence());
    
    seq = 1001;
    sg = new SequenceGenerator(seq);
    assertEquals(seq, sg.getNextSequence());
    assertEquals(seq, sg.getCurrentSequence());
    assertEquals(++seq, sg.getNextSequence());
    assertEquals(seq, sg.getCurrentSequence());
    
  }

}
