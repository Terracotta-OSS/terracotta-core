/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Attribute;

/**
 * Adapters to handle annotation copy
 * FIXME license EPL + FIXME field annotation + ctor annotation when all wrapped ??  [but why does it matters]
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class AsmCopyAdapter {

  /**
   * Copy annotation from one to another place
   */
  public static class CopyAnnotationAdapter implements AnnotationVisitor {

    private final AnnotationVisitor m_from;

    private final AnnotationVisitor m_to;

    public CopyAnnotationAdapter(AnnotationVisitor from, AnnotationVisitor copyTo) {
      m_from = from;
      m_to = copyTo;
    }

    public void visit(String name, Object value) {
      m_from.visit(name, value);
      m_to.visit(name, value);
    }

    public void visitEnum(String name, String desc, String value) {
      m_from.visitEnum(name, desc, value);
      m_to.visitEnum(name, desc, value);
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
      return new CopyAnnotationAdapter(
              m_from.visitAnnotation(name, desc),
              m_to.visitAnnotation(name, desc)
      );
    }

    public AnnotationVisitor visitArray(String name) {
      return new CopyAnnotationAdapter(
              m_from.visitArray(name),
              m_to.visitArray(name)
      );
    }

    public void visitEnd() {
      m_from.visitEnd();
      m_to.visitEnd();
    }

  }

  /**
   * Copy all annotations of a method
   * //FIXME needed ?
   */
  public static class CopyMethodAnnotationElseNullAdapter extends AsmNullAdapter.NullMethodAdapter {

    private final MethodVisitor m_copyTo;

    public CopyMethodAnnotationElseNullAdapter(MethodVisitor copyTo) {
      m_copyTo = copyTo;
    }

    public void visitAttribute(Attribute attr) {
      m_copyTo.visitAttribute(attr);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return new CopyAnnotationAdapter(
              AsmNullAdapter.NullAnnotationVisitor.NULL_ANNOTATION_ADAPTER,
              m_copyTo.visitAnnotation(desc, visible)
      );
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
      return new CopyAnnotationAdapter(
              AsmNullAdapter.NullAnnotationVisitor.NULL_ANNOTATION_ADAPTER,
              m_copyTo.visitParameterAnnotation(parameter, desc, visible)
      );
    }

  }
}
