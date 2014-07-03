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

import com.github.cherimojava.data.mongo.entity.*;
import com.google.common.collect.Maps;
import org.bson.*;
import org.bson.codecs.Decoder;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.Binary;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.bson.types.ObjectId;
import org.mongodb.Document;
import org.mongodb.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.github.cherimojava.data.mongo.entity.Entity.ID;
import static java.lang.String.format;

/**
 * Decoder which decodes Entity instances from their JSON representation
 *
 * @param <T>
 *            Entity class this decoder decodes
 * @author philnate
 * @since 1.0.0
 */
public class EntityDecoder<T extends Entity> implements Decoder<T> {

	private final Class<T> clazz;
	private final MongoDatabase db;
	private final EntityFactory factory;
	private static final Logger LOG = LoggerFactory.getLogger(EntityDecoder.class);
	private final CodecRegistry codecRegistry;
	private static final Map<BsonType, Class<?>> bsonTypeMap;

	// TODO utilize bsontypemap
	static {
		bsonTypeMap = Maps.newHashMap();
		bsonTypeMap.put(BsonType.ARRAY, List.class);
		bsonTypeMap.put(BsonType.BINARY, Binary.class);
		bsonTypeMap.put(BsonType.BOOLEAN, Boolean.class);
		bsonTypeMap.put(BsonType.DATE_TIME, Date.class);
		bsonTypeMap.put(BsonType.DB_POINTER, BsonDbPointer.class);
		bsonTypeMap.put(BsonType.DOCUMENT, Document.class);
		bsonTypeMap.put(BsonType.DOUBLE, Double.class);
		bsonTypeMap.put(BsonType.INT32, Integer.class);
		bsonTypeMap.put(BsonType.INT64, Long.class);
		bsonTypeMap.put(BsonType.MAX_KEY, MaxKey.class);
		bsonTypeMap.put(BsonType.MIN_KEY, MinKey.class);
		bsonTypeMap.put(BsonType.OBJECT_ID, ObjectId.class);
		bsonTypeMap.put(BsonType.REGULAR_EXPRESSION, BsonRegularExpression.class);
		bsonTypeMap.put(BsonType.STRING, String.class);
		bsonTypeMap.put(BsonType.TIMESTAMP, BsonTimestamp.class);
		bsonTypeMap.put(BsonType.UNDEFINED, BsonUndefined.class);
	}

	public EntityDecoder(EntityFactory factory, EntityProperties properties, CodecRegistry registry/*
																									 * ,
																									 * BsonTypeClassMap
																									 * map
																									 */) {
		clazz = (Class<T>) properties.getEntityClass();
		codecRegistry = registry;
		this.factory = factory;
		db = factory.getDb();
	}

	@Override
	public T decode(BsonReader reader, DecoderContext ctx) {
		// right now don't care about DecoderContext
		T e = factory.create(clazz);
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
									new Document(
											ID,
											seProperties.getProperty("_id").getType() == ObjectId.class ? reader.readObjectId("$id")
													: reader.readString("$id"))).iterator().next());
					reader.readEndDocument();
				} else {
					e.set(name, new EntityDecoder<>(factory, seProperties, codecRegistry/* , bsonTypeMap */).decode(
							reader, null));
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
					e.set(propertyName,
							codecRegistry.get(bsonTypeMap.get(reader.getCurrentBsonType())).decode(reader, null));
				}
			}
		}
		reader.readEndDocument();
		return e;
	}
}
