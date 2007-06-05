/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import java.lang.reflect.Field;

/**
 * generates session cookie id of the format: <4hex:random><4hex:nextId>[[16hex:random]*]
 */
public class WebsphereIdDeclarator implements IdDeclarator {
  private static final String SessionContextClassName = "com.ibm.ws.webcontainer.httpsession.SessionContext";
  private static final String CLONE_SEP                     = getServerDelimiter();

  public String transform(String sessionId) {
    return getCacheId() + sessionId + CLONE_SEP + getCloneId();
  }

  private static String getServerDelimiter() {
    Class sessionContextClass;
    try {
      sessionContextClass = WebsphereIdDeclarator.class.getClassLoader().loadClass(SessionContextClassName);
      Field f = sessionContextClass.getDeclaredField("cloneSeparator");
      f.setAccessible(true);
      Character val = (Character) f.get(null);
      return val.toString();
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    } catch (SecurityException e) {
      throw new AssertionError(e);
    } catch (NoSuchFieldException e) {
      throw new AssertionError(e);
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
  
  private String getCloneId() {
    Class sessionContextClass;
    try {
      sessionContextClass = this.getClass().getClassLoader().loadClass(SessionContextClassName);
      Field f = sessionContextClass.getDeclaredField("cloneId");
      f.setAccessible(true);
      String val = (String) f.get(null);
      return val;
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    } catch (SecurityException e) {
      throw new AssertionError(e);
    } catch (NoSuchFieldException e) {
      throw new AssertionError(e);
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  // TODO: This method needs to be refactor. Currently, it just get the default cache id from the Websphere Session
  // Context.
  // For more information, refer to CDV-258.
  private String getCacheId() {
    Class sessionContextClass;
    try {
      sessionContextClass = this.getClass().getClassLoader().loadClass(SessionContextClassName);
      Field f = sessionContextClass.getDeclaredField("defaultCacheId");
      f.setAccessible(true);
      String val = (String) f.get(null);
      return val;
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    } catch (SecurityException e) {
      throw new AssertionError(e);
    } catch (NoSuchFieldException e) {
      throw new AssertionError(e);
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
}
