/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package org.terracotta.entity.map.common;

import org.terracotta.entity.MessageCodec;

import java.io.IOException;
import java.io.UncheckedIOException;


public class ClusteredMapCodec implements MessageCodec<MapOperation, MapResponse> {

  @Override
  public byte[] encodeMessage(MapOperation message) {
    try {
      return OperationCodec.encode(message);
    } catch (IOException e) {
      throw new UncheckedIOException("something wrong happend", e);
    }
  }

  @Override
  public MapOperation decodeMessage(byte[] payload) {
    try {
      return OperationCodec.decode(payload);
    } catch (IOException e) {
      throw new UncheckedIOException("something wrong happend", e);
    }
  }

  @Override
  public byte[] encodeResponse(MapResponse response) {
    try {
      return ResponseCodec.encode(response);
    } catch (IOException e) {
      throw new UncheckedIOException("something wrong happend", e);
    }
  }

  @Override
  public MapResponse decodeResponse(byte[] payload) {
    try {
      return ResponseCodec.decode(payload);
    } catch (IOException e) {
      throw new UncheckedIOException("something wrong happend", e);
    }
  }
}
