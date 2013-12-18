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
package com.github.cherimojava.data.mongo.entities;

/**
 * Basic Interface for documents containing common functionality for all records, this type of entity keeps a history of
 * previously saved states.
 *
 * @author philnate
 * @see Entity
 */
interface VersionedEntity extends Entity {
	/**
	 * fieldname where the Version is stored
	 */
	static final String _V = "_v";

	/**
	 * fieldname where the History is stored
	 */
	static final String _H = "_h";

	/**
	 * allows to programatically read a given value from an entities history. Will fail if the given property isn't
	 * declared through a method name / {@link javax.inject.Named} or the version isn't valid.
	 *
	 * @param property
	 *            name to retrive
	 * @param version
	 *            of the property to read
	 * @return value of the property or null if the value isn't present.
	 * @throws IllegalArgumentException
	 *             if the property isn't declared for that entity
	 */
	public Object get(String property, int version);

	/**
	 * Two entities are considered equal if they're from the same type and all underlying properties are equal as well.
	 * This includes the history of the {@link VersionedEntity} as well.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o);

	/**
	 * HashCode is being created over all properties and their potential history. This makes the generation of the
	 * hashCode a heavy operation, so use it with caution. As Entities aren't static by design you should use them as
	 * Keys within HashMaps and other Hashing only if you're not modifying them after insertion into the data structure.
	 */
	@Override
	public int hashCode();
}
