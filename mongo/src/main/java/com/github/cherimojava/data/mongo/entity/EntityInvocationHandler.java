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

import static com.github.cherimojava.data.mongo.entity.Entity.ID;
import static com.github.cherimojava.data.mongo.entity.EntityFactory.getDefaultClass;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.io.EntityCodec;
import com.google.common.base.Defaults;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

/**
 * Proxy class doing the magic for Entity based Interfaces
 *
 * @author philnate
 * @since 1.0.0
 */
class EntityInvocationHandler implements InvocationHandler {

	private static final Logger LOG = LoggerFactory.getLogger(EntityInvocationHandler.class);

	// TODO should be its own class
	/* registry containing information about codecs for encoding ids */
	private static CodecRegistry idRegistry = CodecRegistries.fromProviders(new ValueCodecProvider());

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
	 * reference to the Proxy, which we're baking
	 */
	private Entity proxy;

	/**
	 * can this entity be modified or not. Obviously we can only block changes coming through setter of the entity, not
	 * for Objects already set here
	 */
	private boolean sealed = false;

	/**
	 * was this object already saved or not
	 */
	boolean persisted = false;

	/**
	 * tells if this entity is lazy loaded or not.
	 */
	private boolean lazy = false;

	/**
	 * will be true if the entity is in the process of being saved, false otherwise
	 */
	private volatile boolean saving = false;

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
	 * creates a new handler and marks it as lazy. Only setting the id. Everything else will be loaded once an
	 * interaction with this object happens
	 * 
	 * @param properties
	 * @param collection
	 * @param id
	 */
	public EntityInvocationHandler(EntityProperties properties, MongoCollection collection, Object id) {
		this(properties, collection);
		lazy = true;
		_put(properties.getProperty(Entity.ID), id);
	}

	/**
	 * actual method which is invoked once the lazy entity is about to be filled with life
	 */
	private void lazyLoad() {
		if (lazy) {
			data = ((EntityInvocationHandler) Proxy.getInvocationHandler(find(collection, data.get(ID)))).data;
			lazy = false;
		}
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
			pp = checkPropertyExists((String) args[0]);
			if (!ID.equals(pp.getMongoName())) {
				// lazy loading isn't needed for the ID itself
				lazyLoad();
			}
			return _get(pp);// we know that this is a string param
		case "set":
			lazyLoad();
			_put(checkPropertyExists((String) args[0]), args[1]);
			return proxy;
		case "save":
			checkState(collection != null,
					"Entity was created without MongoDB reference. You have to save the entity through an EntityFactory");
			lazyLoad();
			if (!saving) {
				saving = true;// mark that we're about to save to break potential cycles
				// TODO create for accessable Id some way to get it validated through validator
				if (properties.hasExplicitId()) {
					// TODO we can release this if it's of type ObjectId
					checkNotNull(data.get(ID), "An explicit defined Id must be set before saving");
				}
				save(this, collection);
				// change state only after successful saving to Mongo
				saving = false;// we're done with saving next one, can write object. Which isn't coming from within this
								// instance
				return true;
			} else {
				LOG.info("Did not save Entity with id {} of class {} as it's cyclic called.", data.get(ID),
						properties.getEntityClass());
				return false;
			}
		case "drop":
			checkState(collection != null,
					"Entity was created without MongoDB reference. You have to drop the entity through an EntityFactory");
			drop(this, collection);
			return null;
		case "equals":
			lazyLoad();
			return _equals(args[0]);
		case "seal":
			sealed = true;
			return null;
		case "entityClass":
			return properties.getEntityClass();
		case "toString":
			lazyLoad();
			return _toString();
		case "hashCode":
			lazyLoad();
			return _hashCode();
		case "load":
			checkState(collection != null,
					"Entity was created without MongoDB reference. You have to load entities through an EntityFactory");
			return find(collection, args[0]);
		}

