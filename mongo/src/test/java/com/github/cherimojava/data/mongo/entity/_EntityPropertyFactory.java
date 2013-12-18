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

import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.TestBase;

import static com.github.cherimojava.data.mongo.CommonInterfaces.*;
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
	public void noMultipleGetterForProperty() {
		try {
			factory.create(MultipleGetterForProperty.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Entity contains already a property"));
		}
	}

	@Test
	public void noAdditionalSetter() {
		try {
			factory.create(AdditionalSetter.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Found setter methods which have no matching getter"));
		}
	}

	@Test
	public void noDuplicateSetter() {
		try {
			factory.create(AdditionalSetterForProperty.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Multiple setter found"));
		}
	}

	@Test
	public void noAllowedMethodDuplication() {
		try {
			factory.create(NotAllowedMethodName.class);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Don't write custom"));
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

		public void setString(String n, String a);
	}

	private static interface NotAllowedMethodName extends Entity {
		public String equals(String some);
	}

}
