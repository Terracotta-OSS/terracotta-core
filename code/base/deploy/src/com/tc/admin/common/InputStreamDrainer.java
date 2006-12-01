/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Reads lines from an InputStream until either EOF is reached or an
 * IOException is raised.
 */

public class InputStreamDrainer extends Thread {
  private InputStream  m_stream;
  private StringBuffer m_buffer;
      
  private static final String LINE_SEP = System.getProperty("line.separator");
  
  public InputStreamDrainer(InputStream stream) {
    m_stream = stream;
  }
    
  public void run() {
    InputStreamReader streamReader   = new InputStreamReader(m_stream);
    BufferedReader    bufferedReader = new BufferedReader(streamReader);
    String            line;
    
    m_buffer = new StringBuffer();
    
    while(true) {
      try {
        if((line = bufferedReader.readLine()) == null) {
          IOUtils.closeQuietly(bufferedReader);
          return;
        }
        m_buffer.append(line+LINE_SEP);
      } catch(Exception e) {
        IOUtils.closeQuietly(bufferedReader);
        return;
      }
    }
  }
  
  public String getBufferContent() {
    return m_buffer.toString();
  }
}
