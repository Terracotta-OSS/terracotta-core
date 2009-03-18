/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.injection.annotations;

import com.tc.injection.exceptions.UnsupportedInjectedDsoInstanceTypeException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field has to be injected with an instance by Terracotta DSO.
 * <p>
 * Note that for this to work, the field's class also has to be included in the instrumented classes.
 * <p>
 * By default, the type of the injected instance will be determined by the type of the field. If the type isn't
 * supported by DSO, an {@link UnsupportedInjectedDsoInstanceTypeException} exception will be thrown when classes are
 * being instrumented.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectedDsoInstance {
  /**
   * Specifies a custom field type for injection.
   */
  String value() default "";
}