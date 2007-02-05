/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.cargo;

import com.tc.net.EphemeralPorts;
import com.tc.process.StartupAppender;
import com.tc.process.StreamCollector;
import com.tc.util.ArchiveBuilder;
import com.tc.util.ClassListToFileList;
import com.tc.util.JarBuilder;
import com.tc.util.PortChooser;
import com.tc.util.ReplaceLine;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;

public abstract class CargoStartupAppender implements StartupAppender {

  public abstract void append() throws Exception;

  /**
   * @param appenderLocation - location of the subprocess startup-appender.jar
   */
  public final void pack(File appenderLocation) throws IOException {
    ArchiveBuilder jar = new JarBuilder(new File(appenderLocation + File.separator + StartupAppender.FILE_NAME));
    Class[] classes = new Class[] { this.getClass(), CargoStartupAppender.class, ArchiveBuilder.class,
        JarBuilder.class, ClassListToFileList.class, StartupAppender.class, PortChooser.class, EphemeralPorts.class,
        StreamCollector.class, Os.class, ReplaceLine.class };
    File[][] files = ClassListToFileList.translate(classes);
    for (int i = 0; i < files[0].length; i++) {
      jar.putEntry(files[1][i].toString(), jar.readFile(files[0][i]));
    }
    jar.putEntry(StartupAppender.APPENDER_TYPE_FILENAME, this.getClass().getName().getBytes());
    jar.finish();
  }
}
