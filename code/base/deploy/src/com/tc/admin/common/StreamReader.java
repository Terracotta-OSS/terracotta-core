/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.SwingUtilities;

public class StreamReader extends Thread {
  TextPaneUpdater      m_updater;
  InputStream          m_stream;
  InputStreamReader    m_streamReader;
  BufferedReader       m_bufferedReader;
  boolean              m_stop;
  OutputStreamListener m_listener;
  String               m_trigger;

  public StreamReader(
    InputStream          stream,
    OutputStreamListener listener,
    String               trigger)
  {
    this(stream, null, listener, trigger);
  }
  
  public StreamReader(
    InputStream          stream,
    TextPaneUpdater      updater,
    OutputStreamListener listener,
    String               trigger)
  {
    m_updater        = updater;
    m_stream         = stream;
    m_streamReader   = new InputStreamReader(stream);
    m_bufferedReader = new BufferedReader(m_streamReader);
    m_stop           = false;
    m_listener       = listener;
    m_trigger        = trigger;
  }
  
  public void setTriggerListener(OutputStreamListener listener) {
    m_listener = listener;
  }
  
  public void setTrigger(String trigger) {
    m_trigger = trigger;
  }
  
  public void run() {
    String line;
    
    while(!m_stop) {
      try {
        if((line = m_bufferedReader.readLine()) == null) {
          IOUtils.closeQuietly(m_bufferedReader);
          return;
        }
        else {
          if(m_updater != null) {
            update(line);
          }
          if(m_listener != null && m_trigger != null &&
             StringUtils.contains(line, m_trigger))
          {
            SwingUtilities.invokeLater(new Runnable()  {
              public void run() {
                m_listener.triggerEncountered();
              }
            });
          }
          continue;
        }
      } catch(IOException ioe) {
        IOUtils.closeQuietly(m_bufferedReader);
        return;
      }
    }
    
    IOUtils.closeQuietly(m_bufferedReader);
  }

  private void update(String line) {
    try {
      m_updater.setLine(line);
      SwingUtilities.invokeAndWait(m_updater);
    } catch(Exception e) {/**/}
  }
  
  public synchronized void finish() {
    m_stop = true;
    IOUtils.closeQuietly(m_bufferedReader);
  }
}
