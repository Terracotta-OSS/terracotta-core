/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * MethodVisitor that is able to delegate all the calls to an array of other
 * method visitors. Labels are properly created for each individual visitor
 * and a simple mapping is maintained to be able to retrieve which label
 * belongs to which visitor.
 */
public class MulticastMethodVisitor implements MethodVisitor {

  private final MethodVisitor[] visitors;
  
  private final Map labelsMapping = new HashMap();
  
  public MulticastMethodVisitor(MethodVisitor[] visitors) {
    this.visitors = visitors;
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitAnnotation(desc, visible);      
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  public AnnotationVisitor visitAnnotationDefault() {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitAnnotationDefault();      
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  public void visitAttribute(Attribute attr) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitAttribute(attr);
    }
  }

  public void visitCode() {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitCode();
    }
  }

  public void visitEnd() {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitEnd();
    }
  }

  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitFieldInsn(opcode, owner, name, desc);
    }
  }

  public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitFrame(type, local, local2, stack, stack2);
    }
  }

  public void visitIincInsn(int var, int increment) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitIincInsn(var, increment);
    }
  }

  public void visitInsn(int opcode) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitInsn(opcode);
    }
  }

  public void visitIntInsn(int opcode, int operand) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitIntInsn(opcode, operand);
    }
  }

  public void visitJumpInsn(int opcode, Label label) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitJumpInsn(opcode, getMappedLabel(label, i));
    }
  }

  public void visitLabel(Label label) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLabel(getMappedLabel(label, i));
    }
  }

  public void visitLdcInsn(Object cst) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLdcInsn(cst);
    }
  }

  public void visitLineNumber(int line, Label start) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLineNumber(line, getMappedLabel(start, i));
    }
  }

  public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLocalVariable(name, desc, signature, getMappedLabel(start, i), getMappedLabel(end, i), index);
    }
  }

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLookupSwitchInsn(getMappedLabel(dflt, i), keys, getMappedLabels(labels, i));
    }
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitMaxs(maxStack, maxLocals);
    }
  }

  public void visitMethodInsn(int opcode, String owner, String name, String desc) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitMethodInsn(opcode, owner, name, desc);
    }
  }

  public void visitMultiANewArrayInsn(String desc, int dims) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitMultiANewArrayInsn(desc, dims);
    }
  }

  public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitParameterAnnotation(parameter, desc, visible);      
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitTableSwitchInsn(min, max, getMappedLabel(dflt, i), getMappedLabels(labels, i));
    }
  }

  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitTryCatchBlock(getMappedLabel(start, i), getMappedLabel(end, i), getMappedLabel(handler, i), type);
    }
  }

  public void visitTypeInsn(int opcode, String desc) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitTypeInsn(opcode, desc);
    }
  }

  public void visitVarInsn(int opcode, int var) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitVarInsn(opcode, var);
    }
  }
  
  private Label getMappedLabel(Label original, int visitorIndex) {
    if (null == original) return null;
    return getMappedLabels(original)[visitorIndex];
  }
  
  private Label[] getMappedLabels(Label[] originals, int visitorIndex) {
    if (null == originals) return null;
    
    Label[] result = new Label[originals.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = getMappedLabel(originals[i], visitorIndex);
    }
    return result;
  }

  private Label[] getMappedLabels(Label original) {
    Label[] labels = (Label[])labelsMapping.get(original);
    if (null == labels) {
      labels = new Label[visitors.length];
      for (int i = 0; i < visitors.length; i++) {
        labels[i] = new Label();
      }
      labelsMapping.put(original, labels);
    }
    return labels;
  }
}