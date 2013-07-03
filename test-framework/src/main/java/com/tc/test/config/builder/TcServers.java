package com.tc.test.config.builder;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class TcServers {

  @XStreamImplicit
  private final List<TcConfigChild> children = new ArrayList<TcConfigChild>();

  @XStreamAsAttribute
  private boolean secure;

  public List<TcConfigChild> getChildren() {
    return children;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public TcServers secure(boolean secure) {
    setSecure(secure);
    return this;
  }

}
