package com.tc.test.config.builder;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
@XStreamAlias("mirror-group")
public class TcMirrorGroup implements TcConfigChild {
  
  @XStreamAlias("group-name")
  @XStreamAsAttribute
  private String groupName;

  @XStreamImplicit
  private List<TcMirrorGroupChild> children = new ArrayList<TcMirrorGroupChild>();

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public TcMirrorGroup groupName(String groupName) {
    setGroupName(groupName);
    return this;
  }

  public List<TcMirrorGroupChild> getChildren() {
    return children;
  }

  public TcMirrorGroup server(TcServer server) {
    children.add(server);
    return this;
  }

}
