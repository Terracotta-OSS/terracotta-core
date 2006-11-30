/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.core.event;

import com.tc.net.core.TCListener;

/**
 * Event emitted from a TCListener
 * 
 * @author teck
 */
public interface TCListenerEvent {
  public TCListener getSource();
}