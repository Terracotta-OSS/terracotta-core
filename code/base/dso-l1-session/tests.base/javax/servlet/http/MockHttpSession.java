/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package javax.servlet.http;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletContext;

public class MockHttpSession implements HttpSession {

  private final Hashtable attributes   = new Hashtable();
  private long            createMillis = 0;
  private String          id;
  private long            lastAccessTime;
  private int             maxIntactiveSeconds;
  private boolean         isNew;

  public MockHttpSession(String id) {
    this.id = id;
  }

  public Object getAttribute(String s) {
    return attributes.get(s);
  }

  public Enumeration getAttributeNames() {
    return attributes.keys();
  }

  public long getCreationTime() {
    return createMillis;
  }

  public String getId() {
    return id;
  }

  public long getLastAccessedTime() {
    return lastAccessTime;
  }

  public int getMaxInactiveInterval() {
    return maxIntactiveSeconds;
  }

  public ServletContext getServletContext() {
    return null;
  }

  public HttpSessionContext getSessionContext() {
    return null;
  }

  public Object getValue(String s) {
    return getAttribute(s);
  }

  public String[] getValueNames() {
    return (String[]) attributes.keySet().toArray(new String[attributes.size()]);
  }

  public void invalidate() {
    // NOTE: implement
  }

  public boolean isNew() {
    return isNew;
  }

  public void putValue(String s, Object obj) {
    setAttribute(s, obj);
  }

  public void removeAttribute(String s) {
    attributes.remove(s);
  }

  public void removeValue(String s) {
    removeAttribute(s);
  }

  public void setAttribute(String s, Object obj) {
    attributes.put(s, obj);
  }

  public void setMaxInactiveInterval(int i) {
    this.maxIntactiveSeconds = i;
  }

}
