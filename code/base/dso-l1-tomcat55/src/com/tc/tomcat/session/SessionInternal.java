/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.tomcat.session;

import org.apache.catalina.Manager;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.SessionListener;
import org.apache.catalina.cluster.session.SerializablePrincipal;
import org.apache.catalina.realm.GenericPrincipal;

import com.terracotta.session.SessionData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpSession;

public class SessionInternal implements Session {

  private static final String                  AUTH_TYPE          = "tomcatAuthType";
  private static final String                  NOTE               = "tomcatNote";
  private static final String                  TOMCAT_PRINCIPAL   = "tomcatPrincipal";
  private static final String                  PORTABLE_PRINCIPAL = "tomcatPortablePrincipal";

  private final com.terracotta.session.Session tcSession;
  private final SessionData                    sessionData;
  private final Realm                          realm;

  public SessionInternal(com.terracotta.session.Session tcSession, Realm realm) {
    this.tcSession = tcSession;
    this.sessionData = tcSession.getSessionData();
    this.realm = realm;
  }

  public void access() {
    //
  }

  public void addSessionListener(SessionListener listener) {
    //
  }

  public void endAccess() {
    //
  }

  public void expire() {
    throw new UnsupportedOperationException();
  }

  public String getAuthType() {
    return (String) sessionData.getTransientAttribute(AUTH_TYPE);
  }

  public long getCreationTime() {
    return tcSession.getCreationTime();
  }

  public String getId() {
    return tcSession.getId();
  }

  public String getIdInternal() {
    return tcSession.getId();
  }

  public String getInfo() {
    return "TerracottaSessionInternal";
  }

  public long getLastAccessedTime() {
    return tcSession.getLastAccessedTime();
  }

  public long getLastAccessedTimeInternal() {
    throw new UnsupportedOperationException();
  }

  public Manager getManager() {
    throw new UnsupportedOperationException();
  }

  public int getMaxInactiveInterval() {
    return tcSession.getMaxInactiveInterval();
  }

  public Object getNote(String name) {
    return sessionData.getTransientAttribute(makeNoteName(name));
  }

  public Iterator getNoteNames() {
    Collection keys = sessionData.getTransientAttributeKeys();
    for (Iterator i = keys.iterator(); i.hasNext();) {
      String key = (String) i.next();
      if (!key.startsWith(NOTE)) {
        i.remove();
      }
    }

    return keys.iterator();
  }

  public Principal getPrincipal() {
    Principal p = (Principal) sessionData.getInternalAttribute(PORTABLE_PRINCIPAL);
    if (p != null) { return p; }

    byte[] principal = (byte[]) sessionData.getInternalAttribute(TOMCAT_PRINCIPAL);
    if (principal != null) { return deserializeGenericPrincipal(principal); }

    return null;
  }

  public HttpSession getSession() {
    return tcSession;
  }

  public boolean isValid() {
    return tcSession.isValid();
  }

  public void recycle() {
    //
  }

  public void removeNote(String name) {
    sessionData.removeTransientAttribute(makeNoteName(name));
  }

  public void removeSessionListener(SessionListener listener) {
    //
  }

  public void setAuthType(String authType) {
    sessionData.setTransientAttribute(AUTH_TYPE, authType);
  }

  public void setCreationTime(long time) {
    throw new UnsupportedOperationException();
  }

  public void setId(String id) {
    throw new UnsupportedOperationException();
  }

  public void setManager(Manager manager) {
    throw new UnsupportedOperationException();
  }

  public void setMaxInactiveInterval(int interval) {
    throw new UnsupportedOperationException();
  }

  public void setNew(boolean isNew) {
    throw new UnsupportedOperationException();
  }

  public void setNote(String name, Object value) {
    sessionData.setTransientAttribute(makeNoteName(name), value);
  }

  public void setPrincipal(Principal principal) {
    if (principal == null) {
      sessionData.removeInternalAttribute(TOMCAT_PRINCIPAL);
      sessionData.removeAttribute(PORTABLE_PRINCIPAL);
    }

    final boolean isTomcatInternalPrincipal = (principal instanceof GenericPrincipal);
    sessionData.removeAttribute(isTomcatInternalPrincipal ? PORTABLE_PRINCIPAL : TOMCAT_PRINCIPAL);

    if (isTomcatInternalPrincipal) {
      sessionData.setInternalAttribute(TOMCAT_PRINCIPAL, serializeGenericPrincipal((GenericPrincipal) principal));
    } else {
      sessionData.setInternalAttribute(PORTABLE_PRINCIPAL, principal);
    }
  }

  private static byte[] serializeGenericPrincipal(GenericPrincipal principal) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try {
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      SerializablePrincipal.writePrincipal(principal, oos);
      oos.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error serializing principal", e);
    }

    return baos.toByteArray();
  }

  private GenericPrincipal deserializeGenericPrincipal(byte[] data) {
    try {
      return SerializablePrincipal.readPrincipal(new ObjectInputStream(new ByteArrayInputStream(data)), realm);
    } catch (IOException e) {
      throw new RuntimeException("Error creating principal", e);
    }
  }

  public void setValid(boolean isValid) {
    throw new UnsupportedOperationException();
  }

  private static String makeNoteName(String name) {
    return NOTE + name;
  }

}