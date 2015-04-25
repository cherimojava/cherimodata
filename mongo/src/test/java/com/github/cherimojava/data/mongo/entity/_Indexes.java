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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

import javax.inject.Named;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.MongoBase;
import com.github.cherimojava.data.mongo.entity.annotation.Collection;
import com.github.cherimojava.data.mongo.entity.annotation.Index;
import com.github.cherimojava.data.mongo.entity.annotation.IndexField;

public class _Indexes extends MongoBase {

	EntityFactory factory;

	@Before
	public void setup() {
		factory = new EntityFactory(db);
	}

	@Test
	public void indexCreation() {
		factory.create(IndexedEntity.class);

		int count = 0;
		for (Document document : db.getCollection(EntityUtils.getCollectionName(IndexedEntity.class)).listIndexes()) {
			count++;
			assertThat(
					document.toJson().replaceAll("\"", ""),
					anyOf(sameJSONAs("{v:1, key:{_id:1}, name:_id_, ns:_Indexes.collection}"),
							sameJSONAs("{v:1, key:{string:1}, name:single, ns:_Indexes.collection}"),
							sameJSONAs("{v:1, unique:true, key:{string:-1, anotherString:1}, name:string_-1_anotherString_1, ns:_Indexes.collection}")));
			System.out.println(document);
		}

		assertEquals(3, count);
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
