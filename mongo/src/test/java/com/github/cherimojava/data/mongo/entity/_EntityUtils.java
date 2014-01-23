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
		assertEquals("URL", decapitalize("URL"));

		assertEquals("CamelCase", capitalize("camelCase"));
		assertEquals("URL", capitalize("URL"));

		assertEquals("s", decapitalize(("S")));
		assertEquals("S", capitalize("s"));
		assertEquals("SE", decapitalize("SE"));
		assertEquals("SE", capitalize("SE"));
	}

	/**
	 * TestCase that method to property name is correctly resolved
	 */
	@Test
	public void validMethodNameResolution() throws NoSuchMethodException {
		assertEquals("string", getPojoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("getString")));
		assertEquals("string",
				getPojoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("setString", String.class)));
        assertEquals("string", getPojoNameFromMethod(AdderTest.class.getDeclaredMethod("addString", String.class, Integer.class)));
	}

	@Test
	public void pojoNameNotChangedByNameAnnotation() throws NoSuchMethodException {
		assertEquals("integer", getPojoNameFromMethod(PrimitiveEntity.class.getMethod("getInteger")));
	}

	/**
	 * TestCase that invalid method names aren't resolved
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

	@Test
	public void nameAnnotationForId() throws NoSuchMethodException {
		try {
			getMongoNameFromMethod(NameForId.class.getDeclaredMethod("getSomething"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("not allowed to use @Name annotation to declare id field"));
		}
	}

	@Test
	public void idAndNameAnnotationInvalid() throws NoSuchMethodException {
		try {
			getMongoNameFromMethod(NameForId.class.getDeclaredMethod("getBoth"));
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("You can not annotate a property with @Name and @Id"));
		}
	}

	@Test
	public void mongoNameResolution() throws NoSuchMethodException {
		assertEquals("string", getMongoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("getString")));
		assertEquals("Integer", getMongoNameFromMethod(PrimitiveEntity.class.getDeclaredMethod("getInteger")));
	}

	@Test
	public void collectionNameResolution() {
		assertEquals("primitiveEntity", getCollectionName(PrimitiveEntity.class));
		assertEquals("Nested", getCollectionName(NestedEntity.class));
	}

	private static interface NameForId extends Entity<NameForId> {
		@Named("_id")
		public String getSomething();

		@Named("something")
		@Id
		public String getBoth();
	}
}
