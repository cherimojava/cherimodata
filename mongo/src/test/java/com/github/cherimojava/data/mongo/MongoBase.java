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
package com.github.cherimojava.data.mongo;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mongodb.MongoClient;
import org.mongodb.MongoClients;
import org.mongodb.MongoDatabase;
import org.mongodb.connection.ServerAddress;

import com.github.cherimojava.data.mongo.TestBase;
import com.google.common.collect.Lists;

/**
 * Base Class for Tests requiring MongoDB
 *
 * @author philnate
 *
 */
public class MongoBase extends TestBase {

	protected MongoDatabase db;

	protected MongoClient client;

	protected List<String> collectionCleanUp;

	@BeforeClass
	public static void prepare() {
		Suite.startMongo();
	}

	@AfterClass
	public static void cleanUp() {
		Suite.stopMongo();
	}

	@Before
	public final void dbSetup() throws IOException {
		client = MongoClients.create(new ServerAddress("localhost", Suite.getPort()));
		db = client.getDatabase(Suite.dbName);
		collectionCleanUp = Lists.newArrayList();
	}

	@After
	public final void collectionCleanup() {
		db.tools().drop();
	}
}
