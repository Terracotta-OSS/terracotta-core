/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.util.Assert;

import java.lang.reflect.Field;

public class WebsphereIdGenerator extends DefaultIdGenerator {

  private static final String SessionContextClassName = "com.ibm.ws.webcontainer.httpsession.SessionContext";

  private final String        cacheId                 = getCacheId();
  private final String        cloneId                 = getCloneId();

  private final String        tcDelimiter;

  public WebsphereIdGenerator(int idLength, String serverId, int lockType, final String delimiter) {
    super(idLength, serverId, lockType, delimiter);

    String tcDefaultDelimiter = ConfigProperties.defaultDelimiter;

    // if you're removing this assertion, we can't blindly use ":" below
    Assert.assertTrue(tcDefaultDelimiter.indexOf(':') < 0);
    if (delimiter.equals(tcDefaultDelimiter)) {
      this.tcDelimiter = ":";
    } else {
      this.tcDelimiter = tcDefaultDelimiter;
    }

    Assert.assertFalse(tcDelimiter.equals(delimiter));
  }

  protected String makeExternalId(String key) {
    // embed the terracotta client id using a different delimiter -- without this we can't detect server hops when WAS
    // cloneId not set
    return cacheId + key + tcDelimiter + getServerId() + getDelimiter() + cloneId;
  }

  protected int getDLMIndex(String requestedSessionId) {
    return requestedSessionId.indexOf(tcDelimiter);
  }

  private String getCloneId() {
    try {
      Class sessionContextClass = this.getClass().getClassLoader().loadClass(SessionContextClassName);
      Field f = sessionContextClass.getDeclaredField("cloneId");
      f.setAccessible(true);
      String val = (String) f.get(null);
      return val;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  // TODO: This method needs to be refactor. Currently, it just get the default cache id from the Websphere Session
  // Context. For more information, refer to CDV-258.
  private String getCacheId() {
    Class sessionContextClass;
    try {
      sessionContextClass = this.getClass().getClassLoader().loadClass(SessionContextClassName);
      Field f = sessionContextClass.getDeclaredField("defaultCacheId");
      f.setAccessible(true);
      String val = (String) f.get(null);
      return val;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

}
