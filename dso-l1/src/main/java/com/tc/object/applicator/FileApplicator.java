/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.TCClass;
import com.tc.object.TCObjectExternal;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Method;

public class FileApplicator extends PhysicalApplicator {
  private final static Method FILE_READ_OBJECT     = findReadObjectMethod();

  private final static String FILE_SEPARATOR_FIELD = "java.io.File._tcFileSeparator";

  public FileApplicator(TCClass clazz, DNAEncoding encoding) {
    super(clazz, encoding);
  }

  private static Method findReadObjectMethod() {
    try {
      Method m = File.class.getDeclaredMethod("readObject", new Class[] { ObjectInputStream.class });
      m.setAccessible(true);
      return m;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    boolean remoteFileSeparatorObtained = false;
    char sepChar = 0;

    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      Assert.eval(a.isTruePhysical());
      String fieldName = a.getFieldName();
      Object fieldValue = a.getObject();
      if (FILE_SEPARATOR_FIELD.equals(fieldName)) {
        sepChar = ((String) fieldValue).charAt(0);
        remoteFileSeparatorObtained = true;
      } else {
        tcObject.setValue(fieldName, fieldValue);
      }
    }

    if (!dna.isDelta()) {
      Assert.assertTrue(remoteFileSeparatorObtained);
      try {
        FILE_READ_OBJECT.invoke(po, new Object[] { new FileObjectInputStream(sepChar, ((File) po).getPath()) });
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    super.dehydrate(objectManager, tcObject, writer, pojo);

    String fieldName = FILE_SEPARATOR_FIELD;
    String fieldValue = File.separator;
    writer.addPhysicalAction(fieldName, fieldValue, false);
  }

  private static class FileObjectInputStream extends ObjectInputStream {

    private final char   sep;
    private boolean      charRead;
    private final String path;

    protected FileObjectInputStream(char sep, String path) throws IOException, SecurityException {
      super();
      this.sep = sep;
      this.path = path;
    }

    @Override
    public void defaultReadObject() {
      //
    }

    @Override
    public GetField readFields() {
      return new GetField() {

        @Override
        public ObjectStreamClass getObjectStreamClass() {
          throw new AssertionError();
        }

        @Override
        public Object get(String name, Object val) {
          if ("path".equals(name)) { return path; }
          throw new AssertionError();
        }

        @Override
        public double get(String name, double val) {
          throw new AssertionError();
        }

        @Override
        public float get(String name, float val) {
          throw new AssertionError();
        }

        @Override
        public long get(String name, long val) {
          throw new AssertionError();
        }

        @Override
        public int get(String name, int val) {
          throw new AssertionError();
        }

        @Override
        public short get(String name, short val) {
          throw new AssertionError();
        }

        @Override
        public char get(String name, char val) {
          throw new AssertionError();
        }

        @Override
        public byte get(String name, byte val) {
          throw new AssertionError();
        }

        @Override
        public boolean get(String name, boolean val) {
          throw new AssertionError();
        }

        @Override
        public boolean defaulted(String name) {
          throw new AssertionError();
        }
      };
    }

    @Override
    public char readChar() throws IOException {
      if (charRead) { throw new EOFException(); }
      charRead = true;
      return sep;
    }
  }

}
