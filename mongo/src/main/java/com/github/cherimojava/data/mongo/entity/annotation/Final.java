/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation denotes that this value can be modified only until it was saved, and is afterwards immutable. For the time
 * being this property is only supported for primitive types and their Object counterparts and
 * {@link org.bson.types.ObjectId}, as otherwise it can't be enforced that the entity stays as is.
 * 
 * @author philnate
 * @since 1.0.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.ANNOTATION_TYPE } )
public @interface Final
{
    /*
     * TODO could probably implement it, in such a way that final methods are not submitted for update. This would then
     * allow that arbitrary stuff could be written, though at runtime it could be changed but not modified. Or put on
     * readtime a proxy around the object and deny all writes( deep)
     */
}
