/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.LiteralValues;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents the a lock on a clustered literal object.
 * <p>
 * Literal locks in Terracotta are special as they locks on the value of the
 * literal object and not on its object identity - as literal objects have no
 * cluster wide object identity.
 */
public class DsoLiteralLockID implements LockID {
  private static final long serialVersionUID = 0x173295fec628dca3L;

  private Object literal;
  
  public DsoLiteralLockID() {
    // please tc serialization
  }
  
  public DsoLiteralLockID(Object literal) {
    this.literal = literal;
  }
  
  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    return LockIDType.DSO_LITERAL;
  }
  
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    LiteralValues type = LiteralValues.values()[serialInput.readByte()];
    switch (type) {
      case BIG_DECIMAL:
        literal = new BigDecimal(serialInput.readString());
        return this;
      case BIG_INTEGER:
        int length = serialInput.readInt();
        byte[] data = new byte[length];
        serialInput.readFully(data);
        literal = new BigInteger(data);
        return this;
      case INTEGER:
        literal = Integer.valueOf(serialInput.readInt());
        return this;
      default:
        throw new AssertionError();
    }
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    LiteralValues type = LiteralValues.valueFor(literal);
    serialOutput.writeByte(type.ordinal());
    switch (type) {
      case BIG_DECIMAL:
        serialOutput.writeString(((BigDecimal) literal).toString());
        break;
      case BIG_INTEGER:
        byte[] data = ((BigInteger) literal).toByteArray();
        serialOutput.writeInt(data.length);
        serialOutput.write(data);
        break;
      case INTEGER:
        serialOutput.writeInt(((Integer) literal).intValue());
        break;
      default:
        throw new AssertionError();
    }
  }
  
  public int hashCode() {
    return LiteralValues.calculateDsoHashCode(literal);
  }
  
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLiteralLockID) {
      return literal.equals(((DsoLiteralLockID) o).literal);
    } else {
      return false;
    }
  }
}
