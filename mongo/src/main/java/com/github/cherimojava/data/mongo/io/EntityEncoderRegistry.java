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

import org.mongodb.Encoder;
import org.mongodb.MongoDatabase;
import org.mongodb.codecs.EncoderRegistry;

import com.github.cherimojava.data.mongo.entity.EntityProperties;

/**
 * EncoderRegistry which returns an EntityEncoder as defaultEncoder
 *
 * @author philnate
 * @since 1.0.0
 */
class EntityEncoderRegistry extends EncoderRegistry {
	private final MongoDatabase db;
	private final EntityProperties properties;

	public EntityEncoderRegistry(MongoDatabase db, EntityProperties properties) {
		this.db = db;
		this.properties = properties;
	}

	@Override
	public Encoder getDefaultEncoder() {
		return new EntityEncoder(db, properties);
	}
}
