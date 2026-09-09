/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.entity;


/**
 * A SyncMessageCodec is for converting high level Passive Sync Messages (currently we are using EntityMessage)
 * into byte[] and vice-versa
 *
 * Note that the implementation cannot assume when it will be called, relative to any other calls related to the entity, or
 * even that the calls will be made sequentially.  Multiple messages should be concurrently serialized or deserialized.
 * Also, encode/decode may not be called on the same object and invocations could be across jvms (for example, client and
 * server jvms on different machines). This means that an implementation is expected to be state-less and each returned
 * {@link EntityMessage} or byte[] must not be cached or reused.
 */
//TODO: Should we replace EntityMessage with a sync-specific message type and new invoke point on the entity?
public interface SyncMessageCodec<M extends EntityMessage> {

    /**
     * Encodes a given {@link EntityMessage} into a byte[] which can be passed over the wire
     *
     * @param concurrencyKey The concurrency key where the message must be run (so it can be described in the message type)
     * @param response The response object to be encoded
     * @return The encoded data which can be passed over the wire
     */
    byte[] encode(int concurrencyKey, M response);

    /**
     * Decodes a given message payload into a high-level {@link EntityMessage} type
     *
     * @param concurrencyKey The concurrency key where the message must be run (so it can be described in the message type)
     * @param payload The byte array containing the raw wire message
     * @return A high-level message instance
     */
    M decode(int concurrencyKey, byte[] payload);


}
