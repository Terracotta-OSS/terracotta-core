/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
  private final static TCMessageType[] values = values();

  TCMessageType(boolean valid) {
    validType = valid;
  }

  public static TCMessageType getInstance(int i) {
    return values[i];
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
