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