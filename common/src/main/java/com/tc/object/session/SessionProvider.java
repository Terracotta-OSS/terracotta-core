/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

public interface SessionProvider {
  public void initProvider();
  
  public SessionID getSessionID();
  
  public SessionID nextSessionID();

  public void resetSessionProvider();
}
