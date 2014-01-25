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

import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.TestBase;

import static com.github.cherimojava.data.mongo.CommonInterfaces.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class _EntityPropertyFactory extends TestBase {

	EntityPropertyFactory factory;

	@Before
	public void init() {
		factory = new EntityPropertyFactory();
	}

	@Test
	public void singletonEntityProperties() {
		assertTrue(factory.create(PrimitiveEntity.class) == factory.create(PrimitiveEntity.class));
	}

	@Test
	public void collectionName() {
		assertEquals("primitiveEntity", factory.create(PrimitiveEntity.class).getCollectionName());
		assertEquals("Nested", factory.create(NestedEntity.class).getCollectionName());
	}

	@Test
	public void returnTypeOnGet() throws NoSuchMethodException {
		try {
			factory.validateGetter(InvalidEntity.class.getDeclaredMethod("getNoReturnParam"));
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("did not declare a return type"));
		}
	}

	@Test
	public void noParameterOnGet() throws NoSuchMethodException {
		try {
			factory.validateGetter(InvalidEntity.class.getDeclaredMethod("getString", String.class));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Get methods can't have parameters"));
		}
	}

	// TODO move to _EntityPropertyFactory
	@Test
	public void noMultiParameterSet() throws NoSuchMethodException {
		try {
			factory.validateSetter(InvalidEntity.class.getDeclaredMethod("setMultiParamSet", String.class, String.class));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Set methods must have one parameter"));
		}
	}

	@Test
	public void referenceMustBeOfTypeEntity() throws NoSuchMethodException {
		try {
			factory.validateGetter(InvalidEntity.class.getDeclaredMethod("getInvalidProp"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Cant declare"));
		}
	}

	@Test
	public void setterReturnOnlyEntity() throws NoSuchMethodException {
		try {
			factory.validateSetter(FluentEntity.class.getMethod("getOtherEntity"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Only Superclasses of"));
		}

		try {
			factory.validateSetter(FluentEntity.class.getMethod("getUnrelatedClass"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Only Superclasses of"));
		}
	}

	@Test
	public void noMultipleGetterForProperty() {
		try {
			factory.create(MultipleGetterForProperty.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Entity contains already a property"));
		}
	}

	@Test
	public void noAdditionalSetter() {
		try {
			factory.create(AdditionalSetter.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(),
					containsString("You can only declare setter methods if there's a matching getter."));
		}
	}

	@Test
	public void noDuplicateSetter() {
		try {
			factory.create(AdditionalSetterForProperty.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(),
					containsString("You can only declare setter methods if there's a matching getter."));
		}
	}

	@Test
	public void noAllowedMethodDuplication() {
		try {
			factory.create(NotAllowedMethodName.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Don't write custom"));
		}
	}

	@Test
	public void propertiesAreInherited() {
		EntityProperties props = factory.create(InheritedEntity.class);
		assertNotNull(props.getProperty("string"));
		assertNotNull(props.getProperty("Integer"));
		assertNotNull(props.getProperty("anotherString"));
		assertNull(props.getProperty("noProperty"));
	}

	@Test
	public void explicitIdDetection() {
		assertFalse(factory.create(PrimitiveEntity.class).hasExplicitId());
		assertTrue(factory.create(ExplicitIdEntity.class).hasExplicitId());
		assertTrue(factory.create(ImplicitIdEntity.class).hasExplicitId());
	}

	@Test
	public void noSetForComputedProperty() throws NoSuchMethodException {
		try {
			factory.validateSetter(InvalidEntity.class.getDeclaredMethod("setComputed", String.class));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("computed"));
		}
	}

	@Test
	public void noGetterForSetter() throws NoSuchMethodException {
		try {
			factory.validateSetter(InvalidEntity.class.getDeclaredMethod("setNoGetter", String.class));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(),
					containsString("You can only declare setter methods if there's a matching getter"));
		}
	}

	private static interface InheritedEntity extends PrimitiveEntity {
		public String getAnotherString();

		public void setAnotherString(String s);
	}

	private static interface MultipleGetterForProperty extends Entity {
		public String getString();

		public void setString(String s);

		@Named("string")
		public String getGnirts();

		public void setGnirts(String s);
	}

	private static interface AdditionalSetter extends Entity {
		public String getString();

		public void setString(String s);

		public void setNothing(String not);
	}

	private static interface AdditionalSetterForProperty extends Entity {
		public String getString();

		public void setString(String s);

		public void setString(int n);
	}

	private static interface NotAllowedMethodName extends Entity {
		public String equals(String some);
	}
}
