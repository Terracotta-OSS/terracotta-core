/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;

import java.util.LinkedList;
import java.util.List;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
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