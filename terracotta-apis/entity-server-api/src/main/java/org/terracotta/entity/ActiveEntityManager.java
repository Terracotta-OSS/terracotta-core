/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
 *
 * @author
 */
public interface ActiveEntityManager extends AutoCloseable {
  /**
   * Sends a message to create an unrelated entity.
   * @param type string representation of the type of entity to be created
   * @param name name of the entity
   * @param version version of the entity
   * @param configuration configuration of the entity
   */
  void create(String type, String name, long version, byte[] configuration);
  /**
   * Sends a message to delete the entity.  This method should be called from the
   * {@link org.terracotta.entity.ActiveServerEntity#disconnected(org.terracotta.entity.ClientDescriptor) disconnected}
   * body or it will fail due to client references still on the entity.
   */
  void destroySelf();
  /**
   * Sends a message to reconfigure the entity.  This method should be called from the
   * {@link org.terracotta.entity.ActiveServerEntity#disconnected(org.terracotta.entity.ClientDescriptor) disconnected}
   * body or it will fail due to client references still on the entity.
   */
  void reconfigureSelf(byte[] configuration);
  /**
   * closes the instance.
   */
  @Override
  void close();
}
