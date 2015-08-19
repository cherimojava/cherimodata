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
 * Starting point for fluent API query building for Entities
 * 
 * @author philnate
 * @param <E> Entity being queries
 */
public interface QueryStart<E extends Entity>
{
    /**
     * Start point to build a query
     * 
     * @param methodCall
     * @param <T>
     * @return
     */
    public <T> QuerySpecifier<T, E> where( T methodCall );

    /**
     * gives access to a simple entity proxy recoding which property is currently queried
     * 
     * @return
     */
    public E e();
}
