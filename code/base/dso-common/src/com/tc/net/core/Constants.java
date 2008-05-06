/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import java.nio.channels.SelectionKey;

/**
 * Constants common to the com.tc.net.core package
 * 
 * @author teck
 */
public final class Constants {
  public static final int DEFAULT_ACCEPT_QUEUE_DEPTH = 512;

  public static String interestOpsToString(final int interestOps) {
    StringBuffer buf = new StringBuffer();
    if ((interestOps & SelectionKey.OP_ACCEPT) != 0) {
      buf.append(" ACCEPT");
    }

    if ((interestOps & SelectionKey.OP_CONNECT) != 0) {
      buf.append(" CONNECT");
    }

    if ((interestOps & SelectionKey.OP_READ) != 0) {
      buf.append(" READ");
    }

    if ((interestOps & SelectionKey.OP_WRITE) != 0) {
      buf.append(" WRITE");
    }
    return buf.toString();
  }

}