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

