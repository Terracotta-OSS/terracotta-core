/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.object.serialization.NotSerializableRuntimeException;

import com.tc.platform.PlatformService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SerializationStrategyImpl implements SerializationStrategy {

  /**
   * String keys which are really serialized objects will have this as their first char This particular value was chosen
   * since it is an invalid character in UTF-16 (http://unicode.org/faq/utf_bom.html#utf16-7)
   */
  private static final char              MARKER   = 0xFFFE;

  private static final byte              HIGH_BIT = (byte) 0x80;
  private final ObjectStreamClassMapping serializer;
  private final ClassLoader              tccl;

  public SerializationStrategyImpl(PlatformService platformService, SerializerMap serializerMap, ClassLoader loader) {
    this.serializer = new ObjectStreamClassMapping(platformService, serializerMap);
    this.tccl = loader;
  }

  @Override
  public Object deserialize(final byte[] data, boolean compression, boolean local) throws IOException,
      ClassNotFoundException {
    InputStream in = new ByteArrayInputStream(data);
    if (compression) {
      in = new GZIPInputStream(in);
    }
    return getObjectFromStream(in, local);
  }

  private Object getObjectFromStream(InputStream in, boolean local) throws IOException,
      ClassNotFoundException {
    SerializerObjectInputStream sois = new SerializerObjectInputStream(in, serializer, tccl, local);
    try {
      return sois.readObject();
    } catch (ObjectStreamClassNotFoundException e) {
      return null;
    } finally {
      sois.close();
    }
  }

  @Override
  public byte[] serialize(final Object value, boolean compression) throws NotSerializableRuntimeException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    SerializerObjectOutputStream oos = null;
    try {
      OutputStream out = baos;
      if (compression) {
        out = new GZIPOutputStream(out);
      }
      oos = new SerializerObjectOutputStream(out, serializer);
      oos.writeObject(value);
    } catch (IOException ioe) {
      throw new NotSerializableRuntimeException(ioe);
    } finally {
      try {
        if (oos != null) {
          oos.close();
        }
      } catch (IOException e) {
        throw new NotSerializableRuntimeException(e);
      }
    }
    return baos.toByteArray();
  }

  @Override
  public String serializeToString(final Object key) throws NotSerializableRuntimeException {
    if (key instanceof String) {
      String stringKey = (String) key;
      // disallow Strings that start with our marker
      if (stringKey.length() >= 1) {
        if (stringKey.charAt(0) == MARKER) {
          //
          throw new NotSerializableRuntimeException("Illegal string key: " + stringKey);
        }

      }
      return stringKey;
    }

    StringSerializedObjectOutputStream out;
    try {
      out = new StringSerializedObjectOutputStream();
      ObjectOutputStream oos = new SerializerObjectOutputStream(out, serializer);
      oos.writeObject(key);
      oos.close();
    } catch (IOException e) {
      throw new NotSerializableRuntimeException(e);
    }
    return out.toString();
  }

  @Override
  public Object deserializeFromString(final String key, boolean localOnly) throws IOException, ClassNotFoundException {
    if (key.length() >= 1 && key.charAt(0) == MARKER) {
      StringSerializedObjectInputStream ssois = new StringSerializedObjectInputStream(key);
      return getObjectFromStream(ssois, localOnly);
    }
    return key;
  }

  private static class StringSerializedObjectInputStream extends InputStream {
    private final String source;
    private final int    length;
    private int          index;

    StringSerializedObjectInputStream(String source) {
      this.source = source;
      this.length = source.length();

      read(); // skip marker char
    }

    @Override
    public int read() {
      if (index == length) {
        // EOF
        return -1;
      }

      return source.charAt(index++) & 0xFF;
    }
  }

  private static class StringSerializedObjectOutputStream extends OutputStream {
    private int    count;
    private char[] buf;

    StringSerializedObjectOutputStream() {
      this(16);
    }

    StringSerializedObjectOutputStream(int size) {
      size = Math.max(1, size);
      buf = new char[size];

      // always add our marker char
      buf[count++] = MARKER;
    }

    @Override
    public void write(int b) {
      if (count + 1 > buf.length) {
        char[] newbuf = new char[buf.length << 1];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
      }

      writeChar(b);
    }

    private void writeChar(int b) {
      // hibyte is always zero since UTF-8 encoding used for String storage in DSO!
      buf[count++] = (char) (b & 0xFF);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) { return; }
      int newcount = count + len;
      if (newcount > buf.length) {
        char newbuf[] = new char[Math.max(buf.length << 1, newcount)];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
      }

      for (int i = 0; i < len; i++) {
        writeChar(b[off + i]);
      }
    }

    @Override
    public String toString() {
      return new String(buf, 0, count);
    }
  }

  private static class SerializerObjectInputStream extends ObjectInputStream {

    private final ObjectStreamClassMapping oscSerializer;
    private final ClassLoader              loader;
    private final boolean                  local;

    public SerializerObjectInputStream(InputStream in, ObjectStreamClassMapping oscSerializer, ClassLoader loader,
                                       boolean local) throws IOException {
      super(in);
      this.oscSerializer = oscSerializer;
      this.loader = loader;
      this.local = local;
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
      int code = decodeInt(this);
      if (local) {
        ObjectStreamClass osc = oscSerializer.localGetObjectStreamClassFor(code);
        if (osc == null) { throw new ObjectStreamClassNotFoundException(); }
        return osc;
      } else {
        return oscSerializer.getObjectStreamClassFor(code);
      }
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      if (loader == null) {
        return super.resolveClass(desc);
      } else {
        try {
          return LoadClassUtil.loadClass(desc.getName(), loader);
        } catch (ClassNotFoundException e) {
          Class primitiveClass = PRIMITIVE_CLASSES.get(desc.getName());
          if (primitiveClass != null) {
            return primitiveClass;
          } else {
            throw e;
          }
        }
      }
    }

    private static final Map<String, Class<?>> PRIMITIVE_CLASSES = new HashMap<String, Class<?>>();

    static {
      PRIMITIVE_CLASSES.put("boolean", Boolean.TYPE);
      PRIMITIVE_CLASSES.put("byte", Byte.TYPE);
      PRIMITIVE_CLASSES.put("char", Character.TYPE);
      PRIMITIVE_CLASSES.put("short", Short.TYPE);
      PRIMITIVE_CLASSES.put("int", Integer.TYPE);
      PRIMITIVE_CLASSES.put("long", Long.TYPE);
      PRIMITIVE_CLASSES.put("float", Float.TYPE);
      PRIMITIVE_CLASSES.put("double", Double.TYPE);
      PRIMITIVE_CLASSES.put("void", Void.TYPE);
    }
  }

  private static class SerializerObjectOutputStream extends ObjectOutputStream {

    private final ObjectStreamClassMapping oscSerializer;

    public SerializerObjectOutputStream(final OutputStream out, final ObjectStreamClassMapping oscSerializer)
        throws IOException {
      super(out);
      this.oscSerializer = oscSerializer;
    }

    @Override
    protected void writeClassDescriptor(final ObjectStreamClass desc) throws IOException {
      int code = oscSerializer.getMappingFor(desc);
      encodeInt(this, code);
    }
  }

  private static final int decodeInt(final InputStream is) throws IOException {
    int rv = 0;
    int length = is.read();

    if ((length & HIGH_BIT) > 0) {
      length &= ~HIGH_BIT;
      if ((length == 0) || (length > 4)) { throw new IOException("invalid length: " + length);

      }
      for (int i = 0; i < length; i++) {
        int l = is.read() & 0xFF;
        rv |= (l << (8 * ((length - 1) - i)));
      }
      if (rv < 0) { throw new IOException("invalid value: " + rv); }
    } else {
      rv = length & 0xFF;
    }

    return rv;
  }

  private static final void encodeInt(final OutputStream os, final int value) throws IOException {
    if (value < 0) {
      throw new IOException("cannot encode negative values");
    } else if (value < 0x80) {
      os.write(value);
    } else if (value <= 0xFF) {
      os.write((0x01 | HIGH_BIT));
      os.write(value);
    } else if (value <= 0xFFFF) {
      os.write(0x02 | HIGH_BIT);
      os.write((value >> 8) & 0xFF);
      os.write(value & 0xFF);
    } else if (value <= 0xFFFFFF) {
      os.write(0x03 | HIGH_BIT);
      os.write((value >> 16) & 0xFF);
      os.write((value >> 8) & 0xFF);
      os.write(value & 0xFF);
    } else {
      os.write(0x04 | HIGH_BIT);
      os.write((value >> 24) & 0xFF);
      os.write((value >> 16) & 0xFF);
      os.write((value >> 8) & 0xFF);
      os.write(value & 0xFF);
    }
  }

  private static class ObjectStreamClassNotFoundException extends RuntimeException {
    //
  }

}
