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
package com.github.cherimojava.data.mongo.entity;

import static com.github.cherimojava.data.mongo.CommonInterfaces.*;
import static com.github.cherimojava.data.mongo.entity.ParameterProperty.Builder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.util.List;

import javax.validation.Validation;
import javax.validation.Validator;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.TestBase;
import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.annotation.Final;
import com.github.cherimojava.data.mongo.entity.annotation.Id;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;

public class _ParameterPropertyBuilder extends TestBase {

	Validator validator;

	@Before
	public void init() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	public void detectFluent() throws NoSuchMethodException {
		assertFalse(Builder.buildFrom(FluentEntity.class.getMethod("getNotFluent"), validator).isFluent(
				ParameterProperty.MethodType.SETTER));
		assertTrue(Builder.buildFrom(FluentEntity.class.getMethod("getOwnClass"), validator).isFluent(
				ParameterProperty.MethodType.SETTER));
		assertTrue(Builder.buildFrom(FluentEntity.class.getMethod("getSuperClass"), validator).isFluent(
				ParameterProperty.MethodType.SETTER));
	}

	@Test
	public void detectType() throws NoSuchMethodException {
		assertEquals(String.class, Builder.buildFrom(FluentEntity.class.getMethod("getNotFluent"), validator).getType());
		assertEquals(List.class, Builder.buildFrom(Ids.class.getMethod("getListEntity"), validator).getType());
	}

	@Test
	public void detectContainedType() throws NoSuchMethodException {
		try {
			Builder.buildFrom(Ids.class.getMethod("getList"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Collections need to be generic"));
		}
		ParameterProperty pp = Builder.buildFrom(Ids.class.getMethod("getListEntity"), validator);
		assertEquals(List.class, pp.getType());
		assertEquals(Entity.class, pp.getGenericType());
		assertTrue(pp.isCollection());

		assertFalse(Builder.buildFrom(PrimitiveEntity.class.getDeclaredMethod("getInteger"), validator).isCollection());
	}

	@Test
	public void retrieveMongoName() throws NoSuchMethodException {
		assertEquals("Integer",
				Builder.buildFrom(PrimitiveEntity.class.getDeclaredMethod("getInteger"), validator).getMongoName());
	}

	@Test
	public void detectIdAnnotation() throws NoSuchMethodException {
		ParameterProperty pp = Builder.buildFrom(Ids.class.getDeclaredMethod("getEIDE"), validator);
		assertEquals("_id", pp.getMongoName());
		assertEquals("EIDE", pp.getPojoName());
	}

	@Test
	public void idNameTo_Id() throws NoSuchMethodException {
		ParameterProperty pp = Builder.buildFrom(Ids.class.getDeclaredMethod("getId"), validator);
		assertEquals("_id", pp.getMongoName());
		assertEquals("id", pp.getPojoName());
	}

	@Test
	public void hasConstraints() throws NoSuchMethodException {
		assertTrue(Builder.buildFrom(PrimitiveEntity.class.getDeclaredMethod("getString"), validator).hasConstraints());
		assertFalse(Builder.buildFrom(PrimitiveEntity.class.getDeclaredMethod("getInteger"), validator).hasConstraints());
	}

	@Test
	public void isTransient() throws NoSuchMethodException {
		assertTrue(Builder.buildFrom(FluentEntity.class.getDeclaredMethod("getNotFluent"), validator).isTransient());
		assertFalse(Builder.buildFrom(FluentEntity.class.getDeclaredMethod("getOwnClass"), validator).isTransient());
	}

	@Test
	public void validReference() throws NoSuchMethodException {
		Builder.buildFrom(ReferencedEntity.class.getDeclaredMethod("getValidProp"), validator);
	}

	@Test
	public void referenceTypeDetection() throws NoSuchMethodException {
		assertTrue(Builder.buildFrom(ReferencingEntity.class.getDeclaredMethod("getPE"), validator).isReference());
		assertTrue(Builder.buildFrom(LazyLoadingEntity.class.getDeclaredMethod("getPE"), validator).isReference());
		assertTrue(Builder.buildFrom(LazyLoadingEntity.class.getDeclaredMethod("getPE"), validator).isLazyLoaded());
		assertFalse(Builder.buildFrom(NestedEntity.class.getDeclaredMethod("getPE"), validator).isReference());
		assertFalse(Builder.buildFrom(NestedEntity.class.getDeclaredMethod("getPE"), validator).isLazyLoaded());
	}

	@Test
	public void referenceDBRefDetection() throws NoSuchMethodException {
		assertTrue(Builder.buildFrom(ReferencingEntity.class.getDeclaredMethod("getDBRef"),validator).isDBRef());
		assertFalse(Builder.buildFrom(ReferencingEntity.class.getDeclaredMethod("getPE"), validator).isDBRef());
		assertTrue(Builder.buildFrom(ReferencingEntity.class.getDeclaredMethod("getPE"), validator).isReference());
	}

	@Test
	public void referenceOnlyOnEntities() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidReference.class.getDeclaredMethod("getInvalidProp"), validator);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Reference annotation can only be used for Entity types"));
		}
	}

	@Test
	public void isFinal() throws NoSuchMethodException {
		assertTrue(Builder.buildFrom(Ids.class.getDeclaredMethod("getId"), validator).isFinal());
		assertTrue(Builder.buildFrom(Ids.class.getDeclaredMethod("getEIDE"), validator).isFinal());
		assertFalse(Builder.buildFrom(NestedEntity.class.getDeclaredMethod("getString"), validator).isFinal());
	}

	// TODO could maybe be extended to lists or so
	@Test
	public void validFinalTypes() throws NoSuchMethodException {
		assertTrue(Builder.buildFrom(FinalTypes.class.getDeclaredMethod("getInt"), validator).isFinal());
		assertTrue(Builder.buildFrom(FinalTypes.class.getDeclaredMethod("getByte"), validator).isFinal());
		assertTrue(Builder.buildFrom(FinalTypes.class.getDeclaredMethod("getOid"), validator).isFinal());
		try {
			Builder.buildFrom(FinalTypes.class.getDeclaredMethod("getObject"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Final is only supported for"));
		}
		// TODO add test if autoboxed primitive allows modification, which leaks into entity
	}

	private static interface FinalTypes extends Entity {
		@Final
		public Integer getInt();

		public void setInt(Integer i);

		@Final
		public byte getByte();

		public void setByte(byte b);

		@Final
		public ObjectId getOid();

		public void setOid(ObjectId id);

		@Final
		public Object getObject();

		public void setObject(Object o);
	}

	private static interface Ids extends Entity {
		@Id
		public String getEIDE();

		public Ids setEIDE(String id);

		@Final
		public String getId();

		public Ids setId(String id);

		public List getList();

		public Ids setList(List l);

		public List<Entity> getListEntity();

		public Ids setListEntity(List<Entity> l);

	}

	private static interface ComputedEntity extends Entity {
		@Computed(StringComputer.class)
		public String getComputed();
	}

	public static class StringComputer implements Computer<Entity, String> {
		@Override
		public String compute(Entity entity) {
			return "computed";
		}
	}

	public static interface ReferencedEntity extends Entity {
		@Reference
		public PrimitiveEntity getValidProp();

		public ReferencedEntity setValidProp(PrimitiveEntity pe);
	}

	public static interface InvalidReference extends Entity {
		@Reference
		public String getInvalidProp();

		public ReferencedEntity setInvalidProp(String s);
	}
}
