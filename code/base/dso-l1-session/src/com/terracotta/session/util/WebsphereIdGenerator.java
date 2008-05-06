/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.util.Assert;
import com.terracotta.session.SessionId;

import java.lang.reflect.Field;

public class WebsphereIdGenerator extends DefaultIdGenerator {

  private static final String SessionContextClassName = "com.ibm.ws.webcontainer.httpsession.SessionContext";

  private final String        cacheId                 = getCacheId();
  private final String        cloneId                 = getCloneId();
  private final int           cacheIdLength           = cacheId.length();

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

  public SessionId makeInstanceFromBrowserId(String requestedSessionId) {
    if (requestedSessionId != null) {
      // trim the cacheId and cloneId from this point on

      if (requestedSessionId.length() >= cacheIdLength) {
        requestedSessionId = requestedSessionId.substring(cacheIdLength);
      }

      int dlmIndex = requestedSessionId.lastIndexOf(getDelimiter());
      if (dlmIndex >= 0) {
        requestedSessionId = requestedSessionId.substring(0, dlmIndex);
      }
    }

    return super.makeInstanceFromBrowserId(requestedSessionId);
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
