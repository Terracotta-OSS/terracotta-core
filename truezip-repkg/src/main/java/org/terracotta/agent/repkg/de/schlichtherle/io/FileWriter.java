/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * FileWriter.java
 *
 * Created on 8. November 2006, 19:42
 */
/*
 * Copyright 2006-2007 Schlichtherle IT Services
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

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * A drop-in replacement for {@link java.io.FileWriter} which provides
 * transparent write access to archive entries as if they were (virtual) files.
 * All file system operations in this class are
 * <a href="package-summary.html#atomicity">virtually atomic</a>.
 *
 * @see <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see FileBusyException
 * @see File#cat
 * @see File#umount
 * @see File#update
 * @see File#setLenient
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */

public class FileWriter extends OutputStreamWriter {

    public FileWriter(String path) throws IOException {
  super(new FileOutputStream(path));
    }

    public FileWriter(String path, boolean append) throws IOException {
  super(new FileOutputStream(path, append));
    }

    public FileWriter(File file) throws IOException {
  super(new FileOutputStream(file));
    }

    public FileWriter(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append));
    }

    public FileWriter(FileDescriptor fd) {
  super(new FileOutputStream(fd));
    }
}
