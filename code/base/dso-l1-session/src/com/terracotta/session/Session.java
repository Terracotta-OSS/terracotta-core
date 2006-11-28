package com.terracotta.session;

import javax.servlet.http.HttpSession;

public interface Session extends HttpSession {

  public SessionData getSessionData();

  public SessionId getSessionId();

  public void bindAttribute(String name, Object newVal);

  public void unbindAttribute(String name);

  public void setInvalid();

  public boolean isValid();
}
