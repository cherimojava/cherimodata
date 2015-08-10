/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.io;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.joda.time.DateTime;

/**
 * Codec for joda-time DateTime, stores the time in the standard bson date format
 * 
 * @author philnate
 */
public class DateTimeCodec
    implements Codec<DateTime>
{
    @Override
    public DateTime decode( BsonReader reader, DecoderContext decoderContext )
    {
        return new DateTime( reader.readDateTime() );
    }

    @Override
    public void encode( BsonWriter writer, DateTime value, EncoderContext encoderContext )
    {
        writer.writeDateTime( value.getMillis() );
    }

    @Override
    public Class<DateTime> getEncoderClass()
    {
        return DateTime.class;
    }
}
