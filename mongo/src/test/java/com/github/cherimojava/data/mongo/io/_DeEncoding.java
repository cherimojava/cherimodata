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
package com.github.cherimojava.data.mongo.io;

import static com.github.cherimojava.data.mongo.CommonInterfaces.*;
import static com.github.cherimojava.data.mongo.entity.Entity.ID;
import static com.github.cherimojava.data.mongo.entity.EntityFactory.instantiate;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.getCollectionName;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mongodb.MongoHelper.getMongoDatabase;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.Document;
import org.mongodb.MongoDatabase;

import com.github.cherimojava.data.mongo.MongoBase;
import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.github.cherimojava.data.mongo.entity.annotation.Id;
import com.github.cherimojava.data.mongo.entity.annotation.Transient;
import com.google.common.collect.Lists;

//TODO primitive types not working yet

@SuppressWarnings("unchecked")
public class _DeEncoding extends MongoBase {

	MongoDatabase db;
	EntityFactory factory;

	@Before
	public void mongoDBSetup() {
		db = getMongoDatabase(client, "deencoding");
		factory = new EntityFactory(db);
	}

	@After
	public void mongoCleanUp() {
		db.tools().drop();
	}

	/**
	 * Tests if de/encoding a simple Entity with only primitive data types work
	 */
	@Test
	public void basicDeEncoding() {
		EntityCodec codec = new EntityCodec<>(db, EntityFactory.getProperties(PrimitiveEntity.class));

		PrimitiveEntity pe = instantiate(PrimitiveEntity.class);
		pe.setString("value");
		pe.setInteger(123);

		StringWriter swriter = new StringWriter();
		JsonWriter jwriter = new JsonWriter(swriter);

		codec.encode(jwriter, pe, null);

		assertJson(sameJSONAs("{ \"Integer\" : 123, \"string\" : \"value\" }"), swriter.toString());

		JsonReader jreader = new JsonReader(swriter.toString());

		PrimitiveEntity read = decode(codec, jreader, PrimitiveEntity.class);

		assertEquals(pe.getString(), read.getString());
		assertEquals(pe.getInteger(), read.getInteger());
	}

	@Test
	public void basicDeEncodingDB() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setString("some string");
		pe.setInteger(12);
		pe.save();

		Document doc = db.getCollection("primitiveEntity").find().limit(1).iterator().next();
		assertJson(sameJSONAs("{ \"Integer\" : 12, \"string\" : \"some string\" }").allowingExtraUnexpectedFields(),
				doc.toString());

