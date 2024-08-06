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
package com.tc.net.core;

import java.nio.channels.SelectionKey;

/**
 * Constants common to the com.tc.net.core package
 * 
 * @author teck
 */
public final class Constants {
  public static final int DEFAULT_ACCEPT_QUEUE_DEPTH = 512;

  public static String interestOpsToString(int interestOps) {
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
