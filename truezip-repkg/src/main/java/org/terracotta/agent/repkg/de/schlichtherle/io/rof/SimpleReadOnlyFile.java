/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * SimpleReadOnlyFile.java
 *
 * Created on 14. Oktober 2005, 20:36
 */
/*
 * Copyright 2005-2007 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io.rof;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

/**
 * A {@link ReadOnlyFile} implementation using a {@link RandomAccessFile}.
 *
 * @author Christian Schlichtherle
 * @version @version@
 */
public class SimpleReadOnlyFile
        extends RandomAccessFile
        implements ReadOnlyFile
{
    public SimpleReadOnlyFile(File file)
    throws FileNotFoundException {
        super(file, "r");
    }
}
