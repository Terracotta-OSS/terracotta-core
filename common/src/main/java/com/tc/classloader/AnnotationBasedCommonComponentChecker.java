package com.tc.classloader;

/**
 * @author vmad
 */
public class AnnotationBasedCommonComponentChecker implements CommonComponentChecker {

    /**
     *
     * @param clazz to be checked
     * @return true if given class is a common component
     */

    @Override
    public boolean check(Class<?> clazz) {
        return clazz.getAnnotation(CommonComponent.class) != null;
    }
}
