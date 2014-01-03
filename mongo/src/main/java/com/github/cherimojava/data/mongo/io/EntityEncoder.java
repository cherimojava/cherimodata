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
package com.github.cherimojava.data.mongo.io;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;

import org.bson.BSONWriter;
import org.bson.types.ObjectId;
import org.mongodb.Encoder;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;
import org.mongodb.codecs.Codecs;
import org.mongodb.codecs.PrimitiveCodecs;
import org.mongodb.json.JSONWriter;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.github.cherimojava.data.mongo.entity.EntityProperties;
import com.github.cherimojava.data.mongo.entity.ParameterProperty;

public class EntityEncoder<T extends Entity> implements Encoder<T> {

	private final Codecs codecs;
	private final Class<T> clazz;
	private MongoDatabase db;

	public EntityEncoder(MongoDatabase db, EntityProperties properties) {
		codecs = Codecs.builder().primitiveCodecs(PrimitiveCodecs.createDefault()).build();
		clazz = (Class<T>) properties.getEntityClass();
		this.db = db;
	}

	@Override
	public void encode(BSONWriter bsonWriter, T value) {
		encode(bsonWriter, value, true);
	}

	private void encodeEntity(BSONWriter writer, T value, boolean toDB) {
		EntityProperties properties = EntityFactory.getProperties(value.entityClass());
		Object id = value.get(Entity.ID);
		if (id != null && !properties.hasExplicitId()) {
			// this is needed to write the object id, which at this time should be set in case it wasn't before
			// TODO shouldn't this be always true?
			// only write out the id if it's not explicitly declared
			writer.writeName(Entity.ID);
			codecs.encode(writer, id);
		}

		for (Method method : value.entityClass().getMethods())
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
					Object eid = EntityCodec._getId(subEntity);
					writer.writeName("$id");
					if (eid.getClass() == ObjectId.class) {
						writer.writeObjectId((ObjectId) eid);
					} else {
						writer.writeString(eid.toString());
					}
					if (toDB) {
						((MongoCollection<Entity>) EntityCodec.getCollectionFor(db, seProperties)).save(subEntity);
						writer.writeEndDocument();
					}
					continue;
				}
				if (codecs.canDecode(method.getReturnType())) {
					writer.writeName(propertyName);
					codecs.encode(writer, value.get(propertyName));
				} else if (Entity.class.isAssignableFrom(method.getReturnType())) {
					// we got some entity, so we need to recurse
					writer.writeName(propertyName);
					encode(writer, (T) value.get(propertyName),toDB);
				}
			}
	}

	private void encode(BSONWriter writer, T value, boolean toDB) {
		writer.writeStartDocument();
		encodeEntity(writer, value, toDB);
		writer.writeEndDocument();
	}

	public String asString(T value) {
		try (StringWriter swriter = new StringWriter(); JSONWriter writer = new JSONWriter(swriter)) {
			encode(writer, value, false);
			return swriter.toString();
		} catch (IOException e) {
			return e.getMessage();
		}
	}

	@Override
	public Class<T> getEncoderClass() {
		return clazz;
	}
}
