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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.Document;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;

import com.github.cherimojava.data.mongo.CommonInterfaces;
import com.github.cherimojava.data.mongo.TestBase;
import com.github.cherimojava.data.mongo.entity.annotation.Collection;
import com.github.cherimojava.data.mongo.entity.annotation.Index;
import com.github.cherimojava.data.mongo.entity.annotation.IndexField;
import com.github.cherimojava.data.mongo.io.EntityCodec;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class _EntityFactory extends TestBase {

	@Mock
	MongoDatabase db;
	@Mock
	MongoCollection<Document> coll;

	EntityFactory factory;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
		when(db.getCollection(anyString(), any(EntityCodec.class))).thenReturn(coll);
		factory = new EntityFactory(db);
	}

	@Test
	@Ignore
	public void indexCreation() {
		factory.create(IndexedEntity.class);

		verify(db).getCollection("collection");
		// TODO change to argument captor?
		// verify(coll).ensureIndex((DBObject) JSON.parse("{\"string\":1}"),
		// (DBObject) JSON.parse("{ \"name\" : \"single\"}"));
		// verify(coll).ensureIndex((DBObject) JSON.parse("{\"string\":-1,\"anotherString\":1}"),
		// (DBObject) JSON.parse("{ \"unique\" : true}"));
	}

	@Test
	public void referencedIndexFieldDoesNotExist() {
		try {
			factory.create(InvalidIndexedEntity.class);
			fail("should throw an exception");
		} catch (NullPointerException e) {
			assertThat(e.getMessage(), containsString("Index field"));
		}
	}

	@Test
	public void fromJSON() {
		CommonInterfaces.PrimitiveEntity pe = factory.fromJson(CommonInterfaces.PrimitiveEntity.class,
				"{\"string\": \"something\",\"Integer\":3}");
		assertEquals(pe.getString(), "something");
		assertEquals((int) pe.getInteger(), 3);
	}

	@Collection(indexes = {
			@Index(name = "single", value = { @IndexField(field = "string", order = IndexField.Ordering.ASC) }),
			@Index(value = { @IndexField(field = "string", order = IndexField.Ordering.DESC),
					@IndexField(field = "anotherString", order = IndexField.Ordering.ASC) }, unique = true) })
	@Named("collection")
	private interface IndexedEntity extends Entity<IndexedEntity> {
		public String getString();

		public IndexedEntity setString(String s);

		public String getAnotherString();

		public IndexedEntity setAnotherString(String s);

	}

	@Named("collection")
	@Collection(indexes = { @Index(name = "single", value = { @IndexField(field = "string", order = IndexField.Ordering.ASC) }) })
	private interface InvalidIndexedEntity extends Entity<InvalidIndexedEntity> {
	}
}
