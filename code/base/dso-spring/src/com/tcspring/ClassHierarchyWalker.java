/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.asm.signature.SignatureReader;
import com.tc.asm.signature.SignatureVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.object.bytecode.hook.DSOContext;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class ClassHierarchyWalker {
  private final transient Log logger    = LogFactory.getLog(getClass());

  private final String        id;
  private final DSOContext    dsoContext;

  private final LinkedList    queue     = new LinkedList();
  private final Set           processed = new HashSet();                // collection of class names

  public ClassHierarchyWalker(String id, DSOContext dsoContext) {
    this.id = id;
    this.dsoContext = dsoContext;
  }

  public void walkClass(ClassInfo classInfo) {
    addClassIfNeeded(classInfo);
    walkThroughClassHierarchy();
  }

  public void walkClass(String className, ClassLoader loader) {
    try {
      walkClass(AsmClassInfo.getClassInfo(className, loader));
    } catch (Exception e) {
      // System.err.println("### ClassHierarchyWalker.walkClass() "+e.getException());
      logger.warn("Unable to read class " + className, e);
    }
  }

  private void walkThroughClassHierarchy() {
    while (queue.size() > 0) {
      ClassInfo classInfo = (ClassInfo) queue.removeFirst();
      processed.add(classInfo.getName());

      addClassGenerics(classInfo.getGenericsSignature(), classInfo.getClassLoader());
      
      String className = classInfo.getName();
      logger.info(this.id + " registering include for " + className);
      // TODO add matching for subclasses
      dsoContext.addInclude(className, true, "* " + className + ".*(..)", classInfo);

      // TODO should we continue walking class hierarchy if include had been ignored?

      addClassIfNeeded(classInfo.getSuperclass());

      FieldInfo[] fields = classInfo.getFields();
      for (int i = 0; i < fields.length; i++) {
        FieldInfo fieldInfo = fields[i];
        int modifiers = fieldInfo.getModifiers();
        if ((modifiers & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
          ClassInfo fieldType = fieldInfo.getType();
          while (fieldType.isArray()) {
            fieldType = fieldType.getComponentType();
          }
          addClassIfNeeded(fieldType);
          
          addFieldGenerics(fieldInfo.getGenericsSignature(), fieldType.getClassLoader());
        }
      }
    }
  }

  private void addClassIfNeeded(String name, ClassLoader loader) {
    addClassIfNeeded(AsmClassInfo.getClassInfo(name, loader));
  }
  
  private void addClassIfNeeded(ClassInfo classInfo) {
    if (classInfo == null) { return; }
    String className = classInfo.getName();
    if (!classInfo.isInterface() && !classInfo.isPrimitive()
        && !"org.springframework.context.ApplicationEvent".equals(className) // XXX this must not be here
        && !"java.lang.Object".equals(className)) {
      if (!processed.contains(className) && !queue.contains(classInfo)) {
        queue.add(classInfo);
      }
    }
  }

  
  
  private void addClassGenerics(String signature, ClassLoader loader) {
    if(signature!=null) {
      SignatureReader signatureReader = new SignatureReader(signature);
      signatureReader.accept(new GenericsCollector(loader));
    }
  }

  private void addFieldGenerics(String signature, ClassLoader loader) {
    if(signature!=null) {
      SignatureReader signatureReader = new SignatureReader(signature);
      signatureReader.accept(new GenericsCollector(loader));
    }
  }

  
  private final class GenericsCollector implements SignatureVisitor {
    private final ClassLoader loader;

    public GenericsCollector(ClassLoader loader) {
      this.loader = loader;
    }

    public void visitClassType(String name) {
      addClassIfNeeded(name, loader);
    }

    public void visitInnerClassType(String name) {
      addClassIfNeeded(name, loader);
    }
    
    public void visitFormalTypeParameter(String name) {
      //
    }

    public void visitTypeVariable(String name) {
      //      
    }

    public SignatureVisitor visitArrayType() {
      return this;
    }

    public SignatureVisitor visitClassBound() {
      return this;
    }

    public SignatureVisitor visitExceptionType() {
      return this;
    }

    public SignatureVisitor visitInterface() {
      return this;
    }

    public SignatureVisitor visitInterfaceBound() {
      return this;
    }

    public SignatureVisitor visitParameterType() {
      return this;
    }

    public SignatureVisitor visitReturnType() {
      return this;
    }

    public SignatureVisitor visitSuperclass() {
      return this;
    }

    public SignatureVisitor visitTypeArgument(char wildcard) {
      return this;
    }

    public void visitBaseType(char descriptor) {
      //
    }

    public void visitTypeArgument() {
      //
    }

    public void visitEnd() {
      //
    }
  }
  
}
