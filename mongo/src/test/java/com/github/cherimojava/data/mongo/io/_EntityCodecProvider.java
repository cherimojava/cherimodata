/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;

import org.bson.codecs.IntegerCodec;
import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.CommonInterfaces;
import com.github.cherimojava.data.mongo.TestBase;
import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;

public class _EntityCodecProvider
    extends TestBase
{
    EntityCodecProvider provider;

    @Before
    public void setup()
    {
        provider = new EntityCodecProvider( null, CommonInterfaces.ExplicitIdEntity.class );
    }

    @Test
    public void retrievePrimitiveCodec()
    {
        assertEquals( IntegerCodec.class, provider.get( Integer.class, null ).getClass() );
    }

    @Test
    public void retrieveListCodec()
    {
        assertEquals( ListCodec.class, provider.get( ArrayList.class, null ).getClass() );
    }

    @Test
    public void retrieveArrayCodec()
    {
        assertEquals( ArrayCodec.class, provider.get( Entity[].class, null ).getClass() );
    }

    @Test
    public void retrieveEntityCodec()
    {
        assertEquals( EntityCodec.class, provider.get( Entity.class, null ).getClass() );
    }

    @Test
    public void retrieveEntityFromProxyCodec()
    {
        Entity e = EntityFactory.instantiate( Entity.class );
        assertNotEquals( Entity.class, e.getClass() );
        assertEquals( EntityCodec.class, provider.get( e.getClass(), null ).getClass() );
    }

    @Test
    public void noKnownCodec()
    {
        assertNull( provider.get( Object.class, null ) );
    }
}
