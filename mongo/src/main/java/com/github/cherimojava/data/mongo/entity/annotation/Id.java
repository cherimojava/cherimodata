/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cherimojava.data.mongo.entity.annotation;

import java.lang.annotation.*;

/**
 * Simple Marker Annotation denoting the id field if it's not named Id. Cannot be combined with a property which is
 * annotated <b>@Named("_id")</b>. Once the id is stored it can't be changed later on
 *
 * @author philnate
 * @since 1.0.0
 * @see Final
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Final
public @interface Id {
	// TODO inherit from final, so that an exception trying to update this is thrown early
}
