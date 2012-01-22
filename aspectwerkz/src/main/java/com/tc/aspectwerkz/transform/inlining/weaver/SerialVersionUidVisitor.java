/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import com.tc.asm.*;

import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;

/**
 * See http://java.sun.com/j2se/1.5.0/docs/guide/serialization/spec/class.html#60
 * <p/>
 * The SerialVersionUidVisitor lookups for the serial ver uid and compute it when not found.
 * See Add and Compute subclasses.
 * <p/>
 * Initial implementation courtesy of Vishal Vishnoi <vvishnoi AT bea DOT com>
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class SerialVersionUidVisitor extends ClassAdapter implements Opcodes {

  public static final String CLINIT = "<clinit>";
  public static final String INIT = "<init>";
  public static final String SVUID_NAME = "serialVersionUID";

  /**
   * flag that indicates if we need to compute SVUID (no need for interfaces)
   */
  protected boolean m_computeSVUID = true;

  /**
   * Set to true if the class already has SVUID
   */
  protected boolean m_hadSVUID = false;

  /**
   * The SVUID value (valid at the end of the visit only ie the one that was present or the computed one)
   */
  protected long m_SVUID;

  /**
   * Internal name of the class
   */
  protected String m_className;

  /**
   * Classes access flag
   */
  protected int m_access;

  /**
   * Interfaces implemented by the class
   */
  protected String[] m_interfaces;

  /**
   * Collection of fields. (except private static
   * and private transient fields)
   */
  protected Collection m_svuidFields = new ArrayList();

  /**
   * Set to true if the class has static initializer
   */
  protected boolean m_hasStaticInitializer = false;

  /**
   * Collection of non private constructors.
   */
  protected Collection m_svuidConstructors = new ArrayList();

  /**
   * Collection of non private method
   */
  protected Collection m_svuidMethods = new ArrayList();

  /**
   * helper method (test purpose)
   *
   * @param klass
   * @return
   */
  public static long calculateSerialVersionUID(Class klass) {
    try {
      ClassReader cr = new ClassReader(klass.getName());
      ClassWriter cw = AsmHelper.newClassWriter(true);
      SerialVersionUidVisitor sv = new SerialVersionUidVisitor(cw);
      cr.accept(sv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      return sv.m_SVUID;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  SerialVersionUidVisitor(final ClassVisitor cv) {
    super(cv);
  }

  /**
   * Visit class header and getDefault class name, access , and interfaces information
   * (step 1,2, and 3) for SVUID computation.
   */
  public void visit(int version, int access,
                    String name, String signature, String superName,
                    String[] interfaces) {
    // getDefault SVUID info. only if check passes
    if (mayNeedSerialVersionUid(access)) {
      m_className = name;
      m_access = access;
      m_interfaces = interfaces;
    }

    // delegate call to class visitor
    super.visit(version, access, name, signature, superName, interfaces);
  }

  /**
   * Visit the methods and getDefault constructor and method information (step
   * 5 and 7). Also determince if there is a class initializer (step 6).
   */
  public MethodVisitor visitMethod(int access,
                                   String name, String desc, String signature,
                                   String[] exceptions) {
    // getDefault SVUI info
    if (m_computeSVUID) {

      // class initialized
      if (name.equals(CLINIT)) {
        m_hasStaticInitializer = true;
      } else {
        // Remember non private constructors and methods for SVUID computation later.
        if ((access & ACC_PRIVATE) == 0) {
          if (name.equals(INIT)) {
            m_svuidConstructors.add(new MethodItem(name, access, desc));
          } else {
            m_svuidMethods.add(new MethodItem(name, access, desc));
          }
        }
      }

    }

    // delegate call to class visitor
    return cv.visitMethod(access, name, desc, signature, exceptions);
  }

  /**
   * Gets class field information for step 4 of the alogrithm. Also determines
   * if the class already has a SVUID.
   */
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    // getDefault SVUID info
    if (m_computeSVUID) {

      // check SVUID
      if (name.equals(SVUID_NAME)) {
        m_hadSVUID = true;
        // we then don't need to compute it actually
        m_computeSVUID = false;
        m_SVUID = ((Long) value).longValue();
      }

      /*
      * Remember field for SVUID computation later.
      * except private static and private transient fields
      */
      if (((access & ACC_PRIVATE) == 0) ||
              ((access & (ACC_STATIC | ACC_TRANSIENT)) == 0)) {
        m_svuidFields.add(new FieldItem(name, access, desc));
      }

    }

    // delegate call to class visitor
    return super.visitField(access, name, desc, signature, value);
  }

  /**
   * Add the SVUID if class doesn't have one
   */
  public void visitEnd() {
    if (m_computeSVUID) {
      // compute SVUID if the class doesn't have one
      if (!m_hadSVUID) {
        try {
          m_SVUID = computeSVUID();
        } catch (Throwable e) {
          throw new RuntimeException("Error while computing SVUID for " + m_className, e);
        }
      }
    }

    // delegate call to class visitor
    super.visitEnd();
  }

  protected boolean mayNeedSerialVersionUid(int access) {
    return true;
    // we don't need to compute SVUID for interfaces //TODO why ???
//            if ((access & ACC_INTERFACE) == ACC_INTERFACE) {
//                m_computeSVUID = false;
//            } else {
//                m_computeSVUID = true;
//            }
//            return m_computeSVUID;
  }

  /**
   * Returns the value of SVUID if the class doesn't have one already. Please
   * note that 0 is returned if the class already has SVUID, thus use
   * <code>isHasSVUID</code> to determine if the class already had an SVUID.
   *
   * @return Returns the serila version UID
   */
  protected long computeSVUID() throws IOException, NoSuchAlgorithmException {
    ByteArrayOutputStream bos = null;
    DataOutputStream dos = null;
    long svuid = 0;

    try {

      bos = new ByteArrayOutputStream();
      dos = new DataOutputStream(bos);

      /*
        1. The class name written using UTF encoding.
      */
      dos.writeUTF(m_className.replace('/', '.'));

      /*
        2. The class modifiers written as a 32-bit integer.
      */
      int classMods = m_access & (ACC_PUBLIC | ACC_FINAL | ACC_INTERFACE | ACC_ABSTRACT);
      dos.writeInt(classMods);

      /*
        3. The name of each interface sorted by name written using UTF encoding.
      */
      Arrays.sort(m_interfaces);
      for (int i = 0; i < m_interfaces.length; i++) {
        String ifs = m_interfaces[i].replace('/', '.');
        dos.writeUTF(ifs);
      }

      /*
        4. For each field of the class sorted by field name (except private
        static and private transient fields):

          1. The name of the field in UTF encoding.
          2. The modifiers of the field written as a 32-bit integer.
          3. The descriptor of the field in UTF encoding

        Note that field signatutes are not dot separated. Method and
        constructor signatures are dot separated. Go figure...
      */
      writeItems(m_svuidFields, dos, false);

      /*
        5. If a class initializer exists, write out the following:
          1. The name of the method, <clinit>, in UTF encoding.
          2. The modifier of the method, java.lang.reflect.Modifier.STATIC,
             written as a 32-bit integer.
          3. The descriptor of the method, ()V, in UTF encoding.
      */
      if (m_hasStaticInitializer) {
        dos.writeUTF("<clinit>");
        dos.writeInt(ACC_STATIC);
        dos.writeUTF("()V");
      }

      /*
        6. For each non-private constructor sorted by method name and signature:
          1. The name of the method, <init>, in UTF encoding.
          2. The modifiers of the method written as a 32-bit integer.
          3. The descriptor of the method in UTF encoding.
      */
      writeItems(m_svuidConstructors, dos, true);

      /*
        7. For each non-private method sorted by method name and signature:
          1. The name of the method in UTF encoding.
          2. The modifiers of the method written as a 32-bit integer.
          3. The descriptor of the method in UTF encoding.
      */
      writeItems(m_svuidMethods, dos, true);

      dos.flush();

      /*
        8. The SHA-1 algorithm is executed on the stream of bytes produced by
        DataOutputStream and produces five 32-bit values sha[0..4].
      */
      MessageDigest md = MessageDigest.getInstance("SHA");

      /*
        9. The hash value is assembled from the first and second 32-bit values
        of the SHA-1 message digest. If the result of the message digest, the
        five 32-bit words H0 H1 H2 H3 H4, is in an array of five int values
        named sha, the hash value would be computed as follows:

        long hash = ((sha[0] >>> 24) & 0xFF) |
        ((sha[0] >>> 16) & 0xFF) << 8 |
        ((sha[0] >>> 8) & 0xFF) << 16 |
        ((sha[0] >>> 0) & 0xFF) << 24 |
        ((sha[1] >>> 24) & 0xFF) << 32 |
        ((sha[1] >>> 16) & 0xFF) << 40 |
        ((sha[1] >>> 8) & 0xFF) << 48 |
        ((sha[1] >>> 0) & 0xFF) << 56;
      */
      byte[] hashBytes = md.digest(bos.toByteArray());
      for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
        svuid = (svuid << 8) | (hashBytes[i] & 0xFF);
      }

    } finally {
      // close the stream (if open)
      if (dos != null) {
        dos.close();
      }
    }

    return svuid;
  }

  /**
   * Sorts the items in the collection and writes it to the data output stream
   *
   * @param itemCollection collection of items
   * @param dos            a <code>DataOutputStream</code> value
   * @param dotted         a <code>boolean</code> value
   * @throws IOException if an error occurs
   */
  protected void writeItems(Collection itemCollection,
                            DataOutputStream dos,
                            boolean dotted) throws IOException {
    int size = itemCollection.size();
    Item items[] = new Item[size];
    items = (Item[]) itemCollection.toArray(items);
    Arrays.sort(items);

    for (int i = 0; i < size; i++) {
      items[i].write(dos, dotted);
    }
  }

  /**
   * An Item represent a field / method / constructor needed in the computation
   */
  static abstract class Item implements Comparable {
    private String m_name;
    private int m_access;
    private String m_desc;

    Item(String name, int access, String desc) {
      m_name = name;
      m_access = access;
      m_desc = desc;
    }

    // see spec, modifiers must be filtered
    protected abstract int filterAccess(int access);

    public int compareTo(Object o) {
      Item other = (Item) o;
      int retVal = m_name.compareTo(other.m_name);
      if (retVal == 0) {
        retVal = m_desc.compareTo(other.m_desc);
      }
      return retVal;
    }

    void write(DataOutputStream dos, boolean dotted) throws IOException {
      dos.writeUTF(m_name);
      dos.writeInt(filterAccess(m_access));
      if (dotted) {
        dos.writeUTF(m_desc.replace('/', '.'));
      } else {
        dos.writeUTF(m_desc);
      }
    }
  }

  /**
   * A field item
   */
  static class FieldItem extends Item {
    FieldItem(String name, int access, String desc) {
      super(name, access, desc);
    }

    protected int filterAccess(int access) {
      return access & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL
              | ACC_VOLATILE | ACC_TRANSIENT);
    }
  }

  /**
   * A method / constructor item
   */
  static class MethodItem extends Item {
    MethodItem(String name, int access, String desc) {
      super(name, access, desc);
    }

    protected int filterAccess(int access) {
      return access & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL
              | ACC_SYNCHRONIZED | ACC_NATIVE | ACC_ABSTRACT | ACC_STRICT);
    }
  }

  /**
   * Add the serial version uid to the class if not already present
   */
  public static class Add extends ClassAdapter {

    private InstrumentationContext m_ctx;
    private ClassInfo m_classInfo;

    public Add(ClassVisitor classVisitor, InstrumentationContext ctx, ClassInfo classInfo) {
      super(classVisitor);
      m_ctx = ctx;
      m_classInfo = classInfo;
    }

    public void visitEnd() {
      if (ClassInfoHelper.implementsInterface(m_classInfo, "java.io.Serializable")) {
        ClassReader cr = new ClassReader(m_ctx.getInitialBytecode());
        ClassWriter cw = AsmHelper.newClassWriter(true);
        SerialVersionUidVisitor sv = new SerialVersionUidVisitor(cw);
        cr.accept(sv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (sv.m_computeSVUID && !sv.m_hadSVUID) {
          cv.visitField(ACC_FINAL + ACC_STATIC, SVUID_NAME, "J", null, Long.valueOf(sv.m_SVUID));
        }
      }
      super.visitEnd();
    }
  }
}
