package com.tc.objectserver.api;

import com.tc.object.net.DSOChannelManagerEventListener;

/**
 * @author tim
 */
public interface ResourceManager extends DSOChannelManagerEventListener {
  boolean isThrowException();

  void setThrottle(float ratio);

  void setThrowException();

  void clear();
}
