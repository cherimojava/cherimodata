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

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.configuration.RootCodecRegistry;
import org.mongodb.Document;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.mongodb.client.MongoCollectionOptions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.codecs.*;

/**
 * A {@link org.bson.codecs.configuration.CodecProvider} for Entities and all the default Codec implementations on which
 * it depends.
 *
 * @author philnate
 * @since 1.0.0
 */
public class EntityCodecProvider implements CodecProvider {
	private BsonTypeClassMap mapping;
	private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();
    private final MongoDatabase db;

	/**
	 * Constructs a new instance with default {@link com.mongodb.codecs.BsonTypeClassMap}
	 */
	public EntityCodecProvider(MongoDatabase db,Class<? extends Entity> clazz) {
		mapping = new EntityTypeMap(clazz);
        this.db = db;
		addCodecs();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
		if (codecs.containsKey(clazz)) {
			return (Codec<T>) codecs.get(clazz);
		}
		// TODO coming from decoding site, we only get Entity as class, how to get properties from this...
		// solution could be that instead we provide the class we have to go to in the decoding context and keep the
		// decoder free of this information

		if (Entity.class.isAssignableFrom(clazz)) {
            //there are two possible class types we can get. Some are the real interfaces and the other classes are proxy based
            Class<?> eclass = Proxy.isProxyClass(clazz)? clazz.getInterfaces()[0]: clazz;
            return (Codec<T>) new EntityCodec(db,
					EntityFactory.getProperties((Class<? extends Entity>) eclass));
		}

        if (Document.class.isAssignableFrom(clazz)) {
            return (Codec<T>) new DocumentCodec(registry,mapping);
        }

		if (List.class.isAssignableFrom(clazz)) {
			return (Codec<T>) new ListCodec(registry, mapping);
		}

		if (clazz.isArray()) {
			return (Codec<T>) new ArrayCodec(registry, mapping);
		}

		return null;
	}

	private void addCodecs() {
		addCodec(new BinaryCodec());
		addCodec(new BooleanCodec());
		addCodec(new DateCodec());
		addCodec(new DoubleCodec());
		addCodec(new IntegerCodec());
		addCodec(new LongCodec());
		addCodec(new MinKeyCodec());
		addCodec(new MaxKeyCodec());
		addCodec(new CodeCodec());
		addCodec(new ObjectIdCodec());
		addCodec(new StringCodec());
		addCodec(new SymbolCodec());
        addCodec(new DateTimeCodec());
	}

	private <T> void addCodec(final Codec<T> codec) {
		codecs.put(codec.getEncoderClass(), codec);
	}

    /**
     * creates a RootCodecRegistry with our EntityCodecProvider as sole CodecProvider 
     * @param db
     * @param clazz
     * @return
     */
    public static CodecRegistry createCodecRegistry(MongoDatabase db, Class<? extends Entity> clazz) {
        return new RootCodecRegistry(Arrays.<CodecProvider> asList(new EntityCodecProvider(db, clazz)));
    }

    /**
     * creates current default MongoCollectionOptions containing only the codecRegistry to use
     * @param db
     * @param clazz
     * @return
     */
    public static MongoCollectionOptions createMongoCollectionOptions(MongoDatabase db, Class<? extends Entity> clazz) {
        return MongoCollectionOptions.builder().codecRegistry(createCodecRegistry(db,clazz)).build();
    }
}
