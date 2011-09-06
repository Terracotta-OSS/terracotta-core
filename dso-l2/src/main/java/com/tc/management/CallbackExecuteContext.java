/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management;

import com.sun.jmx.remote.generic.SynchroCallback;
import com.tc.async.api.EventContext;
import com.tc.util.concurrent.TCFuture;

import javax.management.remote.message.Message;

public class CallbackExecuteContext implements EventContext {
  private final TCFuture future;
  private final Message request;
  private final ClassLoader threadContextLoader;
  private final SynchroCallback callback;

  public CallbackExecuteContext(ClassLoader threadContextLoader, SynchroCallback callback, Message request, TCFuture future) {
    this.threadContextLoader = threadContextLoader;
    this.callback = callback;
    this.request = request;
    this.future = future;
  }

  public TCFuture getFuture() {
    return future;
  }

  public Message getRequest() {
    return request;
  }

  public ClassLoader getThreadContextLoader() {
    return threadContextLoader;
  }

  public SynchroCallback getCallback() {
    return callback;
  }
}