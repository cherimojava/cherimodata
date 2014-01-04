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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mongodb.Document;
import org.mongodb.MongoCollection;
import org.mongodb.MongoCursor;

import com.github.cherimojava.data.mongo.io.EntityEncoder;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.*;
//TODO javadoc
//TODO add support for add

class EntityInvocationHandler implements InvocationHandler {

	/**
	 * holds the properties backing this entity class
	 */
	private final EntityProperties properties;

	private final MongoCollection collection;

	private boolean changed = false;

	private Entity proxy;

	private boolean sealed = false;

	/**
	 * holds the actual data of the Entity
	 */
	Map<String, Object> data;

	public EntityInvocationHandler(EntityProperties properties) {
		this(properties, null);
	}

	public EntityInvocationHandler(EntityProperties properties, MongoCollection collection) {
		this.properties = properties;
		data = Maps.newHashMap();
		this.collection = collection;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		ParameterProperty pp;

		switch (methodName) {
		case "get":
			// check if the mongo name is declared
			pp = properties.getProperty((String) args[0]);
			checkArgument(pp != null, "Unknown property %s, not declared for Entity %s", args[0],
					properties.getEntityClass());
			return _get(pp);// we know that this is a string param
		case "set":
			pp = properties.getProperty((String) args[0]);
			checkArgument(pp != null, "Unknown property %s, not declared for Entity %s", args[0],
					properties.getEntityClass());
			_put(pp, args[1]);
			return proxy;
		case "save":
			checkState(collection != null,
					"Entity was created without MongoDB reference. You have to save the entity through an EntityFactory");
			if (changed) {
				// TODO create for accessable Id some way to get it validated through validator
				if (properties.hasExplicitId()) {
					// TODO we can release this if it's of type ObjectId
					checkNotNull(data.get("_id"), "An explicit defined Id must be set before saving");
				}
				for (ParameterProperty cpp : properties.getValidationProperties()) {
					cpp.validate(data.get(cpp.getMongoName()));
				}
				save(this, collection);
				// change state only after successful saving to Mongo
				changed = false;
				return true;
			} else {
				return false;
			}
		case "drop":
			checkState(collection != null,
					"Entity was created without MongoDB reference. You have to drop the entity through an EntityFactory");
			drop(this, collection);
			return null;
		case "equals":
			return _equals(args[0]);
		case "seal":
			sealed = true;
			return null;
		case "entityClass":
			return properties.getEntityClass();
		case "toString":
			return _toString();
		case "hashCode":
			return _hashCode();
		case "load":
			checkState(collection != null,
					"Entity was created without MongoDB reference. You have to load entity through an EntityFactory");
			return find(collection, args[0]);
		}

		if (methodName.startsWith("get")) {
			return _get(properties.getProperty(method));
		}
		if (methodName.startsWith("set")) {
			pp = properties.getProperty(method);

			_put(pp, args[0]);
			// if we want this to be fluent we need to return this
			if (pp.isFluent()) {
				return proxy;
			}
		}

		return null;
	}

	private void _put(ParameterProperty pp, Object value) {
		checkArgument(!sealed, "Entity is sealed and does not allow further modifications");
		pp.validate(value);
		if (value != data.put(pp.getMongoName(), value)) {
			changed = true;
		}
	}

	@SuppressWarnings("unchecked")
	private Object _get(ParameterProperty property) {
		if (property.isComputed()) {
			// if this property is computed we need to calculate the value for it
			return property.getComputer().compute(proxy);
		} else {
			return data.get(property.getMongoName());
		}
	}

	/**
	 * equals method of the entity represented by this EntityInvocationHandler instance
	 *
	 * @param o
	 * @return
	 */
	private boolean _equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!Proxy.isProxyClass(o.getClass())) {
			// for all non proxies we know that we can return false
			return false;
		}
		if (!Proxy.getInvocationHandler(o).getClass().equals(getClass())) {
			// for all proxies not being EntityInvocationHandler return false
			return false;
		}
		EntityInvocationHandler handler = (EntityInvocationHandler) Proxy.getInvocationHandler(o);
		if (!handler.properties.getEntityClass().equals(properties.getEntityClass())) {
			// this is not the same entity class, so false
			return false;
		}

		return data.equals(handler.data);
	}

	/**
	 * hashCode method of the entity represented by this EntityInvocationHandler instance
	 *
	 * @return
	 */
	private int _hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder();
		for (Object key : data.values()) {
			hcb.append(key);
		}
		return hcb.build();
	}

	/**
	 * toString method of the entity represented by this EntityInvocationHandler instance
	 *
	 * @return
	 */
	private String _toString() {
		return new EntityEncoder<>(null, properties).asString(proxy);
	}

	/**
	 * stores the given EntityInvocationHandler represented Entity in the given Collection
	 *
	 * @param handler
	 * @param coll
	 */
	static <T extends Entity> void save(EntityInvocationHandler handler, MongoCollection<T> coll) {
		coll.save((T) handler.proxy);
	}

	/**
	 * removes the given EntityInvocationHandler represented Entity from the given Collection
	 *
	 * @param handler
	 * @param coll
	 */
	static <T extends Entity> void drop(EntityInvocationHandler handler, MongoCollection<T> coll) {
		// coll.drop((T)handler.proxy);
	}

	/**
	 * searches for the given Id within the MongoCollection and returns, if the id was found the corresponding entity
	 *
	 * @param collection
	 *            where the entity class is stored in
	 * @param id
	 *            of the entity to load
	 * @param <T>
	 *            Type of the entity
	 * @return returns the entity belonging to the given Id within the collection or null if no such entity exists in
	 *         the given collection
	 */
	static <T extends Entity> T find(MongoCollection<T> collection, Object id) {
		try (MongoCursor<? extends Entity> curs = collection.find(new Document(Entity.ID, id)).limit(1).iterator()) {
			return (T) ((curs.hasNext()) ? curs.next() : null);
		}
	}

	void setProxy(Entity proxy) {
		checkArgument(this.proxy == null, "Proxy for Handler can be only set once");
		this.proxy = proxy;
	}
}