		PrimitiveEntity read = factory.load(PrimitiveEntity.class, doc.get("_id"));
		assertEquals(pe, read);
		assertEquals(read, pe.load(doc.get("_id")));
	}

	@Test
	public void noDuplicateIdWritten() {
		ExplicitIdEntity eid = factory.fromJson(ExplicitIdEntity.class, "{\"_id\":\"explicit\"}");
		assertEquals("explicit", eid.getName());
		EntityCodec<ExplicitIdEntity> codec = new EntityCodec<>(db, factory.getProperties(ExplicitIdEntity.class));

		StringWriter swriter = new StringWriter();
		JsonWriter writer = new JsonWriter(swriter);
		codec.encode(writer, eid, null);
		assertEquals(1, StringUtils.countMatches(swriter.toString(), "explicit"));

		ExplicitIdEntity eid2 = factory.create(ExplicitIdEntity.class);
		eid2.setName("once");
		swriter = new StringWriter();
		writer = new JsonWriter(swriter);
		codec.encode(writer, eid2, null);
		assertEquals(1, StringUtils.countMatches(swriter.toString(), "once"));
	}

	@Test
	public void computedStoredCorrectly() {
		ComputedPropertyEntity cpe = factory.create(ComputedPropertyEntity.class);
		cpe.setString("some");
		cpe.setInteger(1);
		cpe.save();

		Document doc = db.getCollection("computedPropertyEntity").find().limit(1).iterator().next();
		assertJson(
				sameJSONAs("{ \"string\":\"some\", \"Integer\":1, \"computed\":\"some1\"}").allowingExtraUnexpectedFields(),
				doc.toString());
		assertEquals(doc.get("_id"), cpe.get("_id"));
	}

	@Test
	public void explicitIdBasicDeEncodingDB() {
		ExplicitIdEntity ee = factory.create(ExplicitIdEntity.class);
		ee.setName("some");
		ee.save();

		Document doc = db.getCollection("explicitIdEntity").find().limit(1).iterator().next();
		assertJson(sameJSONAs("{ \"_id\" : \"some\" }").allowingExtraUnexpectedFields(), doc.toString());

		ExplicitIdEntity read = factory.load(ExplicitIdEntity.class, doc.get("_id"));
		assertEquals(ee, read);
	}

	@Test
	public void transientPropertyDeEncodingDB() {
		TransientEntity te = factory.create(TransientEntity.class);
		te.setIdentity("some");
		te.setTransient("trans");

		te.save();

		Document doc = db.getCollection("transientEntity").find(new Document("_id", "some")).iterator().next();
		assertJson(sameJSONAs("{ \"_id\" : \"some\" }"), doc.toString());

		TransientEntity read = factory.load(TransientEntity.class, doc.get("_id"));
		assertEquals(te.getIdentity(), read.getIdentity());
		assertNull(read.getTransient());
	}

	@Test
	public void transientPropertyNotDecoded() {
		Document doc = new Document("_id", "different");
		doc.append("transient", "ignored");
		db.getCollection("transientEntity").save(doc);

		TransientEntity read = factory.load(TransientEntity.class, doc.get("_id"));
		assertEquals("different", read.getIdentity());
		assertNull(read.getTransient());
	}

	/**
	 * tests if de/encoding a complex (nested) entity works
	 */
	@Test
	public void nestedDeEncoding() {
		EntityCodec codec = new EntityCodec<>(db, EntityFactory.getProperties(NestedEntity.class));

		PrimitiveEntity pe = instantiate(PrimitiveEntity.class);
		pe.setString("value");
		pe.setInteger(123);

		NestedEntity ce = instantiate(NestedEntity.class);
		ce.setString("outer");
		ce.setPE(pe);

		StringWriter swriter = new StringWriter();
		JsonWriter jwriter = new JsonWriter(swriter);

		codec.encode(jwriter, ce, null);

		assertJson(sameJSONAs("{ \"string\" : \"outer\", \"PE\" : { \"Integer\" : 123, \"string\" : \"value\" } }"),
				swriter.toString());

		JsonReader jreader = new JsonReader(swriter.toString());
		NestedEntity ceRead = decode(codec, jreader, NestedEntity.class);
		PrimitiveEntity peRead = ceRead.getPE();

		assertEquals(pe.getInteger(), peRead.getInteger());
		assertEquals(pe.getString(), peRead.getString());
		assertEquals(ce.getString(), ceRead.getString());
	}

	@Test
	public void referenceDeEncodingDB() {
		ReferencingEntity re = factory.create(ReferencingEntity.class);
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);

		pe.setString("nestedString");
		re.setPE(pe);
		re.setString("some");

		re.save();
		Document reRead = db.getCollection("referencingEntity").find(new Document(ID, re.get(ID))).iterator().next();
		System.out.println(reRead);
		ObjectId dbRef = (ObjectId) ((Document) reRead.get("PE")).get("$id");
		assertNotNull(dbRef);
		assertNotNull(pe.get(ID));
		assertEquals(pe.get(ID), dbRef);
		Document peRead = db.getCollection("primitiveEntity").find(new Document(ID, pe.get(ID))).iterator().next();

		assertEquals(pe.get(ID), peRead.get(ID));
		assertEquals(pe.getString(), peRead.get("string"));

		ReferencingEntity read = factory.load(ReferencingEntity.class, re.get(ID));
		assertEquals(re, read);
	}

	@Test
	public void ignoreUnknownProps() {
		PrimitiveEntity pe = factory.fromJson(PrimitiveEntity.class,
				"{\"string\":\"some\",\"Integer\":1,\"unknown\":\"dontfail\"}");
		assertEquals(1, (int) pe.getInteger());
		assertEquals("some", pe.getString());
	}

	@Test
	public void ignoreUnknownSubtype() {
		PrimitiveEntity pe = factory.fromJson(PrimitiveEntity.class,
				"{\"unknown\": {\"string\":\"dontfail\"},\"string\":\"some\",\"Integer\":1}");
		assertEquals(1, (int) pe.getInteger());
		assertEquals("some", pe.getString());
	}

	@Test
	public void loadedEntityCanBeSaved() {
		ReferencingEntity re = factory.create(ReferencingEntity.class);
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);

		pe.setString("nestedString");
		re.setPE(pe);
		re.setString("some");

		re.save();
		ReferencingEntity read = factory.load(ReferencingEntity.class, re.get(ID));
		read.setInteger(2).save();
		pe.setInteger(4).save();
	}

	@Test
	@Ignore
	public void lazyLoadEntity() {
		// right now we can check that it's not loaded by checking to String which then shouldn't show the lazy loaded
		// content, but this might be not what we want
		LazyLoadingEntity lle = factory.create(LazyLoadingEntity.class);
		lle.setString("myString");
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		pe.setString("some");
		lle.setPE(pe);

		lle.save();

		LazyLoadingEntity read = factory.load(LazyLoadingEntity.class, lle.get(ID));
		assertFalse(read.toString(), read.toString().contains("some"));
	}

	@Test
	public void collectionDeEncoding() {
		EntityCodec codec = new EntityCodec<>(db, EntityFactory.getProperties(CollectionEntity.class));

		CollectionEntity ce = instantiate(CollectionEntity.class);
		ce.setArrayStrings(new String[] { "one", "two" });
		ce.setStrings(Lists.newArrayList("three", "four"));

		StringWriter swriter = new StringWriter();
		JsonWriter jwriter = new JsonWriter(swriter);

		codec.encode(jwriter, ce, null);
		assertJson(sameJSONAs("{ \"arrayStrings\": [\"one\",\"two\"],\"strings\":[\"three\",\"four\"] }"),
				swriter.toString());

		JsonReader jreader = new JsonReader(swriter.toString());
		CollectionEntity ceRead = decode(codec, jreader, CollectionEntity.class);
		assertJson(sameJSONAs("{ \"arrayStrings\": [\"one\",\"two\"],\"strings\":[\"three\",\"four\"] }"),
				ceRead.toString());
	}

	@Test
	public void collectionDeEncodingDB() {
		EntityCodec codec = new EntityCodec<>(db, EntityFactory.getProperties(Listed.class));
		Listed listed = factory.create(Listed.class);
		listed.setList(Lists.newArrayList(factory.create(PrimitiveEntity.class).setString("nested")));
		StringWriter swriter = new StringWriter();
		JsonWriter jwriter = new JsonWriter(swriter);

		codec.encode(jwriter, listed, null);
		assertJson(sameJSONAs("{ \"list\": [{\"string\": \"nested\"}]}"), swriter.toString());
		JsonReader jsonReader = new JsonReader(swriter.toString());
		Listed read = decode(codec, jsonReader, Listed.class);
		assertNotNull(read.getList());
		assertEquals(listed.getList().get(0), read.getList().get(0));

        listed.save();
        Listed r = factory.load(Listed.class,listed.get(ID));
        assertEquals(listed,r);
	}

	@Test
	public void enumDeEncoding() {
		EntityCodec codec = new EntityCodec<>(db, EntityFactory.getProperties(EnumEntity.class));
		EnumEntity ee = instantiate(EnumEntity.class);
		ee.setCategory(EnumEntity.Category.Misc);

		StringWriter swriter = new StringWriter();
		JsonWriter jwriter = new JsonWriter(swriter);

		codec.encode(jwriter, ee, null);
		assertJson(sameJSONAs("{ \"category\": \"Misc\" }"), swriter.toString());
		JsonReader jreader = new JsonReader(swriter.toString());
		EnumEntity eeRead = decode(codec, jreader, EnumEntity.class);
		assertEquals(ee, eeRead);
	}

	@Test
	public void enumDeEncodingUnknownEnum() {
		EntityCodec codec = new EntityCodec<>(db, EntityFactory.getProperties(EnumEntity.class));
		JsonReader jreader = new JsonReader("{ \"category\": \"miscellaneous\" }");
		try {
			decode(codec, jreader, EnumEntity.class);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("doesn't match any declared enum value of"));
		}
	}

	@Test
	public void saveDrop() {
		PrimitiveEntity pe = factory.create(PrimitiveEntity.class);
		factory.create(PrimitiveEntity.class).setString("don't delete").save();
		pe.setString("413").save();
		assertEquals(2, db.getCollection(getCollectionName(PrimitiveEntity.class)).find().count());
		pe.drop();
		assertEquals(1, db.getCollection(getCollectionName(PrimitiveEntity.class)).find().count());
	}

	private static interface TransientEntity extends Entity {
		@Transient
		public String getTransient();

		public TransientEntity setTransient(String trans);

		@Id
		public String getIdentity();

		public TransientEntity setIdentity(String id);
	}

	private static interface EnumEntity extends Entity {
		public EnumEntity setCategory(Category cat);

		public Category getCategory();

		public static enum Category {
			Misc,
			Other
		}
	}

	private interface Listed extends Entity<Listed> {

		public Listed setList(List<PrimitiveEntity> entities);

		public List<PrimitiveEntity> getList();
	}
}
