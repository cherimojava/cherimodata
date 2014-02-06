/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cherimojava.data.mongo.entity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mongodb.Document;
import org.mongodb.MongoCollection;
import org.mongodb.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.io.EntityEncoder;
import com.google.common.collect.Maps;

import static com.github.cherimojava.data.mongo.entity.Entity.ID;
import static com.github.cherimojava.data.mongo.entity.EntityFactory.getDefaultClass;
import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * Proxy class doing the magic for Entity based Interfaces
 *
 * @author philnate
 * @since 1.0.0
 */
class EntityInvocationHandler implements InvocationHandler {

	private static final Logger LOG = LoggerFactory.getLogger(EntityInvocationHandler.class);

	/**
	 * holds the properties backing this entity class
	 */
	private final EntityProperties properties;

	/**
	 * Mongo Collection to which this Entity is being save. Might be null, in which case it's not possible to perform
	 * any MongoDB using operations like .save() on this entity
	 */
	private final MongoCollection collection;

	/**
	 * tells if this entity was changed after the last time it was saved/created. Thus needing to be saved or not
	 */
	private boolean changed = false;

	/**
	 * reference to the Proxy, which we're baking
	 */
	private Entity proxy;

	/**
	 * can this entity be modified or not. Obviously we can only block changes coming through setter of the entity, not
	 * for Objects already set here
	 */
	private boolean sealed = false;

	/**
	 * holds the actual data of the Entity
	 */
	Map<String, Object> data;

	/**
	 * creates a new Handler for the given EntityProperties (Entity class). No Mongo reference will be created meaning
	 * Mongo based operations like (.save()) are not supported
	 *
	 * @param properties
	 *            EntityProperties for which a new Instance shall be created
	 */
	public EntityInvocationHandler(EntityProperties properties) {
		this(properties, null);
	}

	/**
	 * creates a new Handler for the givne EntityProperties (Entity class), Entity will be saved to the given
	 * MongoCollection.
	 *
	 * @param properties
	 *            EntityProperties for which a new Instance shall be created
	 * @param collection
	 *            MongoCollection to which the entity will be persisted to
	 */
	public EntityInvocationHandler(EntityProperties properties, MongoCollection collection) {
		this.properties = properties;
		data = Maps.newHashMap();
		this.collection = collection;
	}

	/**
	 * Method which is actually invoked if a proxy method is being called. Used as dispatcher to actual methods doing
	 * the work
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		ParameterProperty pp;

		switch (methodName) {
		case "get":
			return _get(checkPropertyExists((String) args[0]));// we know that this is a string param
		case "set":
			_put(checkPropertyExists((String) args[0]), args[1]);
			return proxy;
		case "save":
			checkState(collection != null,
					"Entity was created without MongoDB reference. You have to save the entity through an EntityFactory");
			if (changed) {
				// TODO create for accessable Id some way to get it validated through validator
				if (properties.hasExplicitId()) {
					// TODO we can release this if it's of type ObjectId
					checkNotNull(data.get(ID), "An explicit defined Id must be set before saving");
				}
				for (ParameterProperty cpp : properties.getValidationProperties()) {
					cpp.validate(data.get(cpp.getMongoName()));
				}
				save(this, collection);
				// change state only after successful saving to Mongo
				changed = false;
				return true;
			} else {
				LOG.info("Did not save Entity with id {} of class {} as no changes where made.", data.get(ID),
						properties.getEntityClass());
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
			if (pp.isFluent(ParameterProperty.MethodType.SETTER)) {
				return proxy;
			}
		}
		if (methodName.startsWith("add")) {
			// for now we know that there's only one parameter
			pp = properties.getProperty(method);
			_add(pp, args[0]);
			// if we want this to be fluent we need to return this
			if (pp.isFluent(ParameterProperty.MethodType.ADDER)) {
				return proxy;
			}
		}
		return null;
	}

	/**
	 * verifies that the entity isn't sealed, if the entity is sealed no further modification is allowed and an
	 * IllegalArgumentException is thrown
	 */
	private void checkNotSealed() {
		checkArgument(!sealed, "Entity is sealed and does not allow further modification");
	}

	/**
	 * check if the given propertyName exists as Mongo property
	 *
	 * @param propertyName
	 *            mongoDB name of property to check
	 * @return ParameterProperty belonging to the given PropertyName
	 * @throws java.lang.IllegalArgumentException
	 *             if the given PropertyName isn't declared
	 */
	private ParameterProperty checkPropertyExists(String propertyName) {
		ParameterProperty pp = properties.getProperty(propertyName);
		checkArgument(pp != null, "Unknown property %s, not declared for Entity %s", propertyName,
				properties.getEntityClass());
		return pp;
	}

