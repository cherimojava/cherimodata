/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.entity.annotation;

/**
 * Holds information about one single Field of an Index
 *
 * @author philnate
 * @since 1.0.0
 */
public @interface IndexField
{
    /**
     * name of the field for this Index, must match the actual name used for the field in mongodb (not in java)
     */
    public String field();

    /**
     * Ordering of the Field (ascending=true, descending=false)
     */
    public Ordering order() default Ordering.ASC;

    /**
     * Ordering information for Indexed field
     *
     * @author philnate
     */
    public static enum Ordering
    {
        /**
         * Ascending Ordering
         */
        ASC, /**
              * Descending Ordering
              */
        DESC
    }
}
