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
import com.mongodb.client.MongoCursor;

/**
 * Conclusion part of query building
 * 
 * @author philnate
 * @since 1.0.0
 * @param <E>
 */
public interface QueryEnd<E extends Entity>
{
    /**
     * runs the query and returns the cursor of the result set
     *
     * @return MongoCursor iterating through the result set
     */
    public MongoCursor<E> iterator();

    /**
     * count how many entities match the given record
     * 
     * @return number of matches
     */
    public long count();

    /**
     * limit the number of results returned to the given number
     * 
     * @param limit max number of results to return
     */
    public QueryEnd<E> limit( int limit );

    /**
     * number of results to skip
     * 
     * @param skip number of items to skip
     */
    public QueryEnd<E> skip( int skip );

    /**
     * begin sort configuration of query.
     * 
     * @return
     */
    public QuerySort<E> sort();
}
