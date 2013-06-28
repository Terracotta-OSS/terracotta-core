package com.tc.test.config.builder;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author Ludovic Orban
 */
@XStreamAlias("restartable")
public class Restartable implements TcConfigChild {

  @XStreamAsAttribute
  private boolean enabled;

  public Restartable() {
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Restartable enabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

}
