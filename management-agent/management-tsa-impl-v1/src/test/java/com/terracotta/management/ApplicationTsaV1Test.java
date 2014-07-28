package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @author: Anthony Dahanne
 */
public class ApplicationTsaV1Test  extends JerseyApplicationTestCommon {
  @Test
  public void testGetClasses() throws Exception {
    ApplicationTsaV1 applicationEhCache = new ApplicationTsaV1();
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
          fail("Should  " + annotatedClass + " be added to ApplicationTsaV1 ?");
        }
      }
    }
    Assert.assertThat(annotatedClasses, equalTo(filteredApplicationClasses));
  }

}
