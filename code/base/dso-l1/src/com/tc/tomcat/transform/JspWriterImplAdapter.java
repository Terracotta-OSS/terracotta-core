/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassVisitor;
import com.tc.object.bytecode.StringGetCharsAdapter;

public class JspWriterImplAdapter extends StringGetCharsAdapter {

  public JspWriterImplAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv, new String[] { ".*" });
  }

}
