/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.entity;

import static com.github.cherimojava.data.mongo.CommonInterfaces.AdderTest;
import static com.github.cherimojava.data.mongo.CommonInterfaces.ExplicitIdEntity;
import static com.github.cherimojava.data.mongo.CommonInterfaces.FluentEntity;
import static com.github.cherimojava.data.mongo.CommonInterfaces.ImplicitIdEntity;
import static com.github.cherimojava.data.mongo.CommonInterfaces.InvalidEntity;
import static com.github.cherimojava.data.mongo.CommonInterfaces.NestedEntity;
import static com.github.cherimojava.data.mongo.CommonInterfaces.PrimitiveEntity;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.CommonInterfaces;
import com.github.cherimojava.data.mongo.TestBase;
import com.github.cherimojava.data.mongo.entity.annotation.Computed;

public class _EntityPropertyFactory
    extends TestBase
{

    EntityPropertiesFactory factory;

    @Before
    public void init()
    {
        factory = new EntityPropertiesFactory();
    }

    @Test
    public void singletonEntityProperties()
    {
        assertTrue( factory.create( PrimitiveEntity.class ) == factory.create( PrimitiveEntity.class ) );
    }

    @Test
    public void collectionName()
    {
        assertEquals( "primitiveEntitys", factory.create( PrimitiveEntity.class ).getCollectionName() );
        assertEquals( "Nested", factory.create( NestedEntity.class ).getCollectionName() );
    }

    @Test
    public void returnTypeOnGet()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateGetter( InvalidEntity.class.getDeclaredMethod( "getNoReturnParam" ) );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "did not declare a return type" ) );
        }
    }

    @Test
    public void noParameterOnGet()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateGetter( InvalidEntity.class.getDeclaredMethod( "getString", String.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Get methods can't have parameters" ) );
        }
    }

    @Test
    public void noMultiParameterSet()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateSetter(
                InvalidEntity.class.getDeclaredMethod( "setMultiParamSet", String.class, String.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Set methods must have one parameter" ) );
        }
    }

    @Test
    public void referenceMustBeOfTypeEntity()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateGetter( InvalidEntity.class.getDeclaredMethod( "getInvalidProp" ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Cant declare" ) );
        }
    }

    @Test
    public void setterReturnOnlyEntity()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateSetter( FluentEntity.class.getMethod( "getOtherEntity" ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Only Superclasses of" ) );
        }

        try
        {
            factory.validateSetter( FluentEntity.class.getMethod( "getUnrelatedClass" ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Only Superclasses of" ) );
        }
    }

    @Test
    public void noMultipleGetterForProperty()
    {
        try
        {
            factory.create( MultipleGetterForProperty.class );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Entity contains already a property" ) );
        }
    }

    @Test
    public void noAdditionalSetter()
    {
        try
        {
            factory.create( AdditionalSetter.class );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(),
                containsString( "You can only declare setter methods if there's a matching getter." ) );
        }
    }

    @Test
    public void noDuplicateSetter()
    {
        try
        {
            factory.create( AdditionalSetterForProperty.class );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(),
                containsString( "You can only declare setter methods if there's a matching getter." ) );
        }
    }

    @Test
    public void noAllowedMethodDuplication()
    {
        try
        {
            factory.create( NotAllowedMethodName.class );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Don't write custom" ) );
        }
    }

    @Test
    public void propertiesAreInherited()
    {
        EntityProperties props = factory.create( InheritedEntity.class );
        assertNotNull( props.getProperty( "string" ) );
        assertNotNull( props.getProperty( "Integer" ) );
        assertNotNull( props.getProperty( "anotherString" ) );
        assertNull( props.getProperty( "noProperty" ) );
        assertEquals( 4, props.getProperties().size() );
    }

    @Test
    public void explicitIdDetection()
    {
        assertFalse( factory.create( PrimitiveEntity.class ).hasExplicitId() );
        assertTrue( factory.create( ExplicitIdEntity.class ).hasExplicitId() );
        assertTrue( factory.create( ImplicitIdEntity.class ).hasExplicitId() );
    }

    @Test
    public void idAvailable()
    {
        assertEquals( Entity.ID, factory.create( PrimitiveEntity.class ).getIdProperty().getPojoName() );
        assertEquals( "name", factory.create( ExplicitIdEntity.class ).getIdProperty().getPojoName() );
        assertEquals( "id", factory.create( ImplicitIdEntity.class ).getIdProperty().getPojoName() );
    }

    @Test
    public void noSetForComputedProperty()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateSetter( InvalidEntity.class.getDeclaredMethod( "setComputed", String.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "computed" ) );
        }
    }

    @Test
    public void noGetterForSetter()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateSetter( InvalidEntity.class.getDeclaredMethod( "setNoGetter", String.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(),
                containsString( "You can only declare setter methods if there's a matching getter" ) );
        }
    }

    @Test
    public void adderNoGetter()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateAdder( AdderTest.class.getDeclaredMethod( "addNoGetter", String.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "has no corresponding getter method" ) );
        }
    }

    @Test
    public void adderNoCollectionGetter()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateAdder( AdderTest.class.getDeclaredMethod( "addNoCollectionGetter", String.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(),
                containsString( "Adder method only allowed for properties extending collection, but was class" ) );
        }
    }

    @Test
    public void adderWrongGenericTypeGetter()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateAdder( AdderTest.class.getDeclaredMethod( "addStrings", Integer.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "but adder has parameter of type" ) );
        }
    }

    @Test
    public void adderParameterCount()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateAdder( AdderTest.class.getDeclaredMethod( "addString", String.class, Integer.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Adder method must define exactly one parameter matching" ) );
        }
    }

    @Test
    public void underlineMethodNameForbidden()
    {
        try
        {
            factory.create( UnderlineMethodEntity.class );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Property can't start with '_' as this is reserved, but" ) );
        }
    }

    @Test
    public void isWithParams()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateIsser( IsWithParamEntity.class.getDeclaredMethod( "isParam", String.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Is method must not define any parameters" ) );
        }
    }

    @Test
    public void isComputed()
        throws NoSuchMethodException
    {
        try
        {
            factory.validateSetter( IsComputedEntity.class.getDeclaredMethod( "setParam", boolean.class ) );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(),
                containsString( "computed property param cannot have a setter method declared" ) );
        }
    }

    @Test
    public void UnderlineNamedMethodForbidden()
    {
        try
        {
            factory.create( UnderlineNameMethodEntity.class );
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Property can't start with '_' as this is reserved, but" ) );
        }
    }

    private static interface UnderlineNameMethodEntity
        extends Entity
    {
        @Named( "_bs" )
        public String getM();

        public UnderlineNameMethodEntity setM( String s );
    }

    private static interface UnderlineMethodEntity
        extends Entity
    {
        public String get_M();

        public UnderlineMethodEntity set_M( String s );
    }

    private static interface InheritedEntity
        extends PrimitiveEntity
    {
        public String getAnotherString();

        public void setAnotherString( String s );
    }

    private static interface IsWithParamEntity
        extends Entity
    {
        public boolean isParam( String b );

        public IsWithParamEntity setParam( boolean param );
    }

    private static interface IsComputedEntity
        extends Entity
    {
        @Computed( CommonInterfaces.StringComputer.class )
        public boolean isParam();

        public IsComputedEntity setParam( boolean param );
    }

    private static interface MultipleGetterForProperty
        extends Entity
    {
        public String getString();

        public void setString( String s );

        @Named( "string" )
        public String getGnirts();

        public void setGnirts( String s );
    }

    private static interface AdditionalSetter
        extends Entity
    {
        public String getString();

        public void setString( String s );

        public void setNothing( String not );
    }

    private static interface AdditionalSetterForProperty
        extends Entity
    {
        public String getString();

        public void setString( String s );

        public void setString( int n );
    }

    private static interface NotAllowedMethodName
        extends Entity
    {
        public String equals( String some );
    }
}
