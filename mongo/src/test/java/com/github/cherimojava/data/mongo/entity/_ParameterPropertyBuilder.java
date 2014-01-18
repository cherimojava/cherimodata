/**
 *    Copyright [cherimojava (http://github.com/cherimojava/cherimodata.git)]
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
	public void detectFluent() throws NoSuchMethodException {
		assertFalse(Builder.buildFrom(FluentEntity.class.getMethod("getNotFluent"), validator).isFluent());
		assertTrue(Builder.buildFrom(FluentEntity.class.getMethod("getOwnClass"), validator).isFluent());
		assertTrue(Builder.buildFrom(FluentEntity.class.getMethod("getSuperClass"), validator).isFluent());
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

	private static interface Ids extends Entity {
		@Id
		public String getEIDE();

		public Ids setEIDE(String id);

		public String getId();

		public Ids setId(String id);
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
}
