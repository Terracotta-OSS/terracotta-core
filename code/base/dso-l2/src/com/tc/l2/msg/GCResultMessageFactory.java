/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import java.util.Set;

public class GCResultMessageFactory {

  public static GCResultMessage createGCResultMessage(int gcIterationCount, Set deleted) {
    return new GCResultMessage(GCResultMessage.GC_RESULT, gcIterationCount, deleted);
  }

}
