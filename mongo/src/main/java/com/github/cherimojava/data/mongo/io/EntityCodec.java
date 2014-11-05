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

import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriter;
import org.bson.types.ObjectId;
import org.mongodb.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.entity.*;
import com.google.common.base.Throwables;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOptions;
import com.mongodb.codecs.CollectibleCodec;
import com.mongodb.codecs.EntityTypeMap;

/**
 * Codec which transforms Entity instances from/to JSON
 *
 * @param <T>
 *            Entity class this Codec takes care of
 * @author philnate
 * @since 1.0.0
 */
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
		return db.getCollection(properties.getCollectionName(), properties.getEntityClass(),
				EntityCodecProvider.createMongoCollectionOptions(db, properties.getEntityClass()));
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
				Class<? extends Entity> cls = null;
				try {
					cls = (Class<? extends Entity>) clazz.getMethod("get" + EntityUtils.capitalize(name), null).getReturnType();
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
				}
				EntityProperties seProperties = EntityFactory.getProperties(cls);
				if (pp.isReference()) {
					// Entity is only stored as reference, so we can only read the id from it
					reader.readStartDocument();
					// read the references collection, but we know where the reference belongs to, so discard
					reader.readString("$ref");
					e.set(name,
							EntityCodec.getCollectionFor(db, seProperties).find(
									new FindOptions().criteria(new Document(
											ID,
											seProperties.getProperty("_id").getType() == ObjectId.class ? reader.readObjectId("$id")
													: reader.readString("$id")))).iterator().next());
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
		Collection<E> coll;
		try {
			if (EntityFactory.getDefaultClass(props.getType()) == null) {
				throw new IllegalStateException(format(
						"Property is of interface %s, but no suitable implementation was registered", props.getType()));
			}
			coll = (Collection<E>) EntityFactory.getDefaultClass(props.getType()).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException("The impossible happened. Could not instantiate Class", e);
		}
		reader.readStartArray();
		while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
			coll.add(decodeEntity(reader, (Class<E>) props.getGenericType()));
		}
		reader.readEndArray();
		return coll;
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

	public void encodeInternal(BsonWriter bsonWriter, T value, EncoderContext ctx) {
		// right now the context doesn't contain anything we care about, ignore it
		encode(bsonWriter, value, true);
	}

	private void encodeEntity(BsonWriter writer, T value, boolean toDB) {
		EntityProperties properties = EntityFactory.getProperties(value.entityClass());

		// mark this entity as persisted
		EntityUtils.persist(value);

		Object id = value.get(Entity.ID);
		if (id != null && !properties.hasExplicitId()) {
			// this is needed to write the object id, which at this time should be set in case it wasn't before
			// TODO shouldn't this be always true?
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
					writer.writeStartDocument(propertyName);
					EntityProperties seProperties = EntityFactory.getProperties((Class<? extends Entity>) pp.getType());
					Entity subEntity = (Entity) value.get(propertyName);
					Object eid = EntityCodec._obtainId(subEntity);
					// this is just for compatibility with other tools, due to our Schema information we know where this
					// comes from
					writer.writeString("$ref", seProperties.getCollectionName());
					writer.writeName("$id");
					if (eid.getClass() == ObjectId.class) {
						writer.writeObjectId((ObjectId) eid);
					} else {
						writer.writeString(eid.toString());
					}
					if (toDB) {
						subEntity.save();
						// ((MongoCollection<Entity>) EntityCodec.getCollectionFor(db, seProperties)).updateOne(
						// new Document(Entity.ID, subEntity.get(Entity.ID)), subEntity);
					}
					writer.writeEndDocument();
					continue;
				}

				if (Entity.class.isAssignableFrom(method.getReturnType())) {
					// we got some entity, so we need to recurse
					writer.writeName(propertyName);
					encode(writer, (T) value.get(propertyName), toDB);
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
	}

	private void encode(BsonWriter writer, T value, boolean toDB) {
		writer.writeStartDocument();
		encodeEntity(writer, value, toDB);
		writer.writeEndDocument();
	}

	public Codec getCodec(Class clazz) {
		return codecRegistry.get(clazz);
	}

	public String asString(T value) {
		try (StringWriter swriter = new StringWriter(); JsonWriter writer = new JsonWriter(swriter)) {
			encode(writer, value, false);
			return swriter.toString();
		} catch (IOException e) {
			Throwables.propagate(e);
			return null;
		}
	}
}
