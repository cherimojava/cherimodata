/**
 *    Copyright [cherimojava (http://github.com/philnate/cherimojava.git)]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */
package me.philnate.cherimodata.mongo.entities.annotations;

import java.lang.annotation.*;

/**
 * Simple Marker Annotation denoting the id field if it's not named Id. Cannot be combined with a property which is annotated <b>@Named("_id")</b>.
 *
 * @author philnate
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Id {
}
