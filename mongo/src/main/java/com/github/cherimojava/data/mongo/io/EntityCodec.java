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
package com.github.cherimojava.data.mongo.io;

import org.bson.BSONReader;
import org.bson.BSONWriter;
import org.bson.types.ObjectId;
import org.mongodb.CollectibleCodec;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;

import com.github.cherimojava.data.mongo.entities.Entity;
import com.github.cherimojava.data.mongo.entities.EntityFactory;
import com.github.cherimojava.data.mongo.entities.EntityProperties;

public class EntityCodec<T extends Entity> implements CollectibleCodec<T> {
	private final Class<T> clazz;
	private final EntityEncoder<T> enc;
	private final EntityDecoder<T> dec;
	private final EntityFactory factory;

	public EntityCodec(MongoDatabase db, EntityProperties properties) {
		clazz = (Class<T>) properties.getEntityClass();
		factory = new EntityFactory(db);
		enc = new EntityEncoder<>(factory, properties);
		dec = new EntityDecoder<>(factory, properties);
	}

	@Override
	public Object getId(T document) {
		return _getId(document);
	}

	/**
	 * retrieves the id of the given document. If there's currently no Id set, an Id is generated and stored.
	 *
	 * @param document
	 *            to retrieve doc id from
	 * @return id of the document, which might be freshly generated
	 */
	public static Object _getId(Entity document) {
		Object id = document.get(Entity.ID);
		// if the returned id is null, we know that this must be an implicit id, otherwise we don't get this far before
		// TODO with nested entities this might be not true
		if (id == null) {
			id = new ObjectId();
			document.set(Entity.ID, id);
		}
		return id;
	}

	@Override
	public <E> T decode(BSONReader reader) {
		return dec.decode(reader);
	}

	@Override
	public void encode(BSONWriter bsonWriter, T value) {
		enc.encode(bsonWriter, value);
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
		return db.getCollection(properties.getCollectionName(), new EntityCodec<>(db, properties));
	}
}
