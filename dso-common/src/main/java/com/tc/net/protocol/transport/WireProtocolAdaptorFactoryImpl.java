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
package com.tc.net.protocol.transport;

import com.tc.async.api.Sink;
import com.tc.net.protocol.ProtocolSwitch;
import com.tc.net.protocol.TCProtocolAdaptor;

public class WireProtocolAdaptorFactoryImpl implements WireProtocolAdaptorFactory {

  private final Sink httpSink;

  // This version is for the server and will use the HTTP protocol switcher thingy
  public WireProtocolAdaptorFactoryImpl(Sink httpSink) {
    this.httpSink = httpSink;
  }

  public WireProtocolAdaptorFactoryImpl() {
    this(null);
  }

  @Override
  public TCProtocolAdaptor newWireProtocolAdaptor(WireProtocolMessageSink sink) {
    if (httpSink != null) { return new ProtocolSwitch(new WireProtocolAdaptorImpl(sink), httpSink); }
    return new WireProtocolAdaptorImpl(sink);
  }
}
