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
package com.tc.net.protocol.tcm;

import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.transport.MessageTransportListener;

/**
 * The internal (comms-side) interface to the message channel. It acts like the bottom half of a NetworkLayer in that it
 * sends and receives messages -- but there's not a proper NetworkLayer above it to pass messages up to. It needs to be
 * a MessageTransportListener since in some stack configurations, it needs to respond to transport events
 * 
 * @author teck
 */
public interface MessageChannelInternal extends NetworkLayer, MessageChannel, MessageTransportListener {
  //
}
