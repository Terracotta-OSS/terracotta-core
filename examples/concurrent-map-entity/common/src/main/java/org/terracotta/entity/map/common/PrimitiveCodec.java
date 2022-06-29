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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PrimitiveCodec {

  public static byte[] encode(Object o) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream output = new ObjectOutputStream(bytes);
    writeTo(output, o);
    output.close();
    return bytes.toByteArray();
  }

  public static Object decode(byte[] bytes) throws IOException {
    ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes));
    return readFrom(input);
  }

  public static Object readFrom(DataInput inputStream) throws IOException {
    try {
      return ((ObjectInputStream) inputStream).readObject();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeTo(DataOutput os, Object o) throws IOException {
    ((ObjectOutputStream) os).writeObject(o);
  }
}
