/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.gradle.agent.init;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.messages.serviceMessages.*;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.StandardOutputListener;
import org.gradle.api.tasks.TaskState;
import org.gradle.api.tasks.compile.AbstractCompile;

/**
 * Author: Nikita.Skvortsov
 * Date: 10/26/10
 */
public class TeamcityTaskListener implements TaskExecutionListener {

  Map<Task, TeamCityErrorStore> taskMessages = new HashMap<Task, TeamCityErrorStore>();

  public void beforeExecute(Task task) {
    ServiceMessage message;
    if (task instanceof AbstractCompile) {
      task.getLogging().captureStandardError(LogLevel.WARN);
      TeamCityErrorStore errorListener = taskMessages.get(task);
      if (null == errorListener) {
        errorListener = new TeamCityErrorStore();
        taskMessages.put(task, errorListener);
      }
      errorListener.reset();
      task.getLogging().addStandardOutputListener(errorListener);
      message = new CompilationStarted(task.getPath());
    } else {
      message = new BlockOpened(task.getPath());
    }
    System.out.println(message.asString());
  }


  public void afterExecute(Task task, TaskState taskState) {
    ServiceMessage message;
    if (task instanceof AbstractCompile) {
      Throwable failure = taskState.getFailure();
      if (null != failure) {
        final TeamCityErrorStore outputListener = taskMessages.get(task);
        for(String str : outputListener.getMessages()) {
          System.out.println(new Message(str, Status.ERROR, null).asString()); // compilation failure
        }
      }
      message = new CompilationFinished(task.getPath());
    } else {
      message = new BlockClosed(task.getPath());
    }
    System.out.println(message.asString());
  }

  private class TeamCityErrorStore implements StandardOutputListener {

  private List<String> messages = new LinkedList<String>();

  public void onOutput(CharSequence chars) {
    String[] strings = chars.toString().split("(\\n|\\r)*$");
    for(String str : strings) {
      if (str.trim().length() > 0) {
        messages.add(str);
      }
    }
  }

  public List<String> getMessages () {
    return messages;
  }

  public void reset() {
    messages.clear();
  }
}

}




