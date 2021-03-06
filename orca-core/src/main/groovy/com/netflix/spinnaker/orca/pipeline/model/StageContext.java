/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.pipeline.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ForwardingMap;
import static com.netflix.spinnaker.orca.pipeline.model.Execution.ExecutionType.PIPELINE;
import static java.util.stream.Collectors.toList;

public class StageContext extends ForwardingMap<String, Object> {

  private final Stage stage;
  private final Map<String, Object> delegate;

  public StageContext(Stage stage) {
    this(stage, new HashMap<>());
  }

  public StageContext(StageContext stageContext) {
    this(stageContext.stage, new HashMap<>(stageContext.delegate));
  }

  public StageContext(Stage stage, Map<String, Object> delegate) {
    this.stage = stage;
    this.delegate = delegate;
  }

  @Override protected Map<String, Object> delegate() {
    return delegate;
  }

  private Map<String, Object> getTrigger() {
    Execution execution = stage.getExecution();
    if (execution.getType() == PIPELINE) {
      return execution.getTrigger();
    } else {
      return Collections.emptyMap();
    }
  }

  @Override public Object get(@Nullable Object key) {
    if (delegate().containsKey(key)) {
      return super.get(key);
    } else {
      return stage
        .ancestors()
        .stream()
        .filter(it -> it.getOutputs().containsKey(key))
        .findFirst()
        .map(it -> it.getOutputs().get(key))
        .orElse(null);
    }
  }

  /*
   * Gets all objects matching 'key', sorted by proximity to the current stage.
   * If the key exists in the current context, it will be the first element returned
   */
  public List<Object> getAll(Object key) {
    List<Object> result = stage
      .ancestors()
      .stream()
      .filter(it -> it.getOutputs().containsKey(key))
      .map(it -> it.getOutputs().get(key))
      .collect(toList());

    if (delegate.containsKey(key)) {
      result.add(0, delegate.get(key));
    }

    Map<String, Object> trigger = getTrigger();
    if (trigger.containsKey(key)) {
      result.add(trigger.get(key));
    }

    return result;
  }
}
