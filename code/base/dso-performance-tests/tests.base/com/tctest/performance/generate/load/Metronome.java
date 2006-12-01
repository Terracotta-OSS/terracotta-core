/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.generate.load;

public class Metronome implements Comparable {

  public Object      object;
  public long        seqNum, load, starttime, endtime;
  public static long globalSeqNum;

  Metronome(Object obj) {
    this.object = obj;
    seqNum = globalSeqNum++;
  }
  
  public int compareTo(Object obj) {
    return new Long(seqNum).compareTo(new Long(((Metronome) obj).seqNum));
  }
}
