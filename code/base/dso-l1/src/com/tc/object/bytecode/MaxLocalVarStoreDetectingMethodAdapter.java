/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

/**
 * Method adaptor that keeps track of the maximum store index that is used for a local variable.
 */
public class MaxLocalVarStoreDetectingMethodAdapter extends MethodAdapter implements Opcodes {

    private int max_var_store = 0;
    
    public MaxLocalVarStoreDetectingMethodAdapter(MethodVisitor mv) {
      super(mv);
    }    
    
    public void visitVarInsn(int opcode, int var) {
      // detect the maximum position at which local variables are already
      // stored in the method, this will be used during the instrumentation
      // to make sure that no current used local variables are overwritten
      if (ISTORE == opcode ||
          LSTORE == opcode ||
          FSTORE == opcode ||
          DSTORE == opcode ||
          ASTORE == opcode) {
        if (var > max_var_store) {
          max_var_store = var;
        }
      }
      
      super.visitVarInsn(opcode, var);
    }
    
    /**
     * Returns the current maximum local variable store index.
     */
    public int getMaxLocalVarStore() {
      return max_var_store;
    }
}