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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.joda.time.DateTime;
import org.junit.Test;

import com.github.cherimojava.data.mongo.TestBase;

public class _DateTimeCodec extends TestBase {

	@Test
	public void dateTimeDeEncoding() throws IOException {
		DateTimeCodec codec = new DateTimeCodec();
		DateTime now = DateTime.now();

		try (StringWriter swriter = new StringWriter(); JsonWriter writer = new JsonWriter(swriter)) {
			codec.encode(writer, now, null);
			JsonReader reader = new JsonReader(swriter.toString());
			assertTrue(now.equals(codec.decode(reader, null)));
		}
	}
}
