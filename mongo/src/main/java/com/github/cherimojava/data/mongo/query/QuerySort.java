/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.query;

import com.github.cherimojava.data.mongo.entity.Entity;

/**
 * Query sorting part
 * 
 * @author philnate
 * @since 1.0.0
 */
public interface QuerySort<E extends Entity>
    extends QueryEnd<E>
{
    /**
     * Adds the given fields to the sort criteria being sorted ascending
     * 
     * @param methodCall to sort ascending
     * @return
     */
    public QuerySort<E> asc( Object... methodCall );

    /**
     * Adds the given fields to the sort criteria being sorted descending
     * 
     * @param methodCall to sort descending
     * @return
     */
    public QuerySort<E> desc( Object... methodCall );

    /**
     * Adds the given fields to the soret criteria by the given sort order. Allows for programmatic usage without too
     * much boiler plate
     * 
     * @param sortOrder
     * @param methodCall
     * @return
     */
    public QuerySort<E> by( Sort sortOrder, Object... methodCall );

    public static enum Sort
    {
        DESC, ASC
    }
}
