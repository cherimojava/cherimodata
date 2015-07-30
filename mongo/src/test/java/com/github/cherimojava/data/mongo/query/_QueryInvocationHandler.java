/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.query;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.cherimojava.data.mongo.CommonInterfaces;
import com.github.cherimojava.data.mongo.TestBase;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class _QueryInvocationHandler
    extends TestBase
{

    @Mock
    MongoDatabase db;

    @Mock
    MongoCollection coll;

    EntityFactory factory;

    @Before
    public void setupMocks()
    {
        MockitoAnnotations.initMocks( this );
        when( db.getCollection( anyString() ) ).thenReturn( coll );
        when( coll.withCodecRegistry( any( CodecRegistry.class ) ) ).thenReturn( coll );
        when( coll.withDocumentClass( any( Class.class ) ) ).thenReturn( coll );
        when( coll.find( any( Bson.class ) ) ).thenReturn( mock( FindIterable.class ) );
        factory = new EntityFactory( db );
    }

    @Test
    public void basicStructureTest()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> q = factory.query( CommonInterfaces.PrimitiveEntity.class );
        OngoingQuery<CommonInterfaces.PrimitiveEntity> o = q.where( q.e().getInteger() ).between( 3, 12 );
        o.and( q.e().getString() ).is( "me" ).iterator();
    }

    @Test
    public void noQueryConditionOnNullProperty()
    {
        try
        {
            QueryStart<CommonInterfaces.PrimitiveEntity> q = factory.query( CommonInterfaces.PrimitiveEntity.class );
            q.where( "bogus" ).is( "bogus" );
            fail( "should throw an exception" );
        }
        catch ( IllegalStateException e )
        {
            assertThat( e.getMessage(), containsString( "from null property" ) );
        }
    }
}
