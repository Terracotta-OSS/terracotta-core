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
package com.tc.objectserver.testentity;

import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;


public class TestEntityServerService implements EntityServerService<EntityMessage, EntityResponse> {
  @Override
  public long getVersion() {
    return 1L;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return "com.tc.objectserver.testentity.TestEntity".equals(typeName);
  }

  @Override
  public TestEntityServer createActiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new TestEntityServer();
  }

  @Override
  public PassiveServerEntity<EntityMessage, EntityResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConcurrencyStrategy<EntityMessage> getConcurrencyStrategy(byte[] configuration) {
    return new NoConcurrencyStrategy<EntityMessage>();
  }

  @Override
  public MessageCodec<EntityMessage, EntityResponse> getMessageCodec() {
    // We need to return a non-null codec but we have no notion of what to do with the messages (since we don't use real types).
    return new MessageCodec<EntityMessage, EntityResponse>() {
      @Override
      public byte[] encodeMessage(EntityMessage message) throws MessageCodecException {
        return new byte[((TestElement)message).length];
      }
      @Override
      public EntityMessage decodeMessage(byte[] payload) throws MessageCodecException {
        return new TestElement(payload.length);
      }
      @Override
      public byte[] encodeResponse(EntityResponse response) throws MessageCodecException {
        // NOTE:  We always return null so just return an empty array.
        return new byte[0];
      }
      @Override
      public EntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
        return new TestElement(payload.length);
      }};
  }
  private static class TestElement implements EntityMessage, EntityResponse {
    public final int length;
    public TestElement(int length) {
      this.length = length;
    }
  }

  @Override
  public SyncMessageCodec<EntityMessage> getSyncMessageCodec() {
    return new SyncMessageCodec<EntityMessage>() {
      @Override
      public byte[] encode(int concurrencyKey, EntityMessage message) throws MessageCodecException {
        throw new UnsupportedOperationException("Not supported");
      }

      @Override
      public EntityMessage decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        throw new UnsupportedOperationException("Not supported");
      }
    };
  }
}