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
package com.tc.net.protocol.tcm;

import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.transport.ClientConnectionErrorListener;
import com.tc.net.protocol.transport.MessageTransportInitiator;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.object.ClientIDProvider;


public interface ClientMessageChannel extends MessageChannel, NetworkLayer, MessageTransportListener, ClientIDProvider, ClientConnectionErrorListener {

  public int getConnectCount();

  public int getConnectAttemptCount();

  public void setMessageTransportInitiator(MessageTransportInitiator initiator);

  public void addClientConnectionErrorListener(ClientConnectionErrorListener errorListener);

  public void removeClientConnectionErrorListener(ClientConnectionErrorListener errorListener);
}
