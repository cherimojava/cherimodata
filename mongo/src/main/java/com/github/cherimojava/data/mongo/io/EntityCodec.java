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

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.github.cherimojava.data.mongo.entity.EntityProperties;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.configuration.RootCodecRegistry;
import org.bson.types.ObjectId;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;
import org.mongodb.codecs.CollectibleCodec;

import java.util.Arrays;

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
	private final EntityEncoder<T> enc;
	private final EntityDecoder<T> dec;
	private final EntityFactory factory;
	private final CodecRegistry codecRegistry;

	public static final CodecRegistry DEFAULT_CODEC_REGISTRY = new RootCodecRegistry(
			Arrays.<CodecProvider> asList(new EntityCodecProvider()));

	public EntityCodec(MongoDatabase db, EntityProperties properties) {
		clazz = (Class<T>) properties.getEntityClass();
		factory = new EntityFactory(db);
		codecRegistry = DEFAULT_CODEC_REGISTRY;
		enc = new EntityEncoder<>(db, properties, codecRegistry);
		dec = new EntityDecoder<>(factory, properties, codecRegistry/* , new BsonTypeClassMap() */);
	}

	@Override
	public T decode(BsonReader reader, DecoderContext ctx) {
		return dec.decode(reader, ctx);
	}

	@Override
	public void encode(BsonWriter bsonWriter, T value, EncoderContext ctx) {
		enc.encode(bsonWriter, value, ctx);
	}

	@Override
	public Class<T> getEncoderClass() {
		return clazz;
	}

	/**
	 * Creates a MongoCollection which has a EntityCodec attached to it
	 *
	 * @param db
	 * @param properties
	 * @return
	 */
	public static MongoCollection<? extends Entity> getCollectionFor(MongoDatabase db, EntityProperties properties) {
		return db.getCollection(properties.getCollectionName(), (Codec) new EntityCodec<>(db, properties));
	}

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

}
