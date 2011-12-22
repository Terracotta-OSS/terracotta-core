/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public interface ObjectStringSerializer extends TCSerializable {

  String readString(TCDataInput in) throws IOException;

  String readFieldName(TCDataInput in) throws IOException;

  void writeString(TCDataOutput out, final String string);

  void writeFieldName(TCDataOutput out, final String fieldName);

  void writeStringBytes(TCDataOutput output, byte[] string);

  byte[] readStringBytes(TCDataInput input) throws IOException;

  int getApproximateBytesWritten();
}
