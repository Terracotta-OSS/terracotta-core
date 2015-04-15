/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
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
