/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import org.apache.commons.io.IOUtils;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.FieldNode;
import com.tc.asm.tree.MethodNode;
import com.tc.exception.TCRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Merges the source class into the target class. All interfaces implemented by source is added to target; also fields,
 * methods are also added to target. This adapter should be added to transform the target class
 */
public class MergeSourceToTargetAdapter extends ClassAdapter implements ClassAdapterFactory {
  private static final String DOT   = ".";
  private static final String SLASH = "/";
  private final ClassNode     sourceClassNode;
  private final ClassVisitor  actualVisitor;
  private final String        targetClassNameDots;
  private final String        sourceClassNameDots;

  /**
   * When created for use as factory, the {@link ClassVisitor} parameter is ignored
   */
  public MergeSourceToTargetAdapter(ClassVisitor cv, String targetClassNameDots, String sourceClassNameDots) {
    super(cv);
    actualVisitor = cv;
    this.sourceClassNameDots = sourceClassNameDots;
    this.targetClassNameDots = targetClassNameDots;
    sourceClassNode = createSourceClassNode();
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loaderParam) {
    return new MergeSourceToTargetAdapter(visitor, targetClassNameDots, sourceClassNameDots);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, (String[]) sourceClassNode.interfaces.toArray(new String[0]));
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public void visitEnd() {
    addSourceFields();
    addSourceMethods();
    super.visitEnd();
  }

  public void addSourceFields() {
    for (Iterator iter = sourceClassNode.fields.iterator(); iter.hasNext();) {
      FieldNode fn = (FieldNode) iter.next();
      fn.accept(actualVisitor);
    }
  }

  public void addSourceMethods() {
    for (Iterator iter = sourceClassNode.methods.iterator(); iter.hasNext();) {
      MethodNode m = (MethodNode) iter.next();
      if (m.name.equals("<init>")) {
        // all constructors are skipped
        continue;
      }
      m.accept(new SourceToTargetRenameClassAdapter(actualVisitor));
    }
  }

  private String replaceClassName(String classNameDots) {
    if (classNameDots == null || classNameDots.length() == 0) { return classNameDots; }
    classNameDots = classNameDots.replace(DOT, SLASH);
    String sourceName = sourceClassNameDots.replace(DOT, SLASH);
    String targetName = targetClassNameDots.replace(DOT, SLASH);
    if (classNameDots.equals(sourceName)) {
      return targetName;
    } else if (classNameDots.startsWith(sourceName)) {
      // recheck this
      return targetName + classNameDots.substring(sourceName.length());
    } else {
      return classNameDots;
    }

  }

  private class SourceToTargetRenameClassAdapter extends ClassAdapter implements Opcodes {
    public SourceToTargetRenameClassAdapter(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superClassName, String[] interfaces) {
      name = replaceClassName(name);
      superClassName = replaceClassName(superClassName);
      super.visit(version, access, name, signature, superClassName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      desc = replaceClassName(desc);
      signature = replaceClassName(signature);
      return new SourceToTargetRenameMethodAdapter(super.visitMethod(access, name, desc, signature, exceptions));
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      super.visitInnerClass(replaceClassName(name), replaceClassName(outerName), replaceClassName(innerName), access);
    }

    private class SourceToTargetRenameMethodAdapter extends MethodAdapter implements Opcodes {
      public SourceToTargetRenameMethodAdapter(MethodVisitor mv) {
        super(mv);
      }

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        owner = replaceClassName(owner);
        // desc can also contain sourceName too... not happening atm
        // TODO revisit to clean desc too
        // desc = replaceClassName(desc);
        super.visitMethodInsn(opcode, owner, name, desc);
      }

      @Override
      public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        owner = replaceClassName(owner);
        // desc = replaceClassName(desc);
        super.visitFieldInsn(opcode, owner, name, desc);
      }

    }
  }

  private ClassNode createSourceClassNode() {
    ClassNode node = new ClassNode();
    ClassReader cr = new ClassReader(getSourceClassBytes(sourceClassNameDots));
    cr.accept(node, ClassReader.SKIP_FRAMES);
    return node;
  }

  private static byte[] getSourceClassBytes(String sourceClassNameDots) {
    return getBytes(sourceClassNameDots, MergeSourceToTargetAdapter.class.getClassLoader());
  }

  private static final byte[] getBytes(final String classNameDots, final ClassLoader provider) {
    try {
      return getBytesForClass(classNameDots, provider);
    } catch (final Exception e) {
      throw new TCRuntimeException("Error sourcing bytes for class " + classNameDots, e);
    }
  }

  public static final byte[] getBytesForClass(final String classNameDots, final ClassLoader loader)
      throws ClassNotFoundException {
    InputStream input = null;
    String resource = null;
    try {
      resource = classNameDots.replace('.', '/') + ".class";
      input = loader.getResourceAsStream(resource);
      if (input == null) { throw new ClassNotFoundException("No resource found for class: " + classNameDots); }
      return IOUtils.toByteArray(input);
    } catch (final IOException e) {
      throw new ClassNotFoundException("Error reading bytes from " + resource, e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
