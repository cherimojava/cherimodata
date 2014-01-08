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
package com.github.cherimojava.data.spring;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.CollectibleCodec;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class _EntityConverter {
	// TODO test suite is missing
	@Mock
	EntityFactory mock;

	@Before
	public void setupMock() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void supportsTestClass() {
		EntityConverter conv = new EntityConverter(mock);
		assertTrue(conv.supports(Entity.class));
		assertTrue(conv.supports(Level1.class));
		assertTrue(conv.supports(Level2.class));
	}

	@Test
	public void supportsTestProxy() {
		EntityConverter conv = new EntityConverter(mock);
		assertTrue(conv.supports(EntityFactory.instantiate(Entity.class).getClass()));
		assertTrue(conv.supports(EntityFactory.instantiate(Level1.class).getClass()));
		assertTrue(conv.supports(EntityFactory.instantiate(Level2.class).getClass()));
	}

	@Test
	public void writeInternal() throws IOException {
		HttpOutputMessage hom = mock(HttpOutputMessage.class);
		OutputStream os = new ByteArrayOutputStream();
		when(hom.getBody()).thenReturn(os);
		EntityConverter conv = new EntityConverter(mock);
		SimpleEntity se = EntityFactory.instantiate(SimpleEntity.class);
		se.setString("SomeString");
		conv.writeInternal(se, hom);
		assertEquals("{ \"string\" : \"SomeString\" }", os.toString());
	}

	@Test
	public void readInternal() throws IOException {
		HttpInputMessage him = mock(HttpInputMessage.class);
		MongoDatabase db = mock(MongoDatabase.class);
		when(db.getCollection(anyString(), any(CollectibleCodec.class))).thenReturn(mock(MongoCollection.class));
		EntityFactory factory = new EntityFactory(db);
		InputStream is = new ByteArrayInputStream("{ \"string\" : \"SomeString\" }".getBytes());
		when(him.getBody()).thenReturn(is);
		EntityConverter conv = new EntityConverter(factory);
		SimpleEntity se = (SimpleEntity) conv.readInternal(SimpleEntity.class, him);
		assertEquals("SomeString", se.getString());
	}

	@Test
	public void integration() throws Exception {
		MongoDatabase db = mock(MongoDatabase.class);
		when(db.getCollection(anyString(), any(CollectibleCodec.class))).thenReturn(mock(MongoCollection.class));
		EntityFactory factory = new EntityFactory(db);
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new EntityController()).setMessageConverters(
				new EntityConverter(factory)).build();
		assertEquals(
				"{ \"string\" : \"PONG\" }",
				mvc.perform(
						post("/t").contentType(MediaType.APPLICATION_JSON).content("{\"string\":\"ping\"}").accept(
								MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
	}

	private static interface Level1 extends Entity {

	}

	private static interface Level2 extends Level1 {
	}

	private static interface SimpleEntity extends Entity {
		public String getString();

		public SimpleEntity setString(String s);
	}

	@Controller
	private class EntityController {

		@RequestMapping(value = "/t", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
		public @ResponseBody
		SimpleEntity post(@RequestBody SimpleEntity setting) {
			assertEquals("ping", setting.getString());
			return EntityFactory.instantiate(SimpleEntity.class).setString("PONG");
		}
	}
}