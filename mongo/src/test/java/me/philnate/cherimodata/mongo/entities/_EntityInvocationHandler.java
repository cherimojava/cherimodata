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
package me.philnate.cherimodata.mongo.entities;

import javax.validation.ConstraintViolationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.CollectibleCodec;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;

import me.philnate.cherimodata.mongo.CommonInterfaces;
import me.philnate.cherimodata.mongo.CommonInterfaces.PrimitiveEntity;
import me.philnate.cherimodata.mongo.TestBase;

import static me.philnate.cherimodata.mongo.CommonInterfaces.ExplicitIdEntity;
import static me.philnate.cherimodata.mongo.entities.EntityFactory.instantiate;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class _EntityInvocationHandler extends TestBase {

	@Mock
	MongoDatabase db;
	PrimitiveEntity pe;
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
			assertTrue(e.getMessage().contains("Unknown property"));
		}

		try {
			pe.get("undeclared");
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Unknown property"));
		}
	}

	// TODO we might want to extract/do additional tests in an external class once the need arises
	@Test
	public void verifyPropertyConstraintOnSet() {
		try {
			pe.setString(null);
			fail("should throw an exception");
		} catch (ConstraintViolationException e) {
			assertTrue(e.getConstraintViolations().iterator().next().getMessage().contains("null"));
		}
		try {
			pe.set("string", null);
			fail("should throw an exception");
		} catch (ConstraintViolationException e) {
			assertTrue(e.getConstraintViolations().iterator().next().getMessage().contains("null"));
		}
	}

	@Test
	public void failOnSaveDropIfNoMongoGiven() {
		try {
			pe.save();
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("Entity was created without MongoDB reference."));
		}
		try {
			pe.drop();
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("Entity was created without MongoDB reference."));
		}
		try {
			pe.load("doesn't matter");
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("Entity was created without MongoDB reference."));
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
			assertTrue(e.getMessage().contains("An explicit defined Id "));
		}
		pe.setName("some");
		pe.save();
	}

	@Test
	public void saveOnlyIfNeeded() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		assertFalse(pe.save());// TODO maybe we want to save it?
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
			assertTrue(e.getConstraintViolations().iterator().next().getMessage().contains("null"));
		}
	}

	@Test
	public void sealEntity() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setInteger(1).seal();
		try {
			pe.setInteger(3);
			fail("should throw an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("sealed"));
		}
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
	public void entitientDifferentContentNotEqual() {
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
        assertEquals(pe1.hashCode(),pe2.hashCode());
    }

    @Test
    public void hashCodeDifferentOnDifferentContent() {
        PrimitiveEntity pe1 = factory.create(PrimitiveEntity.class);
        PrimitiveEntity pe2 = factory.create(PrimitiveEntity.class);
        pe1.setInteger(Integer.valueOf(12));
        pe2.setInteger(2);

        pe1.setString("omething");
        pe2.setString("some" + "thing");
        assertNotEquals(pe1.hashCode(),pe2.hashCode());
    }

	@Test
	public void computedProperty() {
		CommonInterfaces.ComputedPropertyEntity cpe = factory.create(CommonInterfaces.ComputedPropertyEntity.class);
		cpe.setString("one").setInteger(1);
		assertEquals("one1", cpe.getComputed());
	}
}
