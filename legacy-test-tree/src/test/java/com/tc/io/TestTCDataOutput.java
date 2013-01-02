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

  @Override
  public void close() {
    return;
  }

  @Override
  public void write(int value) {
    out.write(value);
  }

  @Override
  public void writeBoolean(boolean value) {
    out.writeBoolean(value);
  }

  @Override
  public void writeByte(int value) {
    out.writeByte(value);
  }

  @Override
  public void writeChar(int value) {
    out.writeChar(value);
  }

  @Override
  public void writeDouble(double value) {
    out.writeDouble(value);
  }

  @Override
  public void writeFloat(float value) {
    out.writeFloat(value);
  }

  @Override
  public void writeInt(int value) {
    out.writeInt(value);
  }

  @Override
  public void writeLong(long value) {
    out.writeLong(value);
  }

  @Override
  public void writeShort(int value) {
    out.writeShort(value);
  }

  @Override
  public void write(byte[] b) {
    out.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    out.write(b, off, len);
  }

  @Override
  public void writeString(String string) {
    out.writeString(string);
  }

  @Override
  public void writeBytes(String s) throws IOException {
    out.writeBytes(s);
  }

  @Override
  public void writeChars(String s) throws IOException {
    out.writeChars(s);
  }

  @Override
  public void writeUTF(String str) throws IOException {
    out.writeUTF(str);
  }

}