	@SuppressWarnings("unchecked")
	private void _add(ParameterProperty pp, Object value) {
		checkNotSealed();
		if (data.get(pp.getMongoName()) == null) {
			try {
				if (getDefaultClass(pp.getType()) != null) {
					Collection coll = (Collection) getDefaultClass(pp.getType()).newInstance();
					coll.add(value);
					data.put(pp.getMongoName(), coll);
				} else {
					throw new IllegalStateException(format(
							"Property is of interface %s, but no suitable implementation was registered", pp.getType()));
				}
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalStateException("The impossible happened. Could not instantiate Class", e);
			}
		} else {
			((Collection) data.get(pp.getMongoName())).add(value);
		}
	}

	/**
	 * Does put operation, Verifies that Entity isn't sealed and that given value matches property constraints. Sets
	 * changed to true if the given value is different from the one currently set for the property. Comparision is
	 * limited to identity
	 *
	 * @param pp
	 *            property to set
	 * @param value
	 *            new value of the property
	 * @throws java.lang.IllegalArgumentException
	 *             if the entity is sealed and doesn't allow further modifications
	 */
	private void _put(ParameterProperty pp, Object value) {
		checkNotSealed();
		pp.validate(value);
		if (value != data.put(pp.getMongoName(), value)) {
			changed = true;
		}
	}

	/**
	 * Returns the currently assigned value for the given Property or null if the property currently isn't set
	 *
	 * @param property
	 *            to get value from
	 * @return value of the property or null if the property isn't set. Might return null if the property is computed
	 *         and the computer returns null
	 */
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
	 * equals method of the entity represented by this EntityInvocationHandler instance. Objects are considered unequal
	 * (false) if o is:
	 * <ul>
	 * <li>null
	 * <li>no Proxy
	 * <li>different Proxy class
	 * <li>Different Entity class
	 * <li>Data doesn't match
	 * </ul>
	 * If all the above is false both entities are considered equal and true will be returned
	 *
	 * @param o
	 *            object to compare this instance with
	 * @return true if both objects match the before mentioned criteria otherwise false
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
	 * @return hashCode of this Entity
	 */
	private int _hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder();
		for (Object key : data.values()) {
			hcb.append(key);
		}
		return hcb.build();
	}

	/**
	 * toString method of the entity represented by this EntityInvocationHandler instance. String is JSON representation
	 * of the current state of the Entity
	 *
	 * @return JSON representation of the Entity
	 */
	private String _toString() {
		return new EntityEncoder<>(null, properties).asString(proxy);
	}

	/**
	 * stores the given EntityInvocationHandler represented Entity in the given Collection
	 *
	 * @param handler
	 *            EntityInvocationHandler (Entity) to save
	 * @param coll
	 *            MongoCollection to save entity into
	 */
	@SuppressWarnings("unchecked")
	static <T extends Entity> void save(EntityInvocationHandler handler, MongoCollection<T> coll) {
		coll.save((T) handler.proxy);
	}

	/**
	 * removes the given EntityInvocationHandler represented Entity from the given Collection
	 *
	 * @param handler
	 *            EntityInvocationHandler (Entity) to drop
	 * @param coll
	 *            MongoCollection in which this entity is saved
	 */
	static <T extends Entity> void drop(EntityInvocationHandler handler, MongoCollection<T> coll) {
		coll.find(new Document(ID, ((T) handler.proxy).get(ID))).removeOne();
	}

	/**
	 * searches for the given Id within the MongoCollection and returns, if the id was found the corresponding entity.
	 * If the entity wasn't found null will be returned
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
	@SuppressWarnings("unchecked")
	static <T extends Entity> T find(MongoCollection<T> collection, Object id) {
		try (MongoCursor<? extends Entity> curs = collection.find(new Document(Entity.ID, id)).limit(1).iterator()) {
			return (T) ((curs.hasNext()) ? curs.next() : null);
		}
	}

	/**
	 * sets the proxy this handler backs, needed for internal work
	 *
	 * @param proxy
	 */
	void setProxy(Entity proxy) {
		checkArgument(this.proxy == null, "Proxy for Handler can be only set once");
		this.proxy = proxy;
	}
}
