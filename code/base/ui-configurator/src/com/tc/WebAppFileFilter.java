/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class WebAppFileFilter extends FileFilter {
  private static final String WEB_APP_FILE_EXT = ".war";
  private static final String DESCRIPTION      = "J2EE webapps";

  public static WebAppFileFilter m_instance;
  
  public static WebAppFileFilter getInstance() {
    if(m_instance == null) {
      m_instance = new WebAppFileFilter();
    }
    return m_instance;
  }
  
  public boolean accept(File f) {
    return f.isDirectory() || f.getName().endsWith(WEB_APP_FILE_EXT);
  }
  
  public String getDescription() {
    return DESCRIPTION;
  }
}
