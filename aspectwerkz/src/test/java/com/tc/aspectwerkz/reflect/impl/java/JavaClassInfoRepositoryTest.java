/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class JavaClassInfoRepositoryTest extends TestCase {

  public void testBasic() {
    Loader loader = new Loader();

    for (int i = 0; i < 3; i++) {
      assertTrue(JavaClassInfoRepository.getRepository(loader) == JavaClassInfoRepository.getRepository(loader));
      ClassInfo ci1 = JavaClassInfoRepository.getRepository(loader).getClassInfo(JavaClassInfoRepository.class
                                                                                     .getName());
      System.gc();
      ThreadUtil.reallySleep(1000L);

      ClassInfo ci2 = JavaClassInfoRepository.getRepository(loader).getClassInfo(JavaClassInfoRepository.class
          .getName());
      assertTrue(ci1 == ci2);
    }

    WeakReference<JavaClassInfoRepository> repoRef = new WeakReference<JavaClassInfoRepository>(
                                                                                                JavaClassInfoRepository
                                                                                                    .getRepository(loader));
    loader = null;

    // keep hard refs on loaders or they might be collected
    List<Loader> loaders = new ArrayList<Loader>();
    for (int i = 0; i < 100; i++) {
      System.gc();

      // we need to exercise the JavaClassInfoRepository's s_repositories guava weak keys map -> make it add a new element
      // put calls MapMakerInternal.Segment.drainReferenceQueues() in guava 13.0.1 which is what we want to happen
      Loader l = new Loader();
      loaders.add(l);
      JavaClassInfoRepository.getRepository(l);
    }

    assertNull(repoRef.get());
    assertEquals(100, JavaClassInfoRepository.repositoriesSize());
  }

  private static class Loader extends URLClassLoader {
    public Loader() {
      super(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs(), null);
    }
  }

}
