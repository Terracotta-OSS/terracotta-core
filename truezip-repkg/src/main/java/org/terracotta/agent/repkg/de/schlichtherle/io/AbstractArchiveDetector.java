/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * AbstractArchiveDetector.java
 *
 * Created on 23. Maerz 2006, 14:20
 */
/*
 * Copyright 2006 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io;

import java.io.FileNotFoundException;
import java.net.URI;

/**
 * Implements the {@link FileFactory} part of the {@link ArchiveDetector}
 * interface.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public abstract class AbstractArchiveDetector implements ArchiveDetector {

    public File createFile(java.io.File blueprint) {
        return new File(blueprint, this);
    }


    public File createFile(java.io.File delegate, File innerArchive) {
        return new File(delegate, innerArchive, this);
    }


    public File createFile(
            File blueprint,
            java.io.File delegate,
            File enclArchive) {
        return new File(blueprint, delegate, enclArchive);
    }


    public File createFile(java.io.File parent, String child) {
        return new File(parent, child, this);
    }


    public File createFile(String pathName) {
        return new File(pathName, this);
    }


    public File createFile(String parent, String child) {
        return new File(parent, child, this);
    }


    public File createFile(URI uri) {
        return new File(uri, this);
    }


    public FileInputStream createFileInputStream(java.io.File file)
    throws FileNotFoundException {
        return new FileInputStream(file);
    }


    public FileOutputStream createFileOutputStream(
            java.io.File file,
            boolean append)
    throws FileNotFoundException {
        return new FileOutputStream(file, append);
    }
}
