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

import org.bson.BSONReader;
import org.bson.BSONType;
import org.bson.types.ObjectId;
import org.mongodb.Decoder;
import org.mongodb.Document;
import org.mongodb.MongoDatabase;
import org.mongodb.codecs.Codecs;
import org.mongodb.codecs.PrimitiveCodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.github.cherimojava.data.mongo.entity.EntityProperties;
import com.github.cherimojava.data.mongo.entity.EntityUtils;
import com.github.cherimojava.data.mongo.entity.ParameterProperty;

import static com.github.cherimojava.data.mongo.entity.Entity.ID;

public class EntityDecoder<T extends Entity> implements Decoder<T> {

	private final Class<T> clazz;
	private final Codecs codecs;
	private final MongoDatabase db;
	private final EntityFactory factory;
	private static final Logger LOG = LoggerFactory.getLogger(EntityDecoder.class);

	public EntityDecoder(EntityFactory factory, EntityProperties properties) {
		clazz = (Class<T>) properties.getEntityClass();
		codecs = Codecs.builder().primitiveCodecs(PrimitiveCodecs.createDefault()).build();
		this.factory = factory;
		db = factory.getDb();
	}

	@Override
	public <E> T decode(BSONReader reader) {
		T e = factory.create(clazz);
		EntityProperties properties = EntityFactory.getProperties(clazz);
		reader.readStartDocument();
		BSONType type;
		while ((type = reader.readBSONType()) != BSONType.END_OF_DOCUMENT) {
			if (type == BSONType.DOCUMENT) {
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
					e.set(name,
							EntityCodec.getCollectionFor(db, seProperties).find(
									new Document(
											ID,
											seProperties.getProperty("_id").getType() == ObjectId.class ? reader.readObjectId("$id")
													: reader.readString("$id"))).iterator().next());
					reader.readEndDocument();
				} else {
					e.set(name, new EntityDecoder<>(factory, seProperties).decode(reader));
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
					reader.skipValue();// sent value to /dev/null
					continue;
				}
				e.set(propertyName, codecs.decode(reader));
			}
		}
		reader.readEndDocument();
		return e;
	}
}
