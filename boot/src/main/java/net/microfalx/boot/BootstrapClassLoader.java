package net.microfalx.boot;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A class loader which loads
 */
public class BootstrapClassLoader extends URLClassLoader {

    public BootstrapClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}
