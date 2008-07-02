/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.awt.Point;
import java.util.Date;

public class ThreadDumpEntry {
  private String m_threadDumpText;
  private Date   m_time;
  private Point  m_viewPosition;

  public ThreadDumpEntry(String threadDumpText) {
    m_threadDumpText = threadDumpText;
    m_time = new Date();
  }

  public String getThreadDumpText() {
    return m_threadDumpText;
  }

  public Date getTime() {
    return new Date(m_time.getTime());
  }

  public void setViewPosition(Point pos) {
    m_viewPosition = pos;
  }

  public Point getViewPosition() {
    return m_viewPosition;
  }

  public String toString() {
    return m_time.toString();
  }
}