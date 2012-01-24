/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;

public class AddInterfacesAdapter extends ClassAdapter implements ClassAdapterFactory {

  private final String[] toAdd;

  public AddInterfacesAdapter(ClassVisitor cv, String[] toAdd) {
    super(cv);
    this.toAdd = toAdd;
  }

  public AddInterfacesAdapter(String[] toAdd) {
    super(null);
    this.toAdd = toAdd;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, ByteCodeUtil.addInterfaces(interfaces, toAdd));
  }

  @Override
  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new AddInterfacesAdapter(visitor, toAdd);
  }
}
