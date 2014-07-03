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
import org.bson.codecs.*;
import org.bson.codecs.CodeCodec;
import org.bson.codecs.MaxKeyCodec;
import org.bson.codecs.MinKeyCodec;
import org.bson.codecs.ObjectIdCodec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.mongodb.codecs.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A {@link org.bson.codecs.configuration.CodecProvider} for Entities and all the default Codec implementations on which
 * it depends.
 *
 * @author philnate
 * @since 1.0.0
 */
public class EntityCodecProvider implements CodecProvider {
	private BsonTypeClassMap mapping;
	private final Map<Class<?>, Codec<?>> codecs = new HashMap<Class<?>, Codec<?>>();

	/**
	 * Constructs a new instance with a defalut {@link com.github.cherimojava.data.mongo.io.BsonTypeMapping}
	 */
	public EntityCodecProvider() {
		mapping = new BsonTypeClassMap();
		addCodecs();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
		if (codecs.containsKey(clazz)) {
			return (Codec<T>) codecs.get(clazz);
		}

		if (Entity.class.isAssignableFrom(clazz)) {
			checkArgument(clazz.getInterfaces().length == 1,
					"Got Entity castable class but the number of interfaces doesn't match.", clazz,
					clazz.getInterfaces());

			return (Codec<T>) new EntityCodec(null,
					EntityFactory.getProperties((Class<? extends Entity>) clazz.getInterfaces()[0]));
		}

		if (List.class.isAssignableFrom(clazz)) {
			// TODO this is pretty ugly
			return (Codec<T>) new ListCodec(registry, mapping);
		}

		return null;
	}

	private void addCodecs() {
		addCodec(new BinaryCodec());
		addCodec(new BooleanCodec());
		addCodec(new DateCodec());
		addCodec(new BsonDBPointerCodec());
		addCodec(new DoubleCodec());
		addCodec(new IntegerCodec());
		addCodec(new LongCodec());
		addCodec(new MinKeyCodec());
		addCodec(new MaxKeyCodec());
		addCodec(new CodeCodec());
		addCodec(new ObjectIdCodec());
		addCodec(new BsonRegularExpressionCodec());
		addCodec(new StringCodec());
		addCodec(new SymbolCodec());
		addCodec(new BsonTimestampCodec());
		addCodec(new BsonUndefinedCodec());
	}

	private <T> void addCodec(final Codec<T> codec) {
		codecs.put(codec.getEncoderClass(), codec);
	}
}