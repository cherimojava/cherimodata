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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.cherimojava.data.mongo.CommonInterfaces;
import com.github.cherimojava.data.mongo.TestBase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class _EntityFactory extends TestBase {

	@Mock
	MongoDatabase db;

	@Mock
	MongoCollection coll;

	EntityFactory factory;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
		when(db.getCollection(anyString())).thenReturn(coll);
		when(coll.withCodecRegistry(any(CodecRegistry.class))).thenReturn(coll);
		when(coll.withDocumentClass(any(Class.class))).thenReturn(coll);
		factory = new EntityFactory(db);
	}

	@Test
	public void defaultClassesOnlyForInterfaces() {
		factory.setDefaultClass(List.class, ArrayList.class);
		try {
			factory.setDefaultClass(ArrayList.class, ArrayList.class);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Can set default Classes only for interfaces"));
		}
	}

	@Test
	public void defaultClassMustBeClass() {
		try {
			factory.setDefaultClass(List.class, List.class);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Default class can't be an interface itself"));
		}
	}

	@Test
	public void mustHaveEmptyConstructor() {
		try {
			factory.setDefaultClass(List.class, NoPubList.class);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(),
					containsString("Supplied implementation does not provide a parameterless constructor."));
		}
	}

	@Test
	public void defaultClassNotAbstract() {
		try {
			factory.setDefaultClass(List.class, AbstractNoPubList.class);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Implementation can't be abstract"));
		}
	}

	@Test
	public void fromJSON() {
		CommonInterfaces.PrimitiveEntity pe = factory.readEntity(CommonInterfaces.PrimitiveEntity.class,
				"{\"string\": \"something\",\"Integer\":3}");
		assertEquals(pe.getString(), "something");
		assertEquals((int) pe.getInteger(), 3);
	}

	@Test
	public void readList() {
		List<CommonInterfaces.PrimitiveEntity> list = factory.readList(CommonInterfaces.PrimitiveEntity.class,
				"[{\"string\": \"one\",\"Integer\":1},{\"string\": \"two\",\"Integer\":2}]");
		assertEquals(list.get(0).getString(), "one");
		assertEquals((int) list.get(0).getInteger(), 1);
		assertEquals(list.get(1).getString(), "two");
		assertEquals((int) list.get(1).getInteger(), 2);
	}

	private class NoPubList extends ArrayList {
		public NoPubList(int i) {
			super(i);
		}
	}

	private abstract class AbstractNoPubList extends NoPubList {
		public AbstractNoPubList(int i) {
			super(i);
		}
	}
}
