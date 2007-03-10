/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.exception.ImplementMe;
import com.terracotta.session.Session;
import com.terracotta.session.SessionData;
import com.terracotta.session.SessionId;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

public class MockSession implements Session {

  public SessionData getSessionData() {
    throw new ImplementMe();
  }

  public SessionId getSessionId() {
    throw new ImplementMe();
  }

  public boolean isValid() {
    throw new ImplementMe();
  }

  public Object getAttribute(String arg0) {
    throw new ImplementMe();
  }

  public Enumeration getAttributeNames() {
    throw new ImplementMe();
  }

  public long getCreationTime() {
    throw new ImplementMe();
  }

  public String getId() {
    throw new ImplementMe();
  }

  public long getLastAccessedTime() {
    throw new ImplementMe();
  }

  public int getMaxInactiveInterval() {
    throw new ImplementMe();
  }

  public ServletContext getServletContext() {
    throw new ImplementMe();
  }

  public HttpSessionContext getSessionContext() {
    throw new ImplementMe();
  }

  public Object getValue(String arg0) {
    throw new ImplementMe();
  }

  public String[] getValueNames() {
    throw new ImplementMe();
  }

  public void invalidate() {
    throw new ImplementMe();
  }

  public boolean isNew() {
    throw new ImplementMe();
  }

  public void putValue(String arg0, Object arg1) {
    throw new ImplementMe();
  }

  public void removeAttribute(String arg0) {
    throw new ImplementMe();
  }

  public void removeValue(String arg0) {
    throw new ImplementMe();
  }

  public void setAttribute(String arg0, Object arg1) {
    throw new ImplementMe();
  }

  public void setMaxInactiveInterval(int arg0) {
    throw new ImplementMe();
  }

}
