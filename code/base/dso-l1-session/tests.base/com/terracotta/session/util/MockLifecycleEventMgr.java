/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.Session;

public class MockLifecycleEventMgr implements LifecycleEventMgr {

  private Session sess;
  private String  name;
  private Object  val;
  private String  lastMethod;


  public void clear() {
    sess = null;
    name = null;
    val = null;
    lastMethod = null;
  }

  public void fireSessionCreatedEvent(Session s) {
    this.lastMethod = "fireSessionCreatedEvent";
    this.sess = s;
  }

  public void fireSessionDestroyedEvent(Session s) {
    this.lastMethod = "fireSessionDestroyedEvent";
    this.sess = s;
  }

  public void unbindAttribute(Session s, String n, Object ov) {
    this.lastMethod = "unbindAttribute";
    this.sess = s;
    this.name = n;
    this.val = ov;
  }

  public void bindAttribute(Session s, String n, Object o) {
    this.lastMethod = "bindAttribute";
    this.sess = s;
    this.name = n;
    this.val = o;
  }

  public void removeAttribute(Session s, String n, Object o) {
    this.lastMethod = "removeAttribute";
    this.sess = s;
    this.name = n;
    this.val = o;
  }

  public void replaceAttribute(Session s, String n, Object o, Object newVal) {
    this.lastMethod = "replaceAttribute";
    this.sess = s;
    this.name = n;
    this.val = o;
  }

  public void setAttribute(Session s, String n, Object o) {
    this.lastMethod = "setAttribute";
    this.sess = s;
    this.name = n;
    this.val = o;
  }

  public String getLastMethod() {
    return lastMethod;
  }

  public String getName() {
    return name;
  }

  public Object getValue() {
    return val;
  }

  public Session getSession() {
    return sess;
  }

}
