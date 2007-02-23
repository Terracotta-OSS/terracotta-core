/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.StringGetCharsAdapter;

public class JspWriterImplAdapter extends StringGetCharsAdapter implements ClassAdapterFactory {

  public JspWriterImplAdapter() {
    super(null, null);
  }
  
  private JspWriterImplAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv, new String[] { ".*" });
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new JspWriterImplAdapter(visitor, loader);
  }

}
