/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.servers;

public class ServerProperty {
  private String m_name;
  private String m_value;
  
  public ServerProperty() {/**/}
  
  public ServerProperty(String name, String value) {
    m_name  = name;
    m_value = value;
  }
  
  public String getName() {
    return m_name;
  }
  
  public void setName(String name) {
    m_name = name;
  }
  
  public String getValue() {
    return m_value;
  }
  
  public void setValue(String value) {
    m_value = value;
  }
  
  public String toString() {
    return m_name+"="+m_value;
  }
}
