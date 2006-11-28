/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc;

public class WebApp {
  String m_name;
  String m_path;
  
  public WebApp(String name, String path) {
    m_name = name;
    m_path = path;
  }

  public String getName() {
    return m_name;
  }
  
  public String getPath() {
    return m_path;
  }
  
  public String toString() {
    return m_name;
  }
}
