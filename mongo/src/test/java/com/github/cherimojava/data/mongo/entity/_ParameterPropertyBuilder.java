/**
 *    Copyright [cherimojava (http://github.com/philnate/cherimojava.git)]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */
package com.github.cherimojava.data.mongo.entity;

import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.TestBase;
import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.annotation.Id;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;
import com.github.cherimojava.data.mongo.entity.annotation.Transient;

import static com.github.cherimojava.data.mongo.CommonInterfaces.*;
import static com.github.cherimojava.data.mongo.entity.ParameterProperty.Builder;
import static org.junit.Assert.*;

public class _ParameterPropertyBuilder extends TestBase {

	Validator validator;

	@Before
	public void init() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	public void validateParameterOnlyOnGet() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidEntity.class.getDeclaredMethod("setString"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Validation is done on get methods only"));
		}
	}

	@Test
	public void validateNoParameterForGet() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidEntity.class.getDeclaredMethod("getString", String.class), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Get methods can't have parameters"));
		}
	}

	@Test
	public void validateReturnParameterForGet() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidEntity.class.getDeclaredMethod("getNoReturnParam"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("did not declare a return type"));
		}
	}

	@Test
	public void validateSetGetTypesMatch() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidEntity.class.getDeclaredMethod("getTypeMismatch"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("no corresponding setter method"));
		}
	}

	@Test
	public void errorSetNoParam() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidEntity.class.getDeclaredMethod("getNoParamSet"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("no corresponding setter method"));
		}
	}

	@Test
	public void errorMultiParameterSet() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidEntity.class.getDeclaredMethod("getMultiParamSet"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("no corresponding setter method"));
		}
	}

	@Test
	public void detectFluent() throws NoSuchMethodException {
		assertFalse(Builder.buildFrom(FluentEntity.class.getMethod("getNotFluent"), validator).isFluent());
		assertTrue(Builder.buildFrom(FluentEntity.class.getMethod("getOwnClass"), validator).isFluent());
		assertTrue(Builder.buildFrom(FluentEntity.class.getMethod("getSuperClass"), validator).isFluent());

		try {
			Builder.buildFrom(FluentEntity.class.getMethod("getOtherEntity"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Only Superclasses of"));
		}

		try {
			Builder.buildFrom(FluentEntity.class.getMethod("getUnrelatedClass"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Only Superclasses of"));
		}
	}

	@Test
	public void detectType() throws NoSuchMethodException {
		assertEquals(String.class, Builder.buildFrom(FluentEntity.class.getMethod("getNotFluent"), validator).getType());
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
	public void noSetForComputedProperty() throws NoSuchMethodException {
		try {
			Builder.buildFrom(InvalidEntity.class.getDeclaredMethod("getComputed"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("computed"));
		}

		assertTrue(Builder.buildFrom(ComputedEntity.class.getDeclaredMethod("getComputed"), validator).isComputed());
	}

	@Test
	public void validReference() throws NoSuchMethodException {
		Builder.buildFrom(ReferencedEntity.class.getDeclaredMethod("getValidProp"), validator);
	}

	@Test
	public void invalidReference() throws NoSuchMethodException {
		try {
			Builder.buildFrom(ReferencedEntity.class.getDeclaredMethod("getInvalidProp"), validator);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Cant declare"));
		}
	}

	@Test
	public void referenceTypeDetection() throws NoSuchMethodException {
		assertTrue(Builder.buildFrom(ReferencingEntity.class.getDeclaredMethod("getPE"), validator).isReference());
		assertTrue(Builder.buildFrom(LazyLoadingEntity.class.getDeclaredMethod("getPE"), validator).isReference());
		assertTrue(Builder.buildFrom(LazyLoadingEntity.class.getDeclaredMethod("getPE"), validator).isLazyLoaded());
		assertFalse(Builder.buildFrom(NestedEntity.class.getDeclaredMethod("getPE"), validator).isReference());
		assertFalse(Builder.buildFrom(NestedEntity.class.getDeclaredMethod("getPE"), validator).isLazyLoaded());
	}

	@SuppressWarnings("unused")
	private static interface FluentEntity extends Entity {
		@Transient
		public String getNotFluent();

		public void setNotFluent(String s);

		public String getOwnClass();

		public FluentEntity setOwnClass(String s);

		public String getSuperClass(); // this should cover the Object case as well

		public Entity setSuperClass(String s);

		public String getOtherEntity();

		public InvalidEntity setOtherEntity(String s);

		public String getUnrelatedClass();

		public String setUnrelatedClass(String s);
	}

	private static interface Ids extends Entity {
		@Id
		public String getEIDE();

		public Ids setEIDE(String id);

		public String getId();

		public Ids setId(String id);
	}

	private static interface InvalidEntity extends Entity {
		public void setString();

		public void getString(String s);

		public void getNoReturnParam();

		public Integer getTypeMismatch();

		public void setTypeMismatch(String s);

		public String getNoParamSet();

		public void setNoParamSet();

		public String getMultiParamSet();

		public void setMultiParamSet(String one, String two);

		@Computed(StringComputer.class)
		public String getComputed();

		public void setComputed(String s);
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

		@Reference
		public String getInvalidProp();

		public ReferencedEntity setInvallidProp(String s);
	}
}
