/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.msg;

import java.util.Set;

public class NullMessageRecycler implements MessageRecycler {

  public void addMessage(DSOMessageBase message, Set keys) {
    return;
  }

  public boolean recycle(Object key) {
    return false;
  }

}
