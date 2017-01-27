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
import org.terracotta.entity.MessageCodecException;


public class MessagePayload {
  public static final MessagePayload emptyPayload() {
    return new MessagePayload(new byte[0], null, ConcurrencyStrategy.MANAGEMENT_KEY, 0, true, true);
  }

  public static final MessagePayload rawDataOnly(byte[] raw) {
    return new MessagePayload(raw, null, ConcurrencyStrategy.MANAGEMENT_KEY, 0, false, false);
  }

  public static final MessagePayload commonMessagePayloadBusy(byte[] raw, EntityMessage message, boolean replicate) {
    return new MessagePayload(raw, message, ConcurrencyStrategy.MANAGEMENT_KEY, 0, replicate, true);
  }

  public static final MessagePayload commonMessagePayloadNotBusy(byte[] raw, EntityMessage message, boolean replicate) {
    return new MessagePayload(raw, message, ConcurrencyStrategy.MANAGEMENT_KEY, 0, replicate, false);
  }

  public static final MessagePayload syncPayloadNormal(byte[] raw, int concurrencyKey) {
    return new MessagePayload(raw, null, concurrencyKey, 0, false, false);
  }

  public static final MessagePayload syncPayloadCreation(byte[] raw, int referenceCount) {
    return new MessagePayload(raw, null, 0, referenceCount, false, false);
  }


  private final byte[] raw;
  private EntityMessage message;
  private final int concurrency;
  private final int referenceCount;
  private final boolean replicate;
  private final boolean canBeBusy;
  private String debugId;
  
  // NOTE:  ReferenceCount is a special-case for synchronizing the creation of an existing entity.
  private MessagePayload(byte[] raw, EntityMessage message, int concurrency, int referenceCount, boolean replicate, boolean canBeBusy) {
    this.raw = raw;
    this.message = message;
    this.debugId = (message != null) ? message.toString() : "";
    this.concurrency = concurrency;
    this.referenceCount = referenceCount;
    this.replicate = replicate;
    this.canBeBusy = canBeBusy;
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
  
  public boolean canBeBusy() {
    return canBeBusy;
  }
  
  public EntityMessage decodeRawMessage(MessageDecoder codec) {
    try {
      return decodeMessage(codec);
    } catch (MessageCodecException mce) {
      throw new RuntimeException(mce);
    }
  }

  public EntityMessage decodeMessage(MessageDecoder codec) throws MessageCodecException {
    if (message == null) {
      message = codec.decode(raw);
      setDebugId(message.toString());
    }
    return message;
  }
  
  public int getConcurrency() {
    return concurrency;
  }
  
  public int getReferenceCount() {
    return this.referenceCount;
  }
  
  public boolean shouldReplicate() {
    return replicate;
  }

  @Override
  public String toString() {
    return "MessagePayload{" + "debugId=" + debugId + '}';
  }
}
