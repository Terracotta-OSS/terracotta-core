/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Temps.java
 *
 * Created on 2. April 2007, 19:25
 */
/*
 * Copyright 2007 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.agent.repkg.de.schlichtherle.io.util;

import java.io.*;

/**
 * A utility class for creating temporary files.
 * This class allows to change the directory for temporary files via the class
 * property <code>directory</code>.
 * If the value of this property is <code>null</code> (which is the default),
 * the value of the system property <code>java.io.tmpdir</code> is used.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.6
 */
public class Temps {

    private static File directory;

    /** You can't instantiate this class. */
    protected Temps() {
    }

    /**
     * Returns the directory for temporary files.
     * By default, this method returns <code>null</code>, which means that
     * the directory used for {@link #createTempFile} is determined by the
     * system property <code>java.io.tmpdir</code>. 
     */
    public static File getDirectory() {
        return directory;
    }

    /**
     * Sets the directory for temporary files.
     * If this is <code>null</code>, the value of the system property
     * <code>java.io.tmpdir</code> is used by {@link #createTempFile}.
     */
    public static void setDirectory(final File directory) {
        Temps.directory = directory;
    }

    /**
     * Like {@link java.io.File#createTempFile}, but uses the value of the
     * class property <code>directory</code> as the directory for temporary
     * files.
     * If the value of this property is <code>null</code>, the directory is
     * determined by the value of the system property
     * <code>java.io.tmpdir</code>.
     * 
     * @see #getDirectory
     * @see #setDirectory
     */
    public static final File createTempFile(
            final String prefix,
            final String suffix)
    throws IOException {
        return File.createTempFile(prefix, suffix, directory);
    }

    /**
     * Like {@link #createTempFile(String, String)}, but uses the default
     * suffix {@code ".tmp"}.
     * 
     * @see #getDirectory
     * @see #setDirectory
     */
    public static final File createTempFile(final String prefix)
    throws IOException {
        return File.createTempFile(prefix, null, directory);
    }
}
