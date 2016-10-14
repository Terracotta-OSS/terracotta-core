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
package com.tc.objectserver.entity;

import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

/**
 *
 */
public class MessagePayload {
  
  private final byte[] raw;
  private EntityMessage message;
  private final int concurrency;
  private final boolean requiresReplication;
  
  public static final MessagePayload EMPTY = new MessagePayload(new byte[0], null, false) {
    @Override
    public EntityMessage getEntityMessage() {
      throw new UnsupportedOperationException("empty payload");
    }
  };

  public MessagePayload(byte[] raw, EntityMessage message, int concurrency) {
    this.raw = raw;
    this.message = message;
    this.concurrency = concurrency;
    this.requiresReplication = false;
  }

  public MessagePayload(byte[] raw, EntityMessage message, boolean requiresReplication) {
    this.raw = raw;
    this.message = message;
    this.concurrency = ConcurrencyStrategy.MANAGEMENT_KEY;
    this.requiresReplication = requiresReplication;
  }
  
  public byte[] getRawPayload() {
    return raw;
  }
  
  public EntityMessage getEntityMessage() {
    return message;
  }
  
  public EntityMessage decodeRawMessage(MessageCodec codec) throws MessageCodecException {
    if (message == null) {
      message = codec.decodeMessage(raw);
    }
    return message;
  }
  
  public int getConcurrency() {
    return concurrency;
  }

  public boolean requiresReplication() {
    return requiresReplication;
  }
}
