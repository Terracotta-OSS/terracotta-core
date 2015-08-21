package com.tc.net.protocol.transport;

import java.io.IOException;

/**
 * @author tim
 */
public class TransportHandshakeException extends IOException {
  TransportHandshakeException(String message) {
    super(message);
  }
}
