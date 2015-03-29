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
package com.github.cherimojava.data.mongo.io;

import static com.github.cherimojava.data.mongo.entity.Entity.ID;
import static java.lang.String.format;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriter;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.github.cherimojava.data.mongo.entity.EntityProperties;
import com.github.cherimojava.data.mongo.entity.EntityUtils;
import com.github.cherimojava.data.mongo.entity.ParameterProperty;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Codec which transforms Entity instances from/to JSON
 *
 * @param <T>
 *            Entity class this Codec takes care of
 * @author philnate
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public class EntityCodec<T extends Entity> implements CollectibleCodec<T> {
	private final Class<T> clazz;
	private final EntityFactory factory;
	private final CodecRegistry codecRegistry;
	private static final Logger LOG = LoggerFactory.getLogger(EntityCodec.class);
	private final MongoDatabase db;

	public EntityCodec(MongoDatabase db, EntityProperties properties) {
		clazz = (Class<T>) properties.getEntityClass();
		this.db = db;
		factory = new EntityFactory(db);
		codecRegistry = EntityCodecProvider.createCodecRegistry(db, clazz);
	}

	/**
	 * Creates a MongoCollection which has a EntityCodec attached to it
	 *
	 * @param db
	 * @param properties
	 * @return
	 */
	public static MongoCollection<? extends Entity> getCollectionFor(MongoDatabase db, EntityProperties properties) {
		return db.getCollection(properties.getCollectionName()).withDefaultClass(properties.getEntityClass()).withCodecRegistry(
				EntityCodecProvider.createCodecRegistry(db, properties.getEntityClass()));
	}

	/*
	 * CollectibleCodec stuff
	 */

	@Override
	public void generateIdIfAbsentFromDocument(T document) {
		_obtainId(document);
	}

	@Override
	public boolean documentHasId(T document) {
		return (getDocumentId(document) != null);
	}

	@Override
	public BsonValue getDocumentId(T document) {
		Object value = _getId(document);
		if (value != null) {
			if (ObjectId.class.isInstance(value)) {
				return new BsonObjectId((ObjectId) value);
			} else if (String.class.isInstance(value)) {
				return new BsonString((String) value);
			}
		}
		return null;
	}

	/**
	 * returns the document id of the document or null if no id exists
	 *
	 * @param document
	 * @param <T>
	 * @return
	 */
	public static <T extends Entity> Object _getId(T document) {
		return document.get(Entity.ID);
	}

	/**
	 * returns the document id of the given document. If there's currently no id defined one will be created
	 *
	 * @param document
	 *            document to obtain document id from
	 * @return document id of the document (might be created during method invocation)
	 */
	public static <T extends Entity> Object _obtainId(T document) {
		Object id = _getId(document);

		if (id == null) {
			id = new ObjectId();
			document.set(Entity.ID, id);
		}
		return id;
	}

	/*
	 * Decoder Stuff
	 */
	private static final EntityTypeMap typeMap = new EntityTypeMap();

	@Override
	public T decode(BsonReader reader, DecoderContext ctx) {
		return decodeEntity(reader, clazz);
	}

	private <E extends Entity> E decodeEntity(BsonReader reader, Class<E> clazz) {
		E e = factory.create(clazz);
		EntityProperties properties = EntityFactory.getProperties(clazz);
		reader.readStartDocument();
		BsonType type;
		while ((type = reader.readBsonType()) != BsonType.END_OF_DOCUMENT) {
			if (type == BsonType.DOCUMENT) {
				String name = reader.readName();
				ParameterProperty pp = properties.getProperty(name);
				if (pp == null) {
					LOG.debug("Found subdocument named {}, but this subdocument isn't known for Entity {}", name,
							clazz.getSimpleName());
					reader.skipValue();
					continue;
				}

				EntityProperties seProperties = getEntityProperties(clazz, name);
				if (pp.isReference()) {
					// Entity is only stored as reference, so we can only read the id from it
					reader.readStartDocument();
					// read the references collection, but we know where the reference belongs to, so discard
					reader.readString("$ref");
					reader.readName("$id");
					e.set(name, getSubEntity(seProperties, pp, reader));
					reader.readEndDocument();
				} else {
					e.set(name, decodeEntity(reader, seProperties.getEntityClass()));
				}
			} else {
				String propertyName = reader.readName();
				ParameterProperty pp = properties.getProperty(propertyName);
				if (pp == null) {
					LOG.debug("Found property named {}, but this property isn't known for Entity {}", propertyName,
							clazz.getSimpleName());
					reader.skipValue();
					continue;
				}
				if (pp.isReference()) {
					// later one should never be true
					if (!pp.isCollection()) {
						EntityProperties seProperties = getEntityProperties(clazz, propertyName);
						e.set(propertyName, getSubEntity(seProperties, pp, reader));
					} else {
						EntityProperties seProperties = EntityFactory.getProperties((Class<? extends Entity>) pp.getGenericType());
						reader.readStartArray();
						Collection<E> coll = getNewCollection(pp.getType());
						while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
							if (pp.isDBRef()) {
								reader.readStartDocument();
								reader.readString("$ref");
								reader.readName();
								coll.add((E) getSubEntity(seProperties, pp, reader));
								reader.readEndDocument();
							} else {
								coll.add((E) getSubEntity(seProperties, pp, reader));
							}
						}
						e.set(propertyName, coll);
						reader.readEndArray();
					}
					continue;
				}
				if (pp.isTransient() || pp.isComputed()) {
					// transient values aren't read, even tough they're written (by earlier version of Entity, etc.)
					// same is true for computed, even tough they're written it's value won't be used, so skip it
					reader.skipValue();// send value to /dev/null
					continue;
				}
				if (pp.getType().isEnum()) {
					String enumString = reader.readString();
					try {
						e.set(propertyName, Enum.valueOf((Class<? extends Enum>) pp.getType(), enumString));
					} catch (IllegalArgumentException iae) {
						throw new IllegalArgumentException(format(
								"String %s doesn't match any declared enum value of enum %s", enumString, pp.getType()));
					}
				} else {
					if (pp.isCollection() && Entity.class.isAssignableFrom(pp.getGenericType())) {
						e.set(propertyName, decodeArray(reader, pp));
					} else {
						e.set(propertyName,
								codecRegistry.get(typeMap.get(reader.getCurrentBsonType())).decode(reader, null));
					}
				}
			}
		}
		reader.readEndDocument();
		EntityUtils.persist(e);// persist after all properties are set
		return e;
	}

	private <E extends Entity> Collection<E> decodeArray(BsonReader reader, ParameterProperty props) {
		Collection<E> coll = getNewCollection(props.getType());

		reader.readStartArray();
		while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
			coll.add(decodeEntity(reader, (Class<E>) props.getGenericType()));
		}
		reader.readEndArray();
		return coll;
	}

	private <E extends Entity> Collection<E> getNewCollection(Class<?> type) {
		try {
			if (EntityFactory.getDefaultClass(type) == null) {
				throw new IllegalStateException(format(
						"Property is of interface %s, but no suitable implementation was registered", type));
			}
			return (Collection<E>) EntityFactory.getDefaultClass(type).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException("The impossible happened. Could not instantiate Class", e);
		}
	}

	/*
	 * Encoder Stuff
	 */

	@Override
	public void encode(BsonWriter bsonWriter, T value, EncoderContext ctx) {
		encodeInternal(bsonWriter, value, ctx);
	}

	@Override
	public Class<T> getEncoderClass() {
		return clazz;
	}

	private void encodeInternal(BsonWriter bsonWriter, T value, EncoderContext ctx) {
		// right now the context doesn't contain anything we care about, ignore it
		encode(bsonWriter, value, true, Lists.<T> newArrayList());
	}

	/**
	 * actual method encoding an entity
	 * 
	 * @param writer
	 *            writer to write to
	 * @param value
	 *            value to write
	 * @param toDB
	 *            is this just a toString() call or a real persisting action
	 */
	private void encodeEntity(BsonWriter writer, T value, boolean toDB, List<T> cycleBreaker) {
		EntityProperties properties = EntityFactory.getProperties(value.entityClass());

		if (cycleBreaker.contains(value)) {
			LOG.debug("detected cycle for type {} with id {}.", properties.getEntityClass().getCanonicalName(),
					value.get(Entity.ID));
			return;// we already visited this entity
		}
		cycleBreaker.add(value);// add the entity so we can check what we already visited

		if (toDB) {
			// mark this entity as persisted, but only if the caller isnt toString (this screws up debugging)
			EntityUtils.persist(value);
		}

		Object id = value.get(Entity.ID);
		if (id != null && !properties.hasExplicitId()) {
			// this is needed to write the object id, which at this time should be set in case it wasn't before
			// only write out the id if it's not explicitly declared
			writer.writeName(Entity.ID);
			Codec codec = codecRegistry.get(id.getClass());
			codec.encode(writer, id, null);
		}

		for (Method method : value.entityClass().getMethods()) {
			// TODO we wanna test this inheritance
			if (method.getName().startsWith("get") && method.getName().length() > 3) {
				ParameterProperty pp = properties.getProperty(method);
				String propertyName = pp.getMongoName();
				if (pp.isTransient() || value.get(propertyName) == null) {
					// transient properties aren't encoded
					// null isn't encoded
					continue;
				}
				if (pp.isReference()) {
					if (!pp.isCollection()) {
						EntityProperties seProperties = EntityFactory.getProperties((Class<? extends Entity>) pp.getType());
						Entity subEntity = (Entity) value.get(propertyName);
						Object eid = EntityCodec._obtainId(subEntity);
						// this is just for compatibility with other tools, due to our Schema information we know where
						// this
						// comes from
						if (pp.isDBRef()) {
							// if this is meant to be stored as Mongo DBRef we need to add parts
							writer.writeStartDocument(propertyName);
							writer.writeString("$ref", seProperties.getCollectionName());
							writer.writeName("$id");
							writeId(eid, writer);
							writer.writeEndDocument();
						} else {
							writer.writeName(propertyName);
							writeId(eid, writer);
						}
					} else {
						EntityProperties seProperties = EntityFactory.getProperties((Class<? extends Entity>) pp.getGenericType());
						writer.writeStartArray(propertyName);
						for (Entity subEntity : (Collection<Entity>) value.get(propertyName)) {
							Object eid = EntityCodec._obtainId(subEntity);
							if (pp.isDBRef()) {
								writer.writeStartDocument();
								writer.writeString("$ref", seProperties.getCollectionName());
								writer.writeName("$id");
								writeId(eid, writer);
								writer.writeEndDocument();
							} else {
								writeId(eid, writer);
							}
						}

						writer.writeEndArray();
					}
					continue;
				}

				if (Entity.class.isAssignableFrom(method.getReturnType())) {
					// we got some entity, so we need to recurse
					writer.writeName(propertyName);
					encode(writer, (T) value.get(propertyName), toDB, cycleBreaker);
				} else if (method.getReturnType().isEnum()) {
					// enum handling
					writer.writeString(propertyName, ((Enum) value.get(propertyName)).name());
				} else {
					// simple property
					writer.writeName(propertyName);
					Object v = value.get(propertyName);
					Codec codec = codecRegistry.get(v.getClass());
					codec.encode(writer, v, EncoderContext.builder().build());
				}
			}
		}
		cycleBreaker.remove(value);
	}

	private static void writeId(Object id, BsonWriter writer) {
		if (id.getClass() == ObjectId.class) {
			writer.writeObjectId((ObjectId) id);
		} else {
			writer.writeString(id.toString());
		}
	}

	private static EntityProperties getEntityProperties(Class clazz, String name) {
		try {
			return EntityFactory.getProperties((Class<? extends Entity>) clazz.getMethod(
					"get" + EntityUtils.capitalize(name)).getReturnType());

		} catch (NoSuchMethodException e1) {
			throw new IllegalStateException(format("failed to read 'get%s()' on class %s. Should not happen", EntityUtils.capitalize(name), clazz));
		}
	}

	private Entity getSubEntity(EntityProperties seProperties, ParameterProperty pp, BsonReader reader) {
		Object id = seProperties.getProperty("_id").getType() == ObjectId.class ? reader.readObjectId()
				: reader.readString();
		if (pp.isLazyLoaded()) {
			return factory.createLazy(seProperties.getEntityClass(), id);
		} else {
			return EntityCodec.getCollectionFor(db, seProperties).find(new Document(ID, id)).iterator().next();
		}
	}

	private void encode(BsonWriter writer, T value, boolean toDB, List<T> cycleBreaker) {
		writer.writeStartDocument();
		encodeEntity(writer, value, toDB, cycleBreaker);
		writer.writeEndDocument();
	}

	public Codec getCodec(Class clazz) {
		return codecRegistry.get(clazz);
	}

	public String asString(T value) {
		try (StringWriter swriter = new StringWriter(); JsonWriter writer = new JsonWriter(swriter)) {
			encode(writer, value, false, Lists.<T> newArrayList());
			return swriter.toString();
		} catch (IOException e) {
			Throwables.propagate(e);
			return null;
		}
	}
}
