/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.entity;

import java.util.Properties;

public interface ActiveInvokeContext<R extends EntityResponse> extends InvokeContext {
  /**
   * source instance from which the invocation originates.
   *
   * @return descriptor
   */
  ClientDescriptor getClientDescriptor();
  /**
   * Opens a channel to consumer of this context.If the context is associated with a
   *  client side invoke, this will open a channel to the InvokeMonitor on the client.  If this context is associated with a server invoke, a channel is opened to the
   *  consumer of the message.  The channel MUST be closed or an exception set or the
   *  message will never be retired from the system and result in a resource leak.
   *
   * @return a channel to send messages to the originator of the message associated with this context
   */
  ActiveInvokeChannel<R> openInvokeChannel();

  ActiveMessenger createInvokeMessenger();

  ActiveEntityManager createEntityManager();
  /**
   * Returns a map of client source information provided by the implementation.  Examples
   * might be the remote IP address of the source or the type of connection used to issue
   * the command.
   * @return a properties map with implementation provided information
   */
  default Properties getClientSourceProperties() {
      return new Properties();
  }

}
