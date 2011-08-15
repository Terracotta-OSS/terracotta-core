/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.util.CheckClassAdapter;
import com.tc.object.tools.BootJar;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapterTest extends TCTestCase {

  public JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapterTest() {
    //
  }

  public void testClasAdapter() throws IOException {
    String res = BootJar.classNameToFileName("java.util.concurrent.LinkedBlockingQueue$Itr");
    ClassReader cr = new ClassReader(getClass().getClassLoader().getResourceAsStream(res));
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ClassVisitor cv = new JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapter(new CheckClassAdapter(cw));
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, pw);
    assertTrue(sw.toString(), sw.toString().length()==0);
  }

}
