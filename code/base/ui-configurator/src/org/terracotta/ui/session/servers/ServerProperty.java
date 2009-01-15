/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session.servers;

public class ServerProperty {
  private String name;
  private String value;

  public ServerProperty() {/**/
  }

  public ServerProperty(String name, String value) {
    setName(name);
    setValue(value);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String toString() {
    return name + "=" + value;
  }
}
