package com.tc.objectserver.api;

import com.tc.object.net.DSOChannelManagerEventListener;

/**
 * @author tim
 */
public interface ResourceManager extends DSOChannelManagerEventListener {
  public enum State {
    NORMAL, THROTTLED, RESTRICTED
  }

  public State getState();

  void setThrottle(float ratio);

  void setThrowException();

  void clear();
}
