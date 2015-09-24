/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.classloader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Tagging a class with this annotation will force the ComponentURLClassloader not load
 *  the tagged class and force the parent class loader to load the class or interface
 *  @see com.tc.classloader.ComponentURLClassLoader#loadClass(java.lang.String, boolean) 
 */
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
public @interface CommonComponent {
  
}
