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
package org.terracotta.entity.map.server;


import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.entity.map.common.MapOperation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;


class ClusteredMapSyncCodec implements SyncMessageCodec<MapOperation> {
    @Override
    public byte[] encode(int concurrencyKey, MapOperation message) throws MessageCodecException {
        // Note that we only expect that a sync will operate on SYNC operations.
        if (MapOperation.Type.SYNC_OP != message.operationType()) {
            throw new AssertionError("Invalid message type for SYNC_OP: " + message.operationType());
        }

        SyncOperation operation = (SyncOperation)message;
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(raw);
            Map<Object, Object> elements = operation.getObjectMap();
            for (Map.Entry<Object, Object> next : elements.entrySet()) {
                out.writeObject(next.getKey());
                out.writeObject(next.getValue());
            }
            out.close();
            byte[] bytes = raw.toByteArray();
            return bytes;
        } catch (IOException ioe) {
            throw new MessageCodecException("io error", ioe);
        }
    }

    @Override
    public MapOperation decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        try {
            InputStream is = new ByteArrayInputStream(payload);
            ObjectInputStream dis = new ObjectInputStream(is);
            Map<Object, Object> map = new HashMap<Object, Object>();
            Object key = readKey(dis);
            while (null != key) {
              Object value = dis.readObject();
              map.put(key, value);
              key = readKey(dis);
            }
            return new SyncOperation(map);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
    }
    
    private Object readKey(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        Object key = null;
        try {
            key = objectInputStream.readObject();
        } catch (EOFException e) {
            // This means that we have reached the last key-value pair.
        }
        return key;
    }
}
