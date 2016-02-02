package com.tc.classloader;

import com.tc.classloader.CommonComponent;

/**
 * @author vmad
 */
public class AnnotationOrDirectoryStrategyChecker implements CommonComponentChecker {
    /**
     * @param clazz to be checked
     * @return true if given class is a common component
     */
    @Override
    public boolean check(Class<?> clazz) {
        if(clazz.getClassLoader() instanceof ApiClassLoader) {
            return true;
        } else if(clazz.getAnnotation(CommonComponent.class) != null) {
            return true;
        } else {
            return false;
        }
    }
}
