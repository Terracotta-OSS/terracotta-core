/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.msg;

import java.util.Set;

public class GCResultMessageFactory {

  public static GCResultMessage createGCResultMessage(Set deleted) {
    return new GCResultMessage(GCResultMessage.GC_RESULT, deleted);
  }

}
