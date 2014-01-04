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
package org.mongodb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.UnknownHostException;

import org.bson.BSONReader;
import org.bson.BSONWriter;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.connection.ServerAddress;

import com.github.cherimojava.data.mongo.MongoBase;
import com.github.cherimojava.data.mongo.entity.Entity;

/**
 * Created with IntelliJ IDEA. User: user Date: 9/9/13 Time: 3:50 PM To change this template use File | Settings | File
 * Templates.
 */
public class _DriverTest extends MongoBase {

	@Test
	@Ignore
	public void simpleTest() throws UnknownHostException {
		MongoClient client = MongoClients.create(new ServerAddress("localhost", 27017));
		MongoCollection<Entity> collection = client.getDatabase("test").getCollection("entity",
				new EntityCodec(Entity.class));
		Entity e = (Entity) Proxy.newProxyInstance(EntityInvocationHandler.class.getClassLoader(),
				new Class<?>[] { Entity.class }, new EntityInvocationHandler());
		collection.save(e);
	}

	public static class EntityCodec<T extends Entity> implements CollectibleCodec<T> {
		private Class<T> clazz;
		private EntityEncoder enc = new EntityEncoder();
		private EntityDecoder dec = new EntityDecoder();

		public EntityCodec(Class<T> clazz) {
			this.clazz = clazz;
		}

		@Override
		public Object getId(T document) {
			return "1";
		}

		@Override
		public <E> T decode(BSONReader reader) {
			return null; // To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void encode(BSONWriter bsonWriter, T value) {
			enc.encode(bsonWriter, value);
			// To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Class<T> getEncoderClass() {
			return null; // To change body of implemented methods use File | Settings | File Templates.
		}

		private class EntityEncoder<T extends Entity> implements Encoder<T> {
			@Override
			public void encode(BSONWriter bsonWriter, T value) {
				bsonWriter.writeStartDocument();
				bsonWriter.writeString("_id", "1");
				bsonWriter.writeEndDocument();
				// To change body of implemented methods use File | Settings | File Templates.
			}

			@Override
			public Class<T> getEncoderClass() {
				return null; // To change body of implemented methods use File | Settings | File Templates.
			}
		}

		private class EntityDecoder<T extends Entity> implements Decoder<T> {
			@Override
			public <E> T decode(BSONReader reader) {
				System.out.println("decoding");
				return null; // To change body of implemented methods use File | Settings | File Templates.
			}
		}

	}

	public static class EntityInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return null; // To change body of implemented methods use File | Settings | File Templates.
		}
	}
}
