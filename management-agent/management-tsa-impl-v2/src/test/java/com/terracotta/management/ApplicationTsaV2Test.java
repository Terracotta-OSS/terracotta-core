package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @author: Anthony Dahanne
 */
public class ApplicationTsaV2Test extends JerseyApplicationTestCommon{
  @Test
  public void testGetClasses() throws Exception {
    ApplicationTsaV2 applicationEhCache = new ApplicationTsaV2();
    Set<Class<?>> applicationClasses = applicationEhCache.getResourceClasses();
    Set<Class<?>> annotatedClasses = annotatedClassesFound();
    Assert.assertThat(annotatedClasses, equalTo(applicationClasses));
  }
}
