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
package com.tc.object.dna.impl;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public interface ObjectStringSerializer extends TCSerializable<ObjectStringSerializer> {

  String readString(TCDataInput in) throws IOException;

  String readFieldName(TCDataInput in) throws IOException;

  void writeString(TCDataOutput out, String string);

  void writeFieldName(TCDataOutput out, String fieldName);

  void writeStringBytes(TCDataOutput output, byte[] string);

  byte[] readStringBytes(TCDataInput input) throws IOException;

  int getApproximateBytesWritten();
}
