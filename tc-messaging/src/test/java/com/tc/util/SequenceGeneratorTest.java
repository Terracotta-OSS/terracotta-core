/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import org.junit.Test;

import static com.tc.util.Assert.assertEquals;

public class SequenceGeneratorTest {
  
  @Test
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
