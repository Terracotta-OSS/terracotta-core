/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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