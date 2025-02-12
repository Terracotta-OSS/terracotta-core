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
package com.tc.objectserver.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodecException;

public class MessagePayload {
  public static final MessagePayload emptyPayload() {
    MessagePayload payload = new MessagePayload(TCByteBufferFactory.getInstance(0), null, ConcurrencyStrategy.MANAGEMENT_KEY, 0, true, true);
    payload.setDebugId("EMPTY");
    return payload;
  }

  public static final MessagePayload rawDataOnly(TCByteBuffer raw) {
    MessagePayload payload = new MessagePayload(raw, null, ConcurrencyStrategy.MANAGEMENT_KEY, 0, false, false);
    payload.setDebugId("RAW");
    return payload;
  }

  public static final MessagePayload commonMessagePayload(TCByteBuffer raw, EntityMessage message, boolean replicate, boolean allowBusy) {
    return new MessagePayload(raw, message, ConcurrencyStrategy.MANAGEMENT_KEY, 0, replicate, allowBusy);
  }

  public static final MessagePayload commonMessagePayloadBusy(TCByteBuffer raw, EntityMessage message, boolean replicate) {
    return new MessagePayload(raw, message, ConcurrencyStrategy.MANAGEMENT_KEY, 0, replicate, true);
  }

  public static final MessagePayload commonMessagePayloadNotBusy(TCByteBuffer raw, EntityMessage message, boolean replicate) {
    return new MessagePayload(raw, message, ConcurrencyStrategy.MANAGEMENT_KEY, 0, replicate, false);
  }

  public static final MessagePayload syncPayloadNormal(TCByteBuffer raw, int concurrencyKey) {
    return new MessagePayload(raw, null, concurrencyKey, 0, false, false);
  }

  public static final MessagePayload syncPayloadCreation(TCByteBuffer raw, int referenceCount) {
    return new MessagePayload(raw, null, 0, referenceCount, false, false);
  }


  private final TCByteBuffer raw;
  private EntityMessage message;
  private MessageCodecException exception;
  private final int concurrency;
  private final int referenceCount;
  private final boolean replicate;
  private final boolean canBeBusy;
  private String debugId;
  
  // NOTE:  ReferenceCount is a special-case for synchronizing the creation of an existing entity.
  private MessagePayload(TCByteBuffer raw, EntityMessage message, int concurrency, int referenceCount, boolean replicate, boolean canBeBusy) {
    this.raw = raw == null || raw.isReadOnly() ? raw : raw.asReadOnlyBuffer();
    this.message = message;
    this.debugId = null;
    this.concurrency = concurrency;
    this.referenceCount = referenceCount;
    this.replicate = replicate;
    this.canBeBusy = canBeBusy;
  }
  
  private byte[] convertRawToBytes() {
    return TCByteBufferFactory.unwrap(raw);
  }
  
  public byte[] getRawPayload() {
    return convertRawToBytes();
  }
  
  public TCByteBuffer getByteBufferPayload() {
    return this.raw.duplicate();
  }
  
  public void setDebugId(String debugId) {
    this.debugId = debugId;
  }
  
  public String getDebugId() {
    if (debugId == null && this.message != null) {
      debugId = message.toString();
    }
    return debugId;
  }
  
  public boolean canBeBusy() {
    return canBeBusy;
  }

  public EntityMessage decodeMessage(MessageDecoder codec) throws MessageCodecException {
    if (exception != null) {
      throw exception;
    }
    try {
      if (message == null) {
        message = codec.decode(convertRawToBytes());
      }
      return message;
    } catch (MessageCodecException ce) {
      exception = ce;
      throw exception;
    } catch (Exception e) {
      exception = new MessageCodecException("error decoding message", e);
      throw exception;
    }
  }
  
  public int getConcurrency() {
    return concurrency;
  }
  
  public int getReferenceCount() {
    return this.referenceCount;
  }
  
  public Class<?> getType() {
    return (message != null) ? message.getClass() : null;
  }
  
  public boolean shouldReplicate() {
    return replicate;
  }
  
  @Override
  public String toString() {
    return "MessagePayload{" + "debugId=" + getDebugId() + '}';
  }
}
