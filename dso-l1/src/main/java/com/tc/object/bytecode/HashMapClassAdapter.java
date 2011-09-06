/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;

public class HashMapClassAdapter extends ClassAdapter implements Opcodes {

  public static final String J_MAP_CLASSNAME_DOTS = "java.util.HashMap";
  public static final String TC_MAP_CLASSNAME_DOTS = "java.util.HashMapTC";

  public static final ClassAdapterFactory FACTORY = new ClassAdapterFactory(){
    public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
      return new HashMapClassAdapter(visitor, getHashMapEntrySetMethodName(loader));
    }
  };

  /**
   * Check for the existance of HashMap.entrySet0() method, otherwise just use HashMap.entrySet() method
   */
  private static String getHashMapEntrySetMethodName(ClassLoader loader){
    String entrySetMethodName = "entrySet";
    
    final ClassInfo jClassInfo = AsmClassInfo.getClassInfo(J_MAP_CLASSNAME_DOTS, loader);
    final MethodInfo[] methods = jClassInfo.getMethods();
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getName().equals("entrySet0")){
        entrySetMethodName = methods[i].getName();
        break;
      }
    }
    
    return entrySetMethodName;
  }
  

  private final String entrySetMethodName;

  public HashMapClassAdapter(ClassVisitor cv, String entrySetMethodName) {
    super(cv);
    this.entrySetMethodName = entrySetMethodName;
  }

  public final MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);

    if (methodName.equals(entrySetMethodName) ) {
      mv = new EntrySetMethodAdapter(mv);
    }

    return mv;
  }

  private final static class EntrySetMethodAdapter extends MethodAdapter implements Opcodes {
    public EntrySetMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    /**
     * Modify the entrySet (or entrySet0 depending on JDK) method - wrap the returned Set in an EntrySetWrapper
     * 
     * With this instrumentation, the modified method looks roughly like this:
     * <code>
     *   private Set entrySet0() {
     *     Set es = entrySet;
     *     es = (es != null ? es : (entrySet = new EntrySet()));
     *     return new EntrySetWrapper( es );
     *   }
     * </code>
     */
    public void visitInsn(final int opcode) {

      if (opcode == ARETURN) {
        mv.visitVarInsn(ASTORE, 1);
        mv.visitTypeInsn(NEW, "java/util/HashMap$EntrySetWrapper");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap$EntrySetWrapper", "<init>", "(Ljava/util/HashMap;Ljava/util/Set;)V");
      }
      super.visitInsn(opcode);
    }
  }

}
