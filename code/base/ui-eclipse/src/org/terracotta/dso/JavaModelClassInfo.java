/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.bytecode.aspectwerkz.SimpleClassInfo;

public class JavaModelClassInfo extends SimpleClassInfo {
  private IType fType;
  private String fSuperClassSig;
  private ClassInfo fSuperClass;
  private String[] fInterfaceSigs;
  private ClassInfo[] fInterfaces;
  
  private static final String[] NO_INTERFACE_SIGS = new String[0];
  private static final ClassInfo[] NO_INTERFACES = new ClassInfo[0];
  
  public JavaModelClassInfo(String classname) {
    super(classname);
    fInterfaceSigs = NO_INTERFACE_SIGS;
    fInterfaces = NO_INTERFACES;
  }
  
  public JavaModelClassInfo(IType type) {
    super(type.getFullyQualifiedName('$'));
    fType = type;
    try {
      fSuperClassSig = type.getSuperclassTypeSignature();
      if(fSuperClassSig != null) {
        fSuperClass = new JavaModelClassInfo(JdtUtils.getResolvedTypeName(fSuperClassSig, type));
      }
      fInterfaceSigs = NO_INTERFACE_SIGS;
      fInterfaces = NO_INTERFACES;
      if(type.isClass()) {
        fInterfaceSigs = type.getSuperInterfaceTypeSignatures();
        fInterfaces = new JavaModelClassInfo[fInterfaceSigs.length];
        for(int i = 0; i < fInterfaceSigs.length; i++) {
          fInterfaces[i] = new JavaModelClassInfo(JdtUtils.getResolvedTypeName(fInterfaceSigs[i], type));
        }
      }
    } catch(JavaModelException jme) {
      fInterfaceSigs = NO_INTERFACE_SIGS;
      fInterfaces = NO_INTERFACES;
    }
  }

  public IType getType() {
    return fType;
  }
  
  public ClassInfo getSuperclass() {
    return fSuperClass;
  }
  
  public ClassInfo[] getInterfaces() {
    return fInterfaces;
  }
  
  public boolean isStale() {
    try {
      if(fType == null) return true;
      String superClassSig = fType.getSuperclassTypeSignature();
      if(superClassSig == null && fSuperClassSig != null ||
          superClassSig != null && fSuperClassSig == null ||
          superClassSig != null && !superClassSig.equals(fSuperClassSig)) { return true; }

      String[] interfaceSigs = fType.getSuperInterfaceTypeSignatures();
      if(interfaceSigs.length != fInterfaceSigs.length) return true;
      for(int i = 0; i < interfaceSigs.length; i++) {
        if(!interfaceSigs[i].equals(fInterfaceSigs[i])) return true;
      }
    } catch(JavaModelException jme) {/**/}
    return false;
  }
}
