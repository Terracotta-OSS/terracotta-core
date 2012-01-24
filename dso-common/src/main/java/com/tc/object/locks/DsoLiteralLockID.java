/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.LiteralValues;
import com.tc.object.bytecode.Manager;
import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.EnumInstance;
import com.tc.object.dna.impl.UTF8ByteDataHolder;

import java.io.IOException;

/**
 * Represents the a lock on a clustered literal object.
 * <p>
 * Literal locks in Terracotta are special as they locks on the value of the literal object and not on its object
 * identity - as literal objects have no cluster wide object identity.
 */
public class DsoLiteralLockID implements LockID {
  private static final long serialVersionUID = 0x173295fec628dca3L;

  private Object            literal;

  public DsoLiteralLockID() {
    // please tc serialization
  }

  public DsoLiteralLockID(Manager mgr, Object literal) throws IllegalArgumentException {
    this.literal = translateLiteral(mgr, literal);
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
      case INTEGER:
        literal = Integer.valueOf(serialInput.readInt());
        return this;
      case LONG:
        literal = Long.valueOf(serialInput.readLong());
        return this;
      case CHARACTER:
        literal = Character.valueOf(serialInput.readChar());
        return this;
      case FLOAT:
        literal = Float.valueOf(serialInput.readFloat());
        return this;
      case DOUBLE:
        literal = Double.valueOf(serialInput.readDouble());
        return this;
      case BYTE:
        literal = Byte.valueOf(serialInput.readByte());
        return this;
      case BOOLEAN:
        literal = Boolean.valueOf(serialInput.readBoolean());
        return this;
      case SHORT:
        literal = Short.valueOf(serialInput.readShort());
        return this;
      case STRING:
        throw new AssertionError("String literal types should be handled by StringLockID");
      case ENUM_HOLDER:
        String className = serialInput.readString();
        ClassInstance classInstance = new ClassInstance(new UTF8ByteDataHolder(className));
        String enumName = serialInput.readString();
        literal = new EnumInstance(classInstance, new UTF8ByteDataHolder(enumName));
        return this;
      case STRING_BYTES:
      case JAVA_LANG_CLASS_HOLDER:
      case STRING_BYTES_COMPRESSED:
        throw new AssertionError("Unusual type found in serialized DsoLiteralLockID stream " + type);
      case OBJECT:
      case OBJECT_ID:
      case JAVA_LANG_CLASS:
      case ARRAY:
      case ENUM:
        throw new AssertionError("Illegal type found in serialized DsoLiteralLockID stream " + type);
    }
    throw new AssertionError("Null type found in serialized DsoLiteralLockID stream " + type);
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    LiteralValues type = LiteralValues.valueFor(literal);
    serialOutput.writeByte(type.ordinal());
    switch (type) {
      case INTEGER:
        serialOutput.writeInt(((Integer) literal).intValue());
        break;
      case LONG:
        serialOutput.writeLong(((Long) literal).longValue());
        break;
      case CHARACTER:
        serialOutput.writeChar(((Character) literal).charValue());
        break;
      case FLOAT:
        serialOutput.writeFloat(((Float) literal).floatValue());
        break;
      case DOUBLE:
        serialOutput.writeDouble(((Double) literal).doubleValue());
        break;
      case BYTE:
        serialOutput.writeByte(((Byte) literal).byteValue());
        break;
      case BOOLEAN:
        serialOutput.writeBoolean(((Boolean) literal).booleanValue());
        break;
      case SHORT:
        serialOutput.writeShort(((Short) literal).shortValue());
        break;
      case STRING:
        throw new AssertionError("String literal types should be handled by StringLockID");
      case ENUM_HOLDER:
        EnumInstance enumInstance = (EnumInstance) literal;
        serialOutput.writeString(enumInstance.getClassInstance().getName().asString());
        serialOutput.writeString(((UTF8ByteDataHolder) enumInstance.getEnumName()).asString());
        break;
      case STRING_BYTES:
      case JAVA_LANG_CLASS_HOLDER:
      case STRING_BYTES_COMPRESSED:
        throw new AssertionError("Unusual type passed to DsoLiteralLockID constructor " + type);
      case OBJECT:
      case OBJECT_ID:
      case JAVA_LANG_CLASS:
      case ARRAY:
      case ENUM:
        throw new AssertionError("Illegal type passed to DsoLiteralLockID constructor " + type);
    }
  }

  @Override
  public int hashCode() {
    return LiteralValues.calculateDsoHashCode(literal);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLiteralLockID) {
      return literal.equals(((DsoLiteralLockID) o).literal);
    } else {
      return false;
    }
  }

  public int compareTo(Object o) {
    throw new ClassCastException("DsoLiteralLockID instances can't be compared");
  }

  private static Object translateLiteral(Manager mgr, Object literal) throws IllegalArgumentException {
    LiteralValues type = LiteralValues.valueFor(literal);
    switch (type) {
      case ENUM:
        Class clazz = literal.getClass();
        ClassInstance classInstance = new ClassInstance(new UTF8ByteDataHolder(clazz.getName()));
        return new EnumInstance(classInstance, new UTF8ByteDataHolder(((Enum) literal).name()));
      default:
        return literal;
    }
  }

}
