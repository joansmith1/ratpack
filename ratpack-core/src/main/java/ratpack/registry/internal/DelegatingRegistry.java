/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.registry.internal;

import com.google.common.reflect.TypeToken;
import ratpack.registry.Registry;

import java.util.Optional;

public interface DelegatingRegistry extends Registry {

  Registry getDelegate();

  @Override
  default <O> Optional<O> maybeGet(TypeToken<O> type) {
    return getDelegate().maybeGet(type);
  }

  @Override
  default <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return getDelegate().getAll(type);
  }

}
