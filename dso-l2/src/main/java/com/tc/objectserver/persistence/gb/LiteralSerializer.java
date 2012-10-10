package com.tc.objectserver.persistence.gb;

import org.terracotta.corestorage.Serializer;

import com.tc.object.ObjectID;
import com.tc.object.dna.impl.UTF8ByteDataHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tim
 */
public class LiteralSerializer implements Serializer<Object> {
  public static final LiteralSerializer INSTANCE = new LiteralSerializer();

  private static enum Type {
    LONG {
      @Override
      public Long deserialize(final ObjectInputStream objectInputStream) throws IOException {
        return objectInputStream.readLong();
      }

      @Override
      void serialize(final ObjectOutputStream objectOutputStream, final Object object) throws IOException {
        if (object instanceof Long) {
          objectOutputStream.writeLong((Long)object);
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Long.class;
      }
    }, INT {
      @Override
      public Integer deserialize(final ObjectInputStream objectInputStream) throws IOException {
        return objectInputStream.readInt();
      }

      @Override
      void serialize(final ObjectOutputStream objectOutputStream, final Object object) throws IOException {
        if (object instanceof Integer) {
          objectOutputStream.writeInt((Integer)object);
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Integer.class;
      }
    }, SHORT {
      @Override
      public Short deserialize(final ObjectInputStream objectInputStream) throws IOException {
        return objectInputStream.readShort();
      }

      @Override
      void serialize(final ObjectOutputStream objectOutputStream, final Object object) throws IOException {
        if (object instanceof Short) {
          objectOutputStream.writeShort((Short)object);
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Short.class;
      }
    }, BYTE {
      @Override
      public Byte deserialize(final ObjectInputStream objectInputStream) throws IOException {
        return objectInputStream.readByte();
      }

      @Override
      void serialize(final ObjectOutputStream objectOutputStream, final Object object) throws IOException {
        if (object instanceof Byte) {
          objectOutputStream.writeByte((Byte) object);
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Byte.class;
      }
    }, STRING {
      @Override
      public String deserialize(final ObjectInputStream objectInputStream) throws IOException {
        return objectInputStream.readUTF();
      }

      @Override
      void serialize(final ObjectOutputStream objectOutputStream, final Object object) throws IOException {
        if (object instanceof String) {
          objectOutputStream.writeUTF((String)object);
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return String.class;
      }
    }, OBJECTID {
      @Override
      public ObjectID deserialize(final ObjectInputStream objectInputStream) throws IOException {
        return new ObjectID(objectInputStream.readLong());
      }

      @Override
      void serialize(final ObjectOutputStream objectOutputStream, final Object object) throws IOException {
        if (object instanceof ObjectID) {
          objectOutputStream.writeLong(((ObjectID)object).toLong());
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return ObjectID.class;
      }
    }, UTF8BYTES {
      @Override
      Object deserialize(final ObjectInputStream objectInputStream) throws IOException {
        byte[] bytes = new byte[objectInputStream.readInt()];
        objectInputStream.read(bytes);
        return new UTF8ByteDataHolder(bytes);
      }

      @Override
      void serialize(final ObjectOutputStream objectOutputStream, final Object object) throws IOException {
        if (object instanceof UTF8ByteDataHolder) {
          byte[] bytes = ((UTF8ByteDataHolder)object).getBytes();
          objectOutputStream.writeInt(bytes.length);
          objectOutputStream.write(bytes);
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return UTF8ByteDataHolder.class;
      }
    };

    abstract Object deserialize(ObjectInputStream objectInputStream) throws IOException;

    abstract void serialize(ObjectOutputStream objectOutputStream, Object object) throws IOException;

    abstract Class<?> toClass();
  }

  private static final Map<Class<?>, Type> classToType;
  static {
    classToType = new HashMap<Class<?>, Type>();
    for (Type type : Type.values()) {
      classToType.put(type.toClass(), type);
    }
  }


  @Override
  public Object deserialize(final ByteBuffer buffer) {
    ByteBufferInputStream byteBufferInputStream = new ByteBufferInputStream(buffer);
    try {
      ObjectInputStream ois = new ObjectInputStream(byteBufferInputStream);
      byte t = (byte) ois.read();
      return Type.values()[t].deserialize(ois);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public ByteBuffer serialize(final Object o) {
    if (o == null || !classToType.containsKey(o.getClass())) {
      throw new IllegalArgumentException("Unknown type " + o + " class " + o.getClass());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      try {
        Type t = classToType.get(o.getClass());
        oos.writeByte((byte) t.ordinal());
        t.serialize(oos, o);
      } finally {
        oos.close();
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return ByteBuffer.wrap(baos.toByteArray());
  }

  @Override
  public boolean equals(final ByteBuffer left, final Object right) {
    return deserialize(left).equals(right);
  }

  private class ByteBufferInputStream extends InputStream {
    final ByteBuffer buffer;

    private ByteBufferInputStream(final ByteBuffer buffer) {
      this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
      if (buffer.hasRemaining()) {
        return buffer.get() & 0xFF;
      } else {
        return -1;
      }
    }
  }
}
