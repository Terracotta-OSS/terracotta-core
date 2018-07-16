/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.core.TCConnection;
import com.tc.util.Assert;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Inspects the first few bytes from a socket to decide if it should stay in the TC comms stack, or if it should be
 * handed off to Jetty. Evolving this switch behaviour to more protocols should probably involve refactoring the generic
 * comms code, not just this class
 */
public class ProtocolSwitch implements TCProtocolAdaptor {

  private static final String[]    HTTP_METHODS      = new String[] { "GET", "POST", "HEAD", "PUT", "OPTIONS",
      "DELETE", "TRACE", "CONNECT"                  };

  private static final Set<String> METHODS           = new HashSet<String>(Arrays.asList(HTTP_METHODS));

  // The longest HTTP method is 7 chars, +1 for the space
  private static final int         INSPECT           = 8;

  private static final int         PROTOCOL_UNKNOWN  = 0;
  private static final int         PROTOCOL_NOT_HTTP = 1;
  private static final int         PROTOCOL_HTTP     = 2;

  private volatile int             protocol          = PROTOCOL_UNKNOWN;
  private final TCByteBuffer[]     buffer            = new TCByteBuffer[] { TCByteBufferFactory.wrap(new byte[INSPECT]) };
  private final TCProtocolAdaptor  delegate;
  private final Sink               httpSink;

  public ProtocolSwitch(TCProtocolAdaptor delegate, Sink httpSink) {
    this.delegate = delegate;
    this.httpSink = httpSink;
  }

  @SuppressWarnings("resource")
  @Override
  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    switch (protocol) {
      case PROTOCOL_NOT_HTTP: {
        delegate.addReadData(source, data, length);
        return;
      }
      case PROTOCOL_UNKNOWN: {
        Assert.assertEquals(1, data.length);
        TCByteBuffer buf = data[0];
        if (buf.hasRemaining()) {
          // didn't get enough bytes yet to make a decision
          return;
        }

        buf.flip();
        boolean isHttp = isHttp(buf);
        buf.rewind();

        if (isHttp) {
          protocol = PROTOCOL_HTTP;
          final Socket socket;
          try {
            socket = source.detach();
          } catch (IOException e) {
            throw new TCProtocolException(e);
          }
          httpSink.addToSink(new HttpConnectionContext(socket, buf));
          return;
        } else {
          protocol = PROTOCOL_NOT_HTTP;
          feedDataToDelegate(source, buf);
          return;
        }
      }
      default:
        throw new AssertionError("Protocol is " + protocol);
    }

    // unreachable
  }

  private void feedDataToDelegate(TCConnection source, TCByteBuffer src) throws TCProtocolException {
    while (src.hasRemaining()) {
      int count = 0;

      TCByteBuffer[] readBuffers = delegate.getReadBuffers();
      for (TCByteBuffer dest : readBuffers) {
        int len = Math.min(src.remaining(), dest.remaining());
        count += len;
        for (int j = 0; j < len; j++) {
          dest.put(src.get());
        }
        if (!src.hasRemaining()) {
          break;
        }
      }

      delegate.addReadData(source, readBuffers, count);
    }
  }

  private static boolean isHttp(TCByteBuffer buf) {
    Assert.assertEquals(INSPECT, buf.limit());
    byte[] bytes = new byte[buf.limit()];
    buf.get(bytes);

    final String s;
    try {
      s = new String(bytes);
    } catch (Exception e) {
      return false;
    }

    int spaceIndex = s.indexOf(' ');
    if (spaceIndex < 0) { return false; }

    String token = s.substring(0, spaceIndex);

    // best I can tell, HTTP methods are case sensitive, so I'm not uppercase'ing the token here
    return METHODS.contains(token);
  }

  @Override
  public TCByteBuffer[] getReadBuffers() {
    switch (protocol) {
      case PROTOCOL_NOT_HTTP:
        return delegate.getReadBuffers();
      case PROTOCOL_UNKNOWN:
        return buffer;
      default:
        throw new IllegalStateException("Unexpected protocol: " + protocol);
    }

    // unreachable
  }
}
