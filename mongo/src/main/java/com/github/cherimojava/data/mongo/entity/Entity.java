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
package com.github.cherimojava.data.mongo.entity;

/**
 * Basic Interface for documents containing common functionality for all records. If the Document needs to be versioned
 * one must use {@link VersionedEntity}. If there's a a setter/Getter method called setId/getId will be used as Id for
 * this document. If no explicit id is declared one will be set on first save(only if something else was changed)
 *
 * @author philnate
 */
// TODO add transactional support
public interface Entity<T extends Entity> {
	public interface Special {
	}

	/**
	 * id fieldname
	 */
	static final String ID = "_id";

	/**
	 * Saves the entity to MongoDB. Saving only happens if the entity has unsaved changes.
	 *
	 * @return boolean which tells if the document was saved or not(if there were changes which needed to be saved or
	 *         not)
	 * @throws IllegalStateException
	 *             if no MongoDB instance is linked to this Entity
	 */
	public boolean save();

	/**
	 * allows to programatically read a given value from an entity. Will fail if the given property isn't declared
	 * through a method name /{@link javax.inject.Named}.
	 *
	 * @param property
	 *            name of property to read (Naming as it will be used within MongoDB)
	 * @return value of the property or null if the value isn't present.
	 * @throws IllegalArgumentException
	 *             if the property isn't declared for that entity
	 */
	public Object get(String property);

	/**
	 * allows to programmatically set a given property for an entity. Will fail if the property isn't declared either
	 * through an explicit set method or {@link javax.inject.Named} annotation on a set method
	 *
	 * @param property
	 *            name of property to write (Naming as it will be used within MongoDB)
	 * @param value
	 *            to write
	 * @throws IllegalArgumentException
	 *             if the property isn't declared for that entity
	 */
	public T set(String property, Object value);

	/**
	 * Removes the entity from the database, but doesn't remove the entity data or state.
	 *
	 * @throws IllegalStateException
	 *             if no MongoDB instance is linked to this Entity
	 */
	public void drop();

	/**
	 * Seals the Entity, so that no further modifications can be done to it. If you have references to stored mutable
	 * objects, you still can modify them
	 */
	public void seal();

	/**
	 * Loads the Entity which corresponds to the given id. Returns the entity belonging to the given Id or null if no
	 * such entity exists
	 *
	 * @param id
	 *            which identifies the Entity to load. Corresponds to the _id field
	 * @return Entity belonging to the given id or null if no such entity exists
	 */
	public T load(Object id);

	/**
	 * Two entity are considered equal if they're from the same type and all underlying properties are equal as well.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o);

	/**
	 * HashCode is being created over all set properties. This makes the generation of the hashCode a heavy operation,
	 * so use it with caution. As Entities aren't static by design you should use them as Keys within HashMaps and other
	 * Hashing only if you're not modifying them after insertion into the data structure.
	 */
	@Override
	public int hashCode();

	/**
	 * Returns the class of this entity as getClass will return the Proxy class rather than the entity class
	 *
	 * @return
	 */
	public Class<T> entityClass();
}
