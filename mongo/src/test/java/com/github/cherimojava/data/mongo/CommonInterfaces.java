/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.validation.constraints.NotNull;

import com.github.cherimojava.data.mongo.entity.Computer;
import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.annotation.Id;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;
import com.github.cherimojava.data.mongo.entity.annotation.Transient;

/**
 * Class holding Entity based interfaces shared across test classes
 */
public class CommonInterfaces
{

    /**
     * interface only containing primitive properties
     */
    public static interface PrimitiveEntity
        extends Entity<PrimitiveEntity>
    {
        public PrimitiveEntity setString( String s );

        @NotNull( groups = Special.class )
        public String getString();

        public Entity setInteger( Integer i );

        @Named( "Integer" )
        public Integer getInteger();
    }

    /**
     * interface with primitive types
     */
    public static interface PrimitiveTypeEntity
        extends Entity<PrimitiveTypeEntity>
    {

        public Entity setInt( int i );

        @Id
        public int getInt();
    }

    /**
     * containing primitive and Entity properties
     */
    @Named( "Nested" )
    public static interface NestedEntity
        extends Entity<NestedEntity>
    {
        public String getString();

        public void setString( String s );

        public PrimitiveEntity getPE();

        public void setPE( PrimitiveEntity pe );
    }

    /**
     * containing arrays and lists, map
     */
    public static interface CollectionEntity
        extends Entity<CollectionEntity>
    {
        public List<String> getStrings();

        public void setStrings( List<String> strings );

        public String[] getArrayStrings();

        public void setArrayStrings( String[] strings );

        public Map<String, String> getMap();

        public void setMap( Map<String, String> map );
    }

    public static interface ExplicitIdEntity
        extends Entity<ExplicitIdEntity>
    {
        @Id
        public String getName();

        public ExplicitIdEntity setName( String name );
    }

    public static interface ImplicitIdEntity
        extends Entity
    {
        public String getId();

        public ImplicitIdEntity setId( String id );
    }

    public static interface ComputedPropertyEntity
        extends PrimitiveEntity
    {
        @Computed( StringComputer.class )
        public String getComputed();
    }

    public static interface ReferencingEntity
        extends PrimitiveEntity
    {
        @Reference
        public PrimitiveEntity getPE();

        public ReferencingEntity setPE( PrimitiveEntity pe );

        @Reference( asDBRef = true )
        public PrimitiveEntity getDBRef();

        public ReferencingEntity setDBRef( PrimitiveEntity pe );

        @Reference
        public List<PrimitiveEntity> getListedEntities();

        public ReferencingEntity setListedEntities( List<PrimitiveEntity> list );

        @Reference( asDBRef = true )
        public List<PrimitiveEntity> getListedDBRefEntities();

        public ReferencingEntity setListedDBRefEntities( List<PrimitiveEntity> list );

    }

    public static interface LazyLoadingEntity
        extends PrimitiveEntity
    {
        @Reference( lazy = true )
        public PrimitiveEntity getPE();

        public LazyLoadingEntity setPE( PrimitiveEntity pe );
    }

    public static class StringComputer
        implements Computer<PrimitiveEntity, String>
    {

        @Override
        public String compute( PrimitiveEntity pe )
        {
            return pe.getString() + pe.getInteger();
        }
    }

    public static interface FluentEntity
        extends Entity
    {
        @Transient
        public String getNotFluent();

        public void setNotFluent( String s );

        public String getOwnClass();

        public FluentEntity setOwnClass( String s );

        public String getSuperClass(); // this should cover the Object case as well

        public Entity setSuperClass( String s );

        public String getOtherEntity();

        public InvalidEntity setOtherEntity( String s );

        public String getUnrelatedClass();

        public String setUnrelatedClass( String s );
    }

    public static interface InvalidEntity
        extends Entity
    {
        public void setString();

        public String getString( String s );

        public void getNoReturnParam();

        public Integer getTypeMismatch();

        public void setTypeMismatch( String s );

        public String getNoParamSet();

        public void setNoParamSet();

        public void setNoGetter( String s );

        public String getMultiParamSet();

        public void setMultiParamSet( String one, String two );

        @Computed( StringComputer.class )
        public String getComputed();

        public void setComputed( String s );

        @Reference
        public String getInvalidProp();

        public InvalidEntity setInvalidProp( String s );
    }

    public static interface AdderTest
        extends Entity
    {
        public void addNoGetter( String m );

        public String getNoCollectionGetter();

        public void addNoCollectionGetter( String m );

        public List<String> getStrings();

        public void addStrings( Integer i );

        public void addString( String s, Integer i );
    }

    public static interface AddEntity
        extends Entity<AddEntity>
    {
        @Id
        public String getId();

        public AddEntity setId( String id );

        public List<String> getString();

        public void addString( String s );

        public void setString( List<String> s );
    }

    public static interface EntityList
        extends Entity<EntityList>
    {
        @Id
        public String getId();

        public EntityList setId( String id );

        public List<PrimitiveEntity> getList();

        public EntityList setList( List<PrimitiveEntity> list );
    }

    public static interface IsEntity
        extends Entity
    {
        public boolean isBoolean();

        public IsEntity setBoolean( boolean b );
    }
}
