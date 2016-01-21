package com.tc.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author vmad
 */
public class ApiClassLoader extends URLClassLoader {
    public ApiClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if(clazz == null) {
            // try to find the class using given jars first,
            // if not found, try loading using parent classloader.
            try {
                clazz = findClass(name);
            } catch (ClassNotFoundException ignore) {
                clazz = getParent().loadClass(name);
            }
        }

        if(clazz != null && resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }
}
