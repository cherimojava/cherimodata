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

import java.util.List;

import javax.inject.Named;

import org.junit.Test;

import com.github.cherimojava.data.mongo.TestBase;
import com.github.cherimojava.data.mongo.entity.annotation.Id;

import static com.github.cherimojava.data.mongo.CommonInterfaces.*;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class _EntityUtils extends TestBase {
	/**
	 * TestCase that the capitalization/decapitalization of names works as expected
	 */
	@Test
	public void capitalizationStringUtils() {
		assertEquals("camelCase", decapitalize("CamelCase"));
		assertEquals("CamelCase", capitalize("camelCase"));
		assertEquals("s", decapitalize("S"));
		assertEquals("S", capitalize("s"));

		assertEquals(capitalize("URL"), decapitalize("URL"));
		assertEquals(capitalize("SE"), decapitalize("SE"));
	}

	/**
	 * TestCase that method to property name is correctly resolved
	 */
	@Test
	public void validMethodNameResolution() throws NoSuchMethodException {
		assertEquals("string", getPojoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("getString")));
		assertEquals("string",
				getPojoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("setString", String.class)));
		assertEquals("string",
				getPojoNameFromMethod(AdderTest.class.getDeclaredMethod("addString", String.class, Integer.class)));
	}

	@Test
	public void pojoNameNotChangedByNameAnnotation() throws NoSuchMethodException {
		assertEquals("integer", getPojoNameFromMethod(PrimitiveEntity.class.getMethod("getInteger")));
	}

	/**
	 * TestCase that invalid methods like equals/get fail
	 */
	@Test
	public void invalidMethodNameResolution() throws NoSuchMethodException {
		try {
			getPojoNameFromMethod(Entity.class.getDeclaredMethod("equals", Object.class));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("retrieve name from this method"));
		}

		try {
			getPojoNameFromMethod(Entity.class.getDeclaredMethod("get", String.class));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("retrieve name from this method"));
		}
	}

	/**
	 * Check that it's not possible to declare Id through @Named
	 */
	@Test
	public void nameAnnotationForId() throws NoSuchMethodException {
		try {
			getMongoNameFromMethod(IdNameTest.class.getDeclaredMethod("getSomething"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("not allowed to use @Name annotation to declare id field"));
		}
	}

	/**
	 * check that if the method name is Id it's resolved into Entity.ID
	 */
	@Test
	public void nameIsId() throws NoSuchMethodException {
		assertEquals(Entity.ID, getMongoNameFromMethod(IdNameTest.class.getDeclaredMethod("getId")));
	}

	/**
	 * check that @Id annotation gets resolved into Entity.ID name
	 */
	@Test
	public void idAnnotated() throws NoSuchMethodException {
		assertEquals(Entity.ID, getMongoNameFromMethod(IdNameTest.class.getDeclaredMethod("getThisIsId")));
	}

	/**
	 * check that it's not possible to annotate a method with @Id and @Named
	 */
	@Test
	public void idAndNameAnnotationInvalid() throws NoSuchMethodException {
		try {
			getMongoNameFromMethod(IdNameTest.class.getDeclaredMethod("getBoth"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("You can not annotate a property with @Name and @Id"));
		}
	}

	/**
	 * check Method to Mongo name resolution
	 */
	@Test
	public void mongoNameResolution() throws NoSuchMethodException {
		assertEquals("string", getMongoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("getString")));
		assertEquals("Integer", getMongoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("getInteger")));
	}

	/**
	 * test the collection Name generation based on Classname
	 */
	@Test
	public void collectionNameResolution() {
		assertEquals("primitiveEntitys", getCollectionName(PrimitiveEntity.class));
		assertEquals("Nested", getCollectionName(NestedEntity.class));
	}

	/**
	 * check if method return type is assignable from the declaring class
	 */
	@Test
	public void isAssignableFromClass() throws NoSuchMethodException {
		assertTrue(EntityUtils.isAssignableFromClass(PrimitiveEntity.class.getDeclaredMethod("setInteger",
				Integer.class)));
		assertFalse(EntityUtils.isAssignableFromClass(NestedEntity.class.getDeclaredMethod("setString", String.class)));
	}

	/**
	 * check if the adder is correctly retrieved from the getter
	 */
	@Test
	public void adderFromGetter() throws NoSuchMethodException {
		assertEquals(AddEntity.class.getDeclaredMethod("addString", String.class),
				getAdderFromGetter(AddEntity.class.getDeclaredMethod("getString")));
		try {
			assertEquals(AddEntity.class.getDeclaredMethod("addString", String.class),
					getAdderFromGetter(AddEntity.class.getDeclaredMethod("getId")));
			fail("Should throw an exception");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("has no corresponding adder"));
		}
	}

	/**
	 * Test that Getter can be retrieved from Adder
	 */
	@Test
	public void getterFromAdder() throws NoSuchMethodException {
		assertEquals(AddEntity.class.getDeclaredMethod("getString"),
				getGetterFromAdder(AddEntity.class.getDeclaredMethod("addString", String.class)));
	}

	/**
	 * Test that Getter can be retrieved correctly from Setter
	 */
	@Test
	public void getterFromSetter() throws NoSuchMethodException {
		assertEquals(PrimitiveEntity.class.getDeclaredMethod("getString"),
				getGetterFromSetter(PrimitiveEntity.class.getDeclaredMethod("setString", String.class)));
		assertEquals(AddEntity.class.getDeclaredMethod("getString"),
				getGetterFromSetter(AddEntity.class.getDeclaredMethod("setString", List.class)));
		try {
			getGetterFromSetter(IdNameTest.class.getDeclaredMethod("setLame", String.class));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("has no corresponding getter"));
		}
	}

	/**
	 * Test that Setter can be retrieved correctly from a Getter
	 */
	@Test
	public void setterFromGetter() throws NoSuchMethodException {
		assertEquals(PrimitiveEntity.class.getDeclaredMethod("setString", String.class),
				getSetterFromGetter(PrimitiveEntity.class.getDeclaredMethod("getString")));
		assertEquals(AddEntity.class.getDeclaredMethod("setString", List.class),
				getSetterFromGetter(AddEntity.class.getDeclaredMethod("getString")));
		try {
			getSetterFromGetter(IdNameTest.class.getDeclaredMethod("getSomething"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("has no corresponding setter"));
		}
	}

	private static interface IdNameTest extends Entity<IdNameTest> {
		@Named("_id")
		public String getSomething();

		@Named("something")
		@Id
		public String getBoth();

		public String getId();

		@Id
		public String getThisIsId();

		public String setLame(String s);
	}
}
