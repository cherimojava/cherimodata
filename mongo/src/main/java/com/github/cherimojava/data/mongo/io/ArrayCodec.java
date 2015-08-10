/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.io;

import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import com.google.common.collect.Lists;

/**
 * codec to handle Arrays, based on {@link com.github.cherimojava.data.mongo.io.ListCodec}
 *
 * @author philnate
 * @since 1.0.0
 */
public class ArrayCodec<T>
    implements Codec<T[]>
{
    private final ListCodec listCodec;

    public ArrayCodec( CodecRegistry registry, BsonTypeClassMap bsonTypeClassMap )
    {
        listCodec = new ListCodec( registry, bsonTypeClassMap );
    }

    @Override
    public T[] decode( BsonReader reader, DecoderContext decoderContext )
    {
        List list = listCodec.decode( reader, decoderContext );
        if ( list != null && !list.isEmpty() )
        {
            return (T[]) list
                .toArray( (T[]) java.lang.reflect.Array.newInstance( list.get( 0 ).getClass(), list.size() ) );
        }
        return null;// if we have nothing in the list, it shouldn't matter if we actually return anything, right?
    }

    @Override
    public void encode( BsonWriter writer, Object[] value, EncoderContext encoderContext )
    {
        listCodec.encode( writer, Lists.newArrayList( value ), encoderContext );
    }

    @Override
    public Class<T[]> getEncoderClass()
    {
        return null;
    }
}
