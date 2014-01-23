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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.CollectibleCodec;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;

import com.github.cherimojava.data.mongo.CommonInterfaces;
import com.github.cherimojava.data.mongo.TestBase;
import com.google.common.collect.Lists;

import static com.github.cherimojava.data.mongo.CommonInterfaces.*;
import static com.github.cherimojava.data.mongo.entity.EntityFactory.instantiate;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class _EntityInvocationHandler extends TestBase {

	@Mock
	MongoDatabase db;
	CommonInterfaces.PrimitiveEntity pe;
	EntityInvocationHandler handler;
	EntityFactory factory;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		handler = new EntityInvocationHandler(new EntityPropertyFactory().create(PrimitiveEntity.class));
		pe = instantiate(PrimitiveEntity.class, handler);
		factory = new EntityFactory(db);
		when(db.getCollection(anyString(), any(CollectibleCodec.class))).thenReturn(mock(MongoCollection.class));
	}

	@Test
	public void entityClass() {
		assertEquals(PrimitiveEntity.class, pe.entityClass());
	}

	/**
	 * This test verifies that non annotated methods get truncated by set/get for writing and reading a property from
	 * the underlying structure
	 */
	@Test
	public void basicSetGet() {
		String v = "value";
		pe.setString(v).setInteger(1);
		// verify set value
		assertEquals(v, handler.data.get("string"));// direct
		assertEquals(v, pe.get("string"));// generic access from Entity
		assertEquals(v, pe.getString());// through getter
	}

	@Test
	public void correctNameResolving() {
		Integer one = 1;
		pe.setInteger(one);
		assertEquals(one, pe.getInteger());
		assertEquals(one, (Integer) pe.get("Integer"));
	}

	@Test
	public void fluentAPISameClass() {
		PrimitiveEntity ret = pe.setString("something");
		assertTrue(pe == ret);
	}

	@Test
	public void fluentAPISuperClass() {
		Entity ent = pe.setInteger(1);
		assertTrue(pe == ent);
	}

	@Test
	public void barfOnUnknownProperty() {
		try {
			pe.set("undeclared", null);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Unknown property"));
		}

		try {
			pe.get("undeclared");
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Unknown property"));
		}
	}

	@Test
	public void verifyPropertyConstraintOnSet() {
		try {
			pe.setString(null);
			fail("should throw an exception");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().iterator().next().getMessage(), containsString("null"));
		}
		try {
			pe.set("string", null);
			fail("should throw an exception");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().iterator().next().getMessage(), containsString("null"));
		}
	}

	@Test
	public void failOnSaveDropIfNoMongoGiven() {
		try {
			pe.save();
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("Entity was created without MongoDB reference."));
		}
		try {
			pe.drop();
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("Entity was created without MongoDB reference."));
		}
		try {
			pe.load("doesn't matter");
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("Entity was created without MongoDB reference."));
		}
	}

	@Test
	public void noFailOnSaveDropIfMongoGiven() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.save();
		pe.drop();
	}

	@Test
	public void explicitIdMustBeSetOnSave() {
		ExplicitIdEntity pe = factory.create(ExplicitIdEntity.class);
		pe.setName("sme").setName(null);// else it won't try to save
		try {
			pe.save();
			fail("should throw an exception");
		} catch (NullPointerException e) {
			assertThat(e.getMessage(), containsString("An explicit defined Id "));
		}
		pe.setName("some");
		pe.save();
	}

	@Test
	public void saveOnlyIfNeeded() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		assertFalse(pe.save());
		assertTrue(pe.setString("some").save());
		assertFalse(pe.save()); // no further changes
		assertFalse(pe.setString("some").save()); // nothing changed
		assertTrue(pe.set("Integer", 2).save());
		assertFalse(pe.set("Integer", 2).save());// nothing changed
		assertFalse(pe.save());// nothing changed
	}

	@Test
	public void verifyPropertyConstraintOnSave() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setInteger(1);// need to set something so that we actually try to save it
		try {
			pe.save();
			fail("should throw an exception");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().iterator().next().getMessage(), containsString("null"));
		}
	}

	@Test
	public void sealEntityPut() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setInteger(1).seal();
		try {
			pe.setInteger(3);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("sealed"));
		}
	}

	@Test
	public void sealEntityAdd() {
		AddEntity ae = factory.create(AddEntity.class);
		ae.addString("one");
		ae.seal();
		try {
			ae.addString("two");
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("sealed"));
		}
	}

	@Test
	public void addCreatesCollectionIfEmpty() {
		EntityFactory.setDefaultClass(List.class, LinkedList.class);
		AddEntity ae = factory.create(AddEntity.class);
		ae.addString("one");
		List<String> strings = ae.getString();

		assertNotNull(strings);
		assertEquals(1, strings.size());
		assertEquals(LinkedList.class, strings.getClass());
	}

	@Test
	public void eihGetsView() {
		AddEntity ae = factory.create(AddEntity.class);
		factory.setDefaultClass(List.class, LinkedList.class);
		ae.addString("one");
		List<String> strings = ae.getString();

		assertNotNull(strings);
		assertEquals(1, strings.size());
		assertEquals(LinkedList.class, strings.getClass());
	}

	@Test
	public void testDefaultsImplementations() {
		AddEntity ae = factory.create(AddEntity.class);
		ae.addString("one");
		List<String> strings = ae.getString();

		assertNotNull(strings);
		assertEquals(1, strings.size());
		assertEquals(ArrayList.class, strings.getClass());
	}

	@Test
	public void addOnExistingList() {
		AddEntity ae = factory.create(AddEntity.class);
		ae.setString(Lists.newArrayList("one", "two"));
		ae.addString("three");
		List<String> strings = ae.getString();
		assertNotNull(strings);
		assertEquals(3, strings.size());
		assertTrue(strings.contains("one"));
		assertTrue(strings.contains("two"));
		assertTrue(strings.contains("three"));
	}

	@Test
	public void nullNotEqual() {
		assertFalse(factory.create(PrimitiveEntity.class).equals(null));
	}

	@Test
	public void differentClassesNotEqual() {
		assertFalse(factory.create(PrimitiveEntity.class).equals("something"));
	}

	@Test
	public void differentEntityNotEqual() {
		assertFalse(factory.create(PrimitiveEntity.class).equals(factory.create(Entity.class)));
	}

	@Test
	public void sameInstanceEquals() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		assertTrue(pe.equals(pe));
	}

	@Test
	public void entitiesSameContentEquals() {
		PrimitiveEntity pe1 = factory.create(PrimitiveEntity.class);
		PrimitiveEntity pe2 = factory.create(PrimitiveEntity.class);
		pe1.setInteger(Integer.valueOf(12));
		pe2.setInteger(12);

		pe1.setString("something");
		pe2.setString("some" + "thing");

		assertTrue(pe1.equals(pe2));
	}

	@Test
	public void entityDifferentContentNotEqual() {
		PrimitiveEntity pe1 = factory.create(PrimitiveEntity.class);
		PrimitiveEntity pe2 = factory.create(PrimitiveEntity.class);
		pe1.setInteger(Integer.valueOf(12));
		pe2.setInteger(2);

		pe1.setString("something");
		pe2.setString("some" + "thing");

		assertFalse(pe1.equals(pe2));
	}

	@Test
	public void hashCodeEqualsOnSameContent() {
		PrimitiveEntity pe1 = factory.create(PrimitiveEntity.class);
		PrimitiveEntity pe2 = factory.create(PrimitiveEntity.class);
		pe1.setInteger(Integer.valueOf(12));
		pe2.setInteger(12);

		pe1.setString("something");
		pe2.setString("some" + "thing");
		assertEquals(pe1.hashCode(), pe2.hashCode());
	}

	@Test
	public void hashCodeDifferentOnDifferentContent() {
		PrimitiveEntity pe1 = factory.create(PrimitiveEntity.class);
		PrimitiveEntity pe2 = factory.create(PrimitiveEntity.class);
		pe1.setInteger(Integer.valueOf(12));
		pe2.setInteger(2);

		pe1.setString("omething");
		pe2.setString("some" + "thing");
		assertNotEquals(pe1.hashCode(), pe2.hashCode());
	}

	@Test
	public void computedProperty() {
		ComputedPropertyEntity cpe = factory.create(ComputedPropertyEntity.class);
		cpe.setString("one").setInteger(1);
		assertEquals("one1", cpe.getComputed());
	}

	@Test
	public void toStringNoParamValidation() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setInteger(1);
		assertJson(sameJSONAs("{ \"Integer\" : 1}"), pe.toString());
	}

	@Test
	public void toStringExternalReferences() {
		ReferencingEntity re = factory.create(ReferencingEntity.class);
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setInteger(1);
		ObjectId id = new ObjectId();
		pe.set("_id", id);
		re.setPE(pe.setString("some"));
		re.setInteger(5);
		assertJson(
				sameJSONAs("{ \"PE\" : { \"$id\" : { \"$oid\" : \"" + id.toHexString() + "\" } }, \"Integer\" : 5 }"),
				re.toString());
	}

	@Test
	public void toStringWriteIdIfSet() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setInteger(1);
		assertJson(sameJSONAs("{ \"Integer\" : 1}"), pe.toString());
		ObjectId id = new ObjectId();
		pe.set("_id", id);
		assertJson(sameJSONAs("{\"_id\":{\"$oid\":\"" + id.toString() + "\"},\"Integer\":1}"), pe.toString());
	}

	@Test
	public void toStringNestedEntity() {
		NestedEntity ne = factory.create(NestedEntity.class);
		ne.setString("value");
		ne.setPE((PrimitiveEntity) pe.setInteger(4));
		assertJson(sameJSONAs("{\"string\": \"value\", \"PE\":{\"Integer\":4}}"), ne.toString());
	}
}
