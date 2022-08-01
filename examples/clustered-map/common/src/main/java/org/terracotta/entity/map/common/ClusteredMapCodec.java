/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.entity.map.common;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.io.IOException;


public class ClusteredMapCodec implements MessageCodec<MapOperation, MapResponse> {

  @Override
  public byte[] encodeMessage(MapOperation message) throws MessageCodecException {
    try {
      return OperationCodec.encode(message);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }

  @Override
  public MapOperation decodeMessage(byte[] payload) throws MessageCodecException {
    try {
      return OperationCodec.decode(payload);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }

  @Override
  public byte[] encodeResponse(MapResponse response) throws MessageCodecException {
    try {
      return ResponseCodec.encode(response);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }

  @Override
  public MapResponse decodeResponse(byte[] payload) throws MessageCodecException {
    try {
      return ResponseCodec.decode(payload);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }
}
