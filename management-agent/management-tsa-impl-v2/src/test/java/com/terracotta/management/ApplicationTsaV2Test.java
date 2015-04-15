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
package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @author: Anthony Dahanne
 */
public class ApplicationTsaV2Test  extends JerseyApplicationTestCommon {
  @Test
  public void testGetClasses() throws Exception {
    ApplicationTsaV2 applicationEhCache = new ApplicationTsaV2();
    Set<Class<?>> filteredApplicationClasses = filterClassesFromJaxRSPackages(applicationEhCache.getResourceClasses());
    Set<Class<?>> annotatedClasses = annotatedClassesFound();
    if (filteredApplicationClasses.size() > annotatedClasses.size()) {
      for (Class<?> applicationClass : filteredApplicationClasses) {
        if(!annotatedClasses.contains(applicationClass)) {
          fail("While scanning the classpath, we could not find " + applicationClass);
        }
      }
    } else {
      for (Class<?> annotatedClass : annotatedClasses) {
        if(!filteredApplicationClasses.contains(annotatedClass)) {
          fail("Should  " + annotatedClass + " be added to ApplicationTsaV2 ?");
        }
      }
    }
    Assert.assertThat(annotatedClasses, equalTo(filteredApplicationClasses));
  }

}

