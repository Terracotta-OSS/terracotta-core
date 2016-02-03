package com.tc.classloader;

import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author vmad
 */
public class BasicCommonComponentChecker implements CommonComponentChecker {

    private final Set<String> commonClasses;

    public BasicCommonComponentChecker(Set<String> commonClasses) {
        this.commonClasses = commonClasses;
    }

    /**
     *
     * @param clazz to be checked
     * @return true if given class is a common component
     */

    @Override
    public boolean check(Class<?> clazz) {
        return this.commonClasses.contains(clazz.getName());
    }
}
