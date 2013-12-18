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
package org.mongodb.codecs;

import com.google.common.collect.Lists;
import org.bson.BSONReader;
import org.bson.BSONWriter;
import org.junit.Test;
import org.mongodb.Codec;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestCase {
	/**
	 * currently the new driver isn't capable of handling correctly primitive types
	 */
	@Test
	public void encodingTest() {
		PrimitiveCodecs codec = PrimitiveCodecs.createDefault();
		assertFalse(codec.canEncode(int.class));// currently false but should be true
		assertFalse(codec.canEncode(Integer.TYPE));

		// Integer.TYPE basically needs to be added to the defaultEncodings
		codec = PrimitiveCodecs.builder().integerCodec(new IntCodec()).build();
		assertTrue(codec.canEncode(int.class));
		assertTrue(codec.canEncode(Integer.TYPE));
	}

	@Test
	public void arrayEncodingTest() {
        Codecs codec = Codecs.createDefault();
		Class string = new String[] {}.getClass();
		assertTrue(string.isArray());
		assertFalse(codec.canDecode(string.getClass())); // should be true

        assertTrue(codec.canDecode(Lists.newArrayList().getClass()));
	}

    @Test
    public void testIterable() {
        List<String> list = Lists.newArrayList();
        assertTrue(Iterable.class.isInstance(list));
        Codecs codec = Codecs.createDefault();

        assertTrue(Iterable.class.isAssignableFrom(list.getClass()));
        assertFalse(Iterable.class.isInstance(list.getClass()));

        assertFalse(Iterable.class.isAssignableFrom(String.class));
        assertTrue(codec.canDecode(list.getClass()));   //should be true
    }

	public class IntCodec implements Codec<Integer> {
		@Override
		public void encode(final BSONWriter bsonWriter, final Integer value) {
			bsonWriter.writeInt32(value);
		}

		@Override
		public Integer decode(final BSONReader reader) {
			return reader.readInt32();
		}

        @Override
		public Class<Integer> getEncoderClass() {
			return int.class;
		}
	}
}
