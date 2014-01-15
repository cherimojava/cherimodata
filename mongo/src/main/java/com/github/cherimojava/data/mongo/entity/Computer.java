/**
 *    Copyright [cherimojava (http://github.com/cherimojava/cherimodata.git)]
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
 */
package com.github.cherimojava.data.mongo.entity;

/**
 * Function which is used to compute a value on request from the given entity F.
 *
 * @param <F>
 *            Entity this computer is getting as input
 * @param <T>
 *            Type of the value this computer computes
 * @author philnate
 * @since 1.0.0
 */
public interface Computer<F extends Entity, T> {
	/**
	 * Computes the attribute value based on the entity handed over
	 *
	 * @param f
	 *            Entity instance for which the property of type T is computed
	 * @return value for the Property based on given Entity
	 */
	public T compute(F f);
}