		lazyLoad();
		pp = properties.getProperty(method);
		if (methodName.startsWith("get") || methodName.startsWith("is")) {
			return _get(pp);
		}
		if (methodName.startsWith("set")) {
			_put(pp, args[0]);
			// if we want this to be fluent we need to return this
			if (pp.isFluent(ParameterProperty.MethodType.SETTER)) {
				return proxy;
			}
		}
		if (methodName.startsWith("add")) {
			// for now we know that there's only one parameter
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
	 * verifies that the given property isn't final and if it's that the entity wasn't saved yet.
	 * 
	 * @param pp
	 *            parameterproperty to check for final
	 */
	private void checkNotFinal(ParameterProperty pp) {
		if (pp.isFinal()) {
			checkState(!persisted, "Entity was already saved, can't modify value of @Final property later on");
		}
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
					data.put(pp.getMongoName(), coll);
				} else {
					throw new IllegalStateException(format(
							"Property is of interface %s, but no suitable implementation was registered", pp.getType()));
				}
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalStateException("The impossible happened. Could not instantiate Class", e);
			}
		}
		if (!value.getClass().isArray()) {
			((Collection) data.get(pp.getMongoName())).add(value);
		} else {
			for (Object val : (Object[]) value) {
				((Collection) data.get(pp.getMongoName())).add(val);
			}
		}
	}

	/**
	 * Does put operation, Verifies that Entity isn't sealed and that given value matches property constraints.
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
		checkNotFinal(pp);
		pp.validate(value);
		data.put(pp.getMongoName(), value);
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
			String name = property.getMongoName();
			// add default value, in case nothing has been set yet
			if (!data.containsKey(name) && property.isPrimitiveType()) {
				data.put(name, Defaults.defaultValue(Primitives.unwrap(property.getType())));
			}
			return data.get(name);
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
		InvocationHandler ihandler = Proxy.getInvocationHandler(o);
		if (!ihandler.getClass().equals(getClass())) {
			// for all proxies not being EntityInvocationHandler return false
			return false;
		}
		EntityInvocationHandler handler = (EntityInvocationHandler) ihandler;
		if (!handler.properties.getEntityClass().equals(properties.getEntityClass())) {
			// this is not the same entity class, so false
			return false;
		}
		// make sure both have all lazy dependencies resolved
		lazyLoad();
		handler.lazyLoad();
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
		return new EntityCodec<>(null, properties).asString(proxy);
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
		for (ParameterProperty cpp : handler.properties.getValidationProperties()) {
			cpp.validate(handler.data.get(cpp.getMongoName()));
		}
		BsonDocumentWrapper wrapper = new BsonDocumentWrapper<>(handler.proxy,
				(org.bson.codecs.Encoder<Entity>) coll.getCodecRegistry().get(handler.properties.getEntityClass()));
		UpdateResult res = coll.updateOne(
				new BsonDocument("_id", BsonDocumentWrapper.asBsonDocument(EntityCodec._obtainId(handler.proxy),
						idRegistry)), new BsonDocument("$set", wrapper), new UpdateOptions());
		if (res.getMatchedCount() == 0) {
			// TODO this seems too nasty, there must be a better way.for now live with it
			coll.insertOne((T) handler.proxy);
		}
		handler.persist();
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
		coll.findOneAndDelete(new Document(ID, (handler.proxy).get(ID)));
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
	 * returns the {@link com.github.cherimojava.data.mongo.entity.EntityInvocationHandler} of the given entity
	 * 
	 * @param e
	 *            entity to retrieve handler from
	 * @return EntityInvocationHandler the entity is baked by
	 */
	public static EntityInvocationHandler getHandler(Entity e) {
		return (EntityInvocationHandler) Proxy.getInvocationHandler(checkNotNull(e));
	}

	/**
	 * sets the proxy this handler backs, needed for internal work
	 *
	 * @param proxy
	 *            this handler is for, allows internal component to get access to outside view of itself
	 */
	void setProxy(Entity proxy) {
		checkArgument(this.proxy == null, "Proxy for Handler can be only set once");
		this.proxy = proxy;
	}

	/**
	 * marks that the given entity is persisted
	 */
	public void persist() {
		persisted = true;
	}
}
