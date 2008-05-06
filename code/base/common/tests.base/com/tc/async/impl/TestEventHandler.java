/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;

import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class TestEventHandler extends AbstractEventHandler {
  private LinkedList contexts = new LinkedList();

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.async.api.EventHandler#handleEvent(com.tc.async.api.EventContext)
   */
  public void handleEvent(EventContext context) {
    contexts.add(context);
  }

  public List getContexts() {
    return contexts;
  }
}