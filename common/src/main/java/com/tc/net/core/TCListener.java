/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.core;

import com.tc.net.core.event.TCListenerEventListener;
import com.tc.util.TCTimeoutException;

import java.net.InetSocketAddress;

/**
 * A handle to a network listening port
 * 
 * @author teck
 */
public interface TCListener {
  public void stop();

  public void stop(long timeout) throws TCTimeoutException;

  public InetSocketAddress getBindSocketAddress();

  public void addEventListener(TCListenerEventListener lsnr);

  public void removeEventListener(TCListenerEventListener lsnr);

  public boolean isStopped();
}
