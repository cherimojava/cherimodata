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
 * Tells that this property isn't inlined into the document, instead only a reference will be stored. The nested entity
 * will be stored within the same database as the parent entity.
 *
 * @author philnate
 * @since 1.0.0
 */
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface Reference
{
    /**
     * Toggles if this property will be resolved to the actual entity on load time of the parent entity or on the first
     * access to this property. Default is true, which means that entity is loaded once data is requested not included
     * in the parent entity
     */
    public boolean lazy() default true;

    // TODO implement this
    /**
     * MongoDB name of attributes to keep directly with the reference. This allows that not the whole record needs to be
     * read in order to answer this request. Can only be used with {@link #lazy()} true. Should be only used for fields
     * which change very infrequently, as there's no auto update.
     */
    public String[]includeFields() default {};

    /**
     * Defines if the entity should be stored as full mongodb DBRef or just the simple id. Default only the Id will be
     * included
     * 
     * @return
     */
    public boolean asDBRef() default false;
}
