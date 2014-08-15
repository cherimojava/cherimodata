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
package org.mongodb.codecs;

import java.util.Map;

import org.bson.BsonType;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.google.common.collect.Maps;

/**
 * BsonTypeClassMap adjusted to our Entity needs
 */
public class EntityTypeMap extends BsonTypeClassMap {
	private static final Map<BsonType, Class<?>> replacements;

	static {
		replacements = Maps.newHashMap();
		replacements.put(BsonType.DOCUMENT, Entity.class);
	}

	public EntityTypeMap() {
		super(replacements);
	}

    public EntityTypeMap(Class<? extends Entity> clazz) {
        super(replacement(clazz));
    }

    private static Map<BsonType,Class<?>>  replacement(Class<? extends Entity> clazz) {
        Map<BsonType,Class<?>> repl= Maps.newHashMap(replacements);
        repl.put(BsonType.DOCUMENT,clazz);
        return repl;
    }

	public Class<?> get(final BsonType bsonType) {
		return super.get(bsonType);
	}
}
