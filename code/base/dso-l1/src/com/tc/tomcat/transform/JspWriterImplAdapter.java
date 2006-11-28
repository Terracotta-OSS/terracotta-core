/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassVisitor;
import com.tc.object.bytecode.StringGetCharsAdapter;

public class JspWriterImplAdapter extends StringGetCharsAdapter {

  public JspWriterImplAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv, new String[] { ".*" });
  }

}
