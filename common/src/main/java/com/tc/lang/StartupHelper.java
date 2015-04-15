/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.lang;

import com.tc.util.runtime.Vm;

import java.lang.reflect.Field;

/**
 * The purpose of this class to execute a startup action (ie. "start the server", or "start the client", etc) in the
 * specified thread group. The side effect of doing this is that any more threads spawned by the startup action will
 * inherit the given thread group. It is somewhat fragile, and sometimes impossible (see java.util.Timer) to be explicit
 * about the thread group when spawning threads <br>
 * <br>
 * XXX: At the moment, this class uses a hack of adjusting the current thread's group to the desired target group. A
 * nicer approach would be to start a new thread in the desiered target group and run the action in that thread and
 * join() it, except that can introduce locking problems (see MNK-65)
 */
public class StartupHelper {

  private final StartupAction action;
  private final ThreadGroup   targetThreadGroup;

  public StartupHelper(ThreadGroup threadGroup, StartupAction action) {
    this.targetThreadGroup = threadGroup;
    this.action = action;
  }

  public void startUp() {
    Thread currentThread = Thread.currentThread();
    ThreadGroup origThreadGroup = currentThread.getThreadGroup();

    setThreadGroup(currentThread, targetThreadGroup);

    try {
      action.execute();
    } catch (Throwable t) {
      targetThreadGroup.uncaughtException(currentThread, t);
      throw new RuntimeException(t);
    } finally {
      setThreadGroup(currentThread, origThreadGroup);
    }
  }

  public interface StartupAction {
    void execute() throws Throwable;
  }

   private static void setThreadGroup(Thread thread, ThreadGroup group) {
    String fieldName = "group";
    Class c = Thread.class;
    if(Vm.isIBM() && isFieldPresent("threadGroup",c)){
      fieldName = "threadGroup";
    }    
    try {
      Field groupField = c.getDeclaredField(fieldName);
      groupField.setAccessible(true);
      groupField.set(thread, group);
    } catch (Exception e) {
      if (e instanceof RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }
  }
  
  private static boolean isFieldPresent(String fieldName,Class targetClass){
    boolean found = false; 
    Field[] fields = targetClass.getDeclaredFields();
    for(Field field : fields){
      if(field.getName().equals(fieldName)){
        found = true;
        break;
      }
    }
    return found;
  }
}
