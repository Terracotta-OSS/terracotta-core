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
