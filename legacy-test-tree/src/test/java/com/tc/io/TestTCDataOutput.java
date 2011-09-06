/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io;

import java.io.IOException;

public final class TestTCDataOutput implements TCDataOutput {

  public final TCDataOutput out;

  public TestTCDataOutput(TCDataOutput out) {
    this.out = out;
  }

  public void close() {
    return;
  }

  public void write(int value) {
    out.write(value);
  }

  public void writeBoolean(boolean value) {
    out.writeBoolean(value);
  }

  public void writeByte(int value) {
    out.writeByte(value);
  }

  public void writeChar(int value) {
    out.writeChar(value);
  }

  public void writeDouble(double value) {
    out.writeDouble(value);
  }

  public void writeFloat(float value) {
    out.writeFloat(value);
  }

  public void writeInt(int value) {
    out.writeInt(value);
  }

  public void writeLong(long value) {
    out.writeLong(value);
  }

  public void writeShort(int value) {
    out.writeShort(value);
  }

  public void write(byte[] b) {
    out.write(b);
  }

  public void write(byte[] b, int off, int len) {
    out.write(b, off, len);
  }

  public void writeString(String string) {
    out.writeString(string);
  }

  public void writeBytes(String s) throws IOException {
    out.writeBytes(s);
  }

  public void writeChars(String s) throws IOException {
    out.writeChars(s);
  }

  public void writeUTF(String str) throws IOException {
    out.writeUTF(str);
  }

}