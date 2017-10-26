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
package com.tc.net.protocol.tcm;

/**
 * Define all the various TC Message type numbers.
 */

public enum TCMessageType {
    ZERO_MESSAGE(false),  
    PING_MESSAGE(true),
    CLIENT_HANDSHAKE_MESSAGE(true),
    CLIENT_HANDSHAKE_ACK_MESSAGE(true),
    CLIENT_HANDSHAKE_REDIRECT_MESSAGE(true),
    UNUSED_MESSAGE(false),
    CLUSTER_MEMBERSHIP_EVENT_MESSAGE(true),
    GROUP_WRAPPER_MESSAGE(true),
    GROUP_HANDSHAKE_MESSAGE(true),
    CLIENT_HANDSHAKE_REFUSED_MESSAGE(true),
    UNUSED2_MESSAGE(false),
    UNUSED3_MESSAGE(false),
    UNUSED4_MESSAGE(false),
    UNUSED5_MESSAGE(false),
    VOLTRON_ENTITY_RECEIVED_RESPONSE(true),
    VOLTRON_ENTITY_MESSAGE(true),
    VOLTRON_ENTITY_COMPLETED_RESPONSE(true),
    VOLTRON_ENTITY_RETIRED_RESPONSE(true),
    VOLTRON_ENTITY_MULTI_RESPONSE(true),
    NOOP_MESSAGE(true),
    DIAGNOSTIC_REQUEST(true),
    DIAGNOSTIC_RESPONSE(true);
    
  private final boolean validType;

  TCMessageType(boolean valid) {
    validType = valid;
  }

  public static TCMessageType getInstance(int i) {
    return values()[i];
  }

  public int getType() {
    return ordinal();
  }

  public String getTypeName() {
    return toString();
  }
  
  public boolean isValid() {
    return this.validType;
  }
}
