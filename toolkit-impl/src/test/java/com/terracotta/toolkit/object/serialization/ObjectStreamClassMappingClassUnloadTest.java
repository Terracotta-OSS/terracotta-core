/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.object.serialization;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tc.platform.PlatformService;

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * @author tim
 */
public class ObjectStreamClassMappingClassUnloadTest {

  private static final int PACKING_UNIT = 512 * 1024;

  public volatile WeakReference<Class<?>> classRef;
  public volatile Serializable specialObject;

  @Before
  public void createSpecialObject() throws Exception {
    ClassLoader duplicate = new URLClassLoader(((URLClassLoader) SpecialClass.class.getClassLoader()).getURLs(), null);

    @SuppressWarnings("unchecked")
    Class<? extends Serializable> special = (Class<? extends Serializable>) duplicate.loadClass(SpecialClass.class.getName());
    classRef = new WeakReference<Class<?>>(special);

    specialObject = special.newInstance();
  }


  @Test
  public void testClassUnloadingAfterGetMapping() throws Exception {
    ObjectStreamClassMapping objectStreamClassMapping = new ObjectStreamClassMapping(mock(PlatformService.class), new LocalSerializerMap());

    objectStreamClassMapping.getMappingFor(ObjectStreamClass.lookup(specialObject.getClass()));

    specialObject = null;

    for (int i = 0; i < 10; i++) {
      if (classRef.get() == null) {
        return;
      } else {
        packHeap();
      }
    }
    throw new AssertionError();
  }

  @Test
  public void testClassUnloadingAfterGetMappingAndGetDescriptor() throws Exception {
    ObjectStreamClassMapping objectStreamClassMapping = new ObjectStreamClassMapping(mock(PlatformService.class), new LocalSerializerMap());

    objectStreamClassMapping.getObjectStreamClassFor(objectStreamClassMapping.getMappingFor(ObjectStreamClass.lookup(specialObject.getClass())));

    specialObject = null;

    for (int i = 0; i < 10; i++) {
      if (classRef.get() == null) {
        return;
      } else {
        packHeap();
      }
    }
    throw new AssertionError();
  }

  private static void packHeap() {
    List<SoftReference<?>> packing = new ArrayList<SoftReference<?>>();
    ReferenceQueue<byte[]> queue = new ReferenceQueue<byte[]>();
    packing.add(new SoftReference<byte[]>(new byte[PACKING_UNIT], queue));
    while (queue.poll() == null) {
      packing.add(new SoftReference<byte[]>(new byte[PACKING_UNIT]));
    }
  }

  public static class SpecialClass implements Serializable {

    private static final long serialVersionUID = 1L;

    //empty impl
  }

}
