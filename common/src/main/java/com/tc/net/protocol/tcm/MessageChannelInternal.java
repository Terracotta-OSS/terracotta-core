/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
