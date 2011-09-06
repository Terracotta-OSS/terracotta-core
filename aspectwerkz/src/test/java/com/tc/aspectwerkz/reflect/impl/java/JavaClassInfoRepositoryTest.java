/*
 * All content copyright (c) 2003-2010 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.ref.WeakReference;
import java.net.URLClassLoader;

import junit.framework.TestCase;

public class JavaClassInfoRepositoryTest extends TestCase {

  public void testBasic() {
    Loader loader = new Loader();

    for (int i = 0; i < 3; i++) {
      assertTrue(JavaClassInfoRepository.getRepository(loader) == JavaClassInfoRepository.getRepository(loader));
      ClassInfo ci1 = JavaClassInfoRepository.getRepository(loader).getClassInfo(JavaClassInfoRepository.class
                                                                                     .getName());
      ClassInfo ci2 = JavaClassInfoRepository.getRepository(loader).getClassInfo(JavaClassInfoRepository.class
                                                                                     .getName());
      assertTrue(ci1 == ci2);
      
      System.gc();
      ThreadUtil.reallySleep(1000L);      
    }

    WeakReference<JavaClassInfoRepository> repoRef = new WeakReference<JavaClassInfoRepository>(
                                                                                                JavaClassInfoRepository
                                                                                                    .getRepository(loader));
    loader = null;
    for (int i = 0; i < 3; i++) {
      System.gc();
      ThreadUtil.reallySleep(3000L);
    }

    assertNull(repoRef.get());
    assertEquals(0, JavaClassInfoRepository.repositoriesSize());
  }

  private static class Loader extends URLClassLoader {
    public Loader() {
      super(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs(), null);
    }
  }

}
