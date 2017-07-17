package org.terracotta.passthrough;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PassthroughClientDescriptorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testSerialization() throws Exception {
    File tmpFile = temporaryFolder.newFile();

    PassthroughServerProcess mockServerProcess = mock(PassthroughServerProcess.class);
    PassthroughConnection mockPassthroughConnection = mock(PassthroughConnection.class);
    when(mockPassthroughConnection.getUniqueConnectionID()).thenReturn(1L);
    PassthroughClientDescriptor actualDescriptor = new PassthroughClientDescriptor(mockServerProcess, mockPassthroughConnection, 1L);

    FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
    objectOutputStream.writeObject(actualDescriptor);
    objectOutputStream.close();

    FileInputStream fileInputStream = new FileInputStream(tmpFile);
    ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
    PassthroughClientDescriptor descriptorFromStream = (PassthroughClientDescriptor)objectInputStream.readObject();
    objectInputStream.close();

    assertThat(descriptorFromStream, is(equalTo(actualDescriptor)));
  }
}