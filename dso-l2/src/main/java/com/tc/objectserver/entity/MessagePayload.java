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
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

/**
 *
 */
public class MessagePayload {
  
  private final byte[] raw;
  private EntityMessage message;
  private final int concurrency;
  private final boolean replicate;
  private String debugId;
  
  public static final MessagePayload EMPTY = new MessagePayload(new byte[0], null, true);
  
  public MessagePayload(byte[] raw, EntityMessage message, boolean replicate) {
    this(raw, message, ConcurrencyStrategy.MANAGEMENT_KEY, replicate);
  }
  
  public MessagePayload(byte[] raw, EntityMessage message, int concurrency) {
    this(raw, message, concurrency, false);
  }

  private MessagePayload(byte[] raw, EntityMessage message, int concurrency, boolean replicate) {
    this.raw = raw;
    this.message = message;
    this.debugId = (message != null) ? message.toString() : "";
    this.concurrency = concurrency;
    this.replicate = replicate;
  }
  
  public byte[] getRawPayload() {
    return raw;
  }
  
  public void setDebugId(String debugId) {
    this.debugId = debugId;
  }
  
  public String getDebugId() {
    return debugId;
  }
  
  public EntityMessage decodeRawMessage(MessageCodec codec) {
    try {
      return decodeMessage(codec);
    } catch (MessageCodecException mce) {
      throw new RuntimeException(mce);
    }
  }

  public EntityMessage decodeMessage(MessageCodec codec) throws MessageCodecException {
    if (message == null) {
      message = codec.decodeMessage(raw);
      setDebugId(message.toString());
    }
    return message;
  }
  
  public int getConcurrency() {
    return concurrency;
  }
  
  public boolean shouldReplicate() {
    return replicate;
  }
}
