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

import java.lang.reflect.Proxy;
import java.util.Map;

import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;
import org.mongodb.OrderBy;
import org.mongodb.json.JSONReader;

import com.github.cherimojava.data.mongo.entity.annotation.Collection;
import com.github.cherimojava.data.mongo.entity.annotation.Index;
import com.github.cherimojava.data.mongo.entity.annotation.IndexField;
import com.github.cherimojava.data.mongo.io.EntityCodec;
import com.github.cherimojava.data.mongo.io.EntityDecoder;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mongodb.Index.Builder;
import static org.mongodb.Index.builder;

/**
 * Utility class for working with Entity based Proxies.
 *
 * @author philnate
 */
public class EntityFactory {

	/**
	 * Where all entity for this factory will be stored. Each entity goes into it's own collection, but within the same
	 * DB
	 */
	private final MongoDatabase db;

	// TODO switch to a loading cache here
	private Map<Class<? extends Entity>, MongoCollection<? extends Entity>> preparedEntites;

	private static EntityPropertyFactory defFactory = new EntityPropertyFactory();

	public EntityFactory(MongoDatabase db) {
		preparedEntites = Maps.newConcurrentMap();
		this.db = db;
	}

	/**
	 * Creates a new Instance of a given Entity class, which holds a reference to the collection where it will be stored
	 * to. This means an invocation of Entity.save() will store the entity in the given Collection
	 *
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public <T extends Entity> T create(Class<T> clazz) {
		EntityProperties properties = defFactory.create(clazz);
		if (!preparedEntites.containsKey(clazz)) {
			prepareEntity(properties);
		}
		return instantiate(clazz, new EntityInvocationHandler(properties, preparedEntites.get(clazz)));
	}

	/**
	 * allows to load an Entity which is identified by this id, or null if no such entity was found.
	 *
	 * @param id
	 *            of the document to load
	 * @return Entity matching this id or null if no such entity was found
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T load(Class<T> clazz, Object id) {
		EntityProperties properties = defFactory.create(clazz);
		if (!preparedEntites.containsKey(clazz)) {
			prepareEntity(properties);
		}
		return EntityInvocationHandler.find((MongoCollection<T>) preparedEntites.get(clazz), id);
	}

	/**
	 * Prepares the datastructure for this Entity class in the given database, this means creating declared indexes etc.
	 */
	private synchronized void prepareEntity(final EntityProperties properties) {
		// TODO need to add verification that index field matches existing property
		Class<? extends Entity> clazz = properties.getEntityClass();
		Collection c = clazz.getAnnotation(Collection.class);
		MongoCollection<? extends Entity> coll = EntityCodec.getCollectionFor(db, properties);
		if (c != null && c.indexes() != null) {
			for (Index index : c.indexes()) {
				Builder indxBuilder = builder();
				if (index.unique()) {
					indxBuilder.unique(true);
				}
				if (isNotEmpty(index.name())) {
					indxBuilder.name(index.name());
				}

				for (IndexField field : index.value()) {
					checkNotNull(properties.getProperty(field.field()),
							"Index field '%s' for index '%s' does not exist for %s", field.field(), index.name(), clazz);
					indxBuilder.addKey(field.field(), (field.order() == IndexField.Ordering.ASC) ? OrderBy.ASC
							: OrderBy.DESC);
				}
				coll.tools().ensureIndex(indxBuilder.build());
			}
		}
		preparedEntites.put(clazz, coll);
	}

	/**
	 * Creates a new Instance of the given Entity based class, this Entity itself has no knowledge of MongoDB, so it
	 * can't be stored/dropped through it's own method. As the Entity is created static there's no DB information, thus
	 * Indexes, etc. can't be created if needed
	 *
	 * @param clazz
	 *            entity class to instantiate from
	 * @return new instance of this class
	 */
	public static <T extends Entity> T instantiate(Class<T> clazz) {
		return instantiate(clazz, new EntityInvocationHandler(defFactory.create(clazz)));
	}

	/**
	 * Returns the EntityProperties representing the given Entity. EntityProperty information generated here is
	 * statically generated and shared across all EntityFactory instances
	 *
	 * @param clazz
	 * @return
	 */
	public static EntityProperties getProperties(Class<? extends Entity> clazz) {
		return defFactory.create(clazz);
	}

	/**
	 * Creates a new instance of the given Entity based Class, allowing to provide a custom EntityInvocationHandler
	 *
	 * @param clazz
	 *            entity class to instantiate from
	 * @param handler
	 *            which is used to provide the functionality of this proxy
	 * @return new instance of this class
	 */
	@SuppressWarnings("unchecked")
	static <T extends Entity> T instantiate(Class<T> clazz, EntityInvocationHandler handler) {
		T proxy = (T) Proxy.newProxyInstance(EntityInvocationHandler.class.getClassLoader(), new Class<?>[] { clazz },
				handler);
		handler.setProxy(proxy);
		return proxy;
	}

	public MongoDatabase getDb() {
		return db;
	}

	public <T extends Entity> T fromJson(Class<T> clazz, String json) {
		return new EntityDecoder<T>(this, getProperties(clazz)).decode(new JSONReader(json));
	}
}
