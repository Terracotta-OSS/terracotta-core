/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.AnnotationVisitor;

/**
 * AnnotationVisitor that is able to delegate all the calls to an array of
 * other annotation visitors.
 */
public class MulticastAnnotationVisitor implements AnnotationVisitor {
  
  private final AnnotationVisitor[] visitors;
  
  public MulticastAnnotationVisitor(AnnotationVisitor[] visitors) {
    this.visitors = visitors;
  }

  public void visit(String name, Object value) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visit(name, value);
    }
  }

  public AnnotationVisitor visitAnnotation(String name, String desc) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitAnnotation(name, desc);      
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  public AnnotationVisitor visitArray(String name) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitArray(name);      
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  public void visitEnd() {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitEnd();
    }
  }

  public void visitEnum(String name, String desc, String value) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitEnum(name, desc, value);
    }
  }
}