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

import java.util.function.Consumer;

public interface ActiveMessenger extends AutoCloseable {

  void sendMessage(EntityMessage message);

  void sendMessage(EntityMessage message, Consumer<EntityResponse> result, Consumer<Exception> failure);

  ReleaseHandle deferRetirement(String tag, EntityMessage message);

  ReleaseHandle deferRetirement(String tag, EntityMessage message, Consumer<EntityResponse> result, Consumer<Exception> failure);

  interface ReleaseHandle {
    String tag();
    void release();
  }
  @Override
  void close();
}
