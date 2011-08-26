/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;

import java.util.Set;

public class NullMessageRecycler implements MessageRecycler {

  public void addMessage(Recyclable message, Set keys) {
    return;
  }

  public boolean recycle(Object key) {
    return false;
  }

}
