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

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.google.common.base.Charsets;

/**
 * Converts an JSON HTTPMessage to and from Entity. To enable this converter simply declare a instance within your
 * context you want it to be used. Annotation based: <br />
 *
 * <pre>
 * &#064;Bean
 * &#064;Autowired
 * public EntityConverter entityConverter(EntityFactory factory) {
 * 	return new EntityConverter(factory);
 * }
 * </pre>
 *
 * @author philnate
 */
public class EntityConverter extends AbstractHttpMessageConverter<Entity> {

	private final EntityFactory factory;

	public EntityConverter(EntityFactory factory) {
		super(MediaType.APPLICATION_JSON);
		this.factory = factory;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Entity.class.isAssignableFrom(clazz);
	}

	@Override
	protected Entity readInternal(Class<? extends Entity> clazz, HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		return factory.fromJson(clazz, IOUtils.toString(inputMessage.getBody(), Charsets.UTF_8));
	}

	@Override
	protected void writeInternal(Entity o, HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		try (OutputStreamWriter osw = new OutputStreamWriter(outputMessage.getBody())) {
			osw.write(o.toString());
		}
	}
}
