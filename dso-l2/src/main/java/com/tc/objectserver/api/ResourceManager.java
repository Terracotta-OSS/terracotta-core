package com.tc.objectserver.api;

import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.text.PrettyPrintable;

/**
 * @author tim
 */
public interface ResourceManager extends DSOChannelManagerEventListener, PrettyPrintable {
  public enum State {
    NORMAL, THROTTLED, RESTRICTED
  }

  public State getState();

  void setThrottle(float ratio);

  void setRestricted();

  void resetState();
}
