package com.tc.classloader;

/**
 * @author vmad
 */
public interface CommonComponentChecker {
    /**
     *
     * @param clazz to be checked
     * @return true if given class is a common component
     */
    boolean check(Class<?> clazz);
}
