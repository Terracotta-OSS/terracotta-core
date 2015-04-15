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
package com.tc.server;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.HttpConnectionContext;

import java.net.Socket;

public class HttpConnectionHandler extends AbstractEventHandler {

  private static final TCLogger     logger = TCLogging.getLogger(HttpConnectionContext.class);

  private final TerracottaConnector terracottaConnector;

  public HttpConnectionHandler(TerracottaConnector terracottaConnector) {
    this.terracottaConnector = terracottaConnector;
    //
  }

  @Override
  public void handleEvent(EventContext context) {
    HttpConnectionContext connContext = (HttpConnectionContext) context;

    Socket s = connContext.getSocket();
    TCByteBuffer buffer = connContext.getBuffer();
    byte[] data = new byte[buffer.limit()];
    buffer.get(data);
    try {
      terracottaConnector.handleSocketFromDSO(s, data);
    } catch (Exception e) {
      logger.error(e);
    }
  }

}
