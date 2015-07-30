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
 * Allows to specify what the property is expected to be set to
 * 
 * @author philnate
 * @param <V> ValueType of the property currently queried
 * @param <T> Entity being queried
 * @since 1.0.0
 */
public interface QuerySpecifier<V, T extends Entity>
{
    /**
     * property must match the given value
     * 
     * @param value
     * @return OngoingQuery for further query building
     */
    public OngoingQuery<T> is( V value );

    /**
     * checks that the given property is between the lower and upper bound
     * 
     * @param lower bound of property value interval requested
     * @param upper bound of property value interval requested
     * @param <N>
     * @return
     */
    public <N extends Number> OngoingQuery<T> between( N lower, N upper );

    /**
     * checks that the given property is less than the given number
     * 
     * @param number being upper exclusive bound of property being checked
     * @param <N>
     * @return
     */
    public <N extends Number> OngoingQuery<T> lessThan( N number );

    /**
     * checks that the given property is less than equal the given number
     * 
     * @param number being the upper inclusive bound of property being checked
     * @param <N>
     * @return
     */
    public <N extends Number> OngoingQuery<T> lessThanEqual( N number );

    /**
     * checks that the given property is greater than the given number
     * 
     * @param number being the lower exclusive bound of the property being checked
     * @param <N>
     * @return
     */
    public <N extends Number> OngoingQuery<T> greaterThan( N number );

    /**
     * checks that the given property is greater than equal the given number
     * 
     * @param number being the lower inclusive bound of the property being checked
     * @param <N>
     * @return
     */
    public <N extends Number> OngoingQuery<T> greaterThanEqual( N number );

    /**
     * checks that the given property is in the set of values given
     * 
     * @param values possible values the property should have
     * @return
     */
    public OngoingQuery<T> in( V... values );
}
