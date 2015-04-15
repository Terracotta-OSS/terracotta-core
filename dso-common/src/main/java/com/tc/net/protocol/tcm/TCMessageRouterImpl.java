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

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author orion
 */
public class TCMessageRouterImpl implements TCMessageRouter {
  private static final TCLogger logger       = TCLogging.getLogger(TCMessageRouter.class);
  private final Map<TCMessageType, TCMessageSink> routesByType = new ConcurrentHashMap<TCMessageType, TCMessageSink>();
  private final TCMessageSink   defaultRoute;

  public TCMessageRouterImpl() {
    this(null);
  }

  public TCMessageRouterImpl(TCMessageSink defRoute) {
    if (null == defRoute) {
      defaultRoute = new TCMessageSink() {
        @Override
        public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
          throw new UnsupportedMessageTypeException(message.getMessageType());
        }
      };
    } else {
      defaultRoute = defRoute;
    }
  }

  @Override
  public void putMessage(TCMessage msg) {
    final boolean debug = logger.isDebugEnabled();

    if (debug) logger.debug("Received a message: " + msg.toString());

    // try routing by message type
    final TCMessageSink route = routesByType.get(msg.getMessageType());

    if (route != null) {
      if (debug) logger.debug(msg.getMessageType().toString() + " message being routed by message type");
      route.putMessage(msg);
    } else {

      defaultRoute.putMessage(msg);
    }
  }

  @Override
  public void routeMessageType(TCMessageType type, TCMessageSink sink) {
    if (null == sink) { throw new IllegalArgumentException("Sink cannot be null"); }
    routesByType.put(type, sink);
  }

  @Override
  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
    routeMessageType(messageType, new TCMessageSinkToSedaSink(destSink, hydrateSink));
  }

  @Override
  public void unrouteMessageType(TCMessageType type) {
    routesByType.remove(type);
  }
}
