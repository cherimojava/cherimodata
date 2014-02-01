/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata/spring)
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
 * Converts a JSON HTTPMessage to and from Entity. If you want to return/read a list of Entities you must wrap them due
 * to Java limitations into a custom Entity which holds a list of entities. To enable this converter you need to add it
 * to your {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter}. This can happen
 * through overriding the appropriate
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport} methods like
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#configureMessageConverters} or
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addDefaultHttpMessageConverters}
 *
 * @author philnate
 * @since 1.0.0
 */
public class EntityConverter extends AbstractHttpMessageConverter<Entity> {

	private final EntityFactory factory;

	/**
	 * creates a new EntityConverter which utilizes the given @{link
	 * com.github.cherimojava.data.mongo.entity.EntityFactory}
	 *
	 * @param factory
	 *            to be used to convert from/to HTTPMessage/Entity
	 */
	public EntityConverter(EntityFactory factory) {
		super(MediaType.APPLICATION_JSON);
		this.factory = factory;
	}

	/**
	 * Checks if the given class can be handled by this Converter or not. Returns true for all
	 * {@link com.github.cherimojava.data.mongo.entity.Entity} based classes, false otherwise
	 *
	 * @param clazz
	 *            to check if it's supported
	 * @return true if the given class can be assigned to Entity, false otherwise
	 */
	@Override
	protected boolean supports(Class<?> clazz) {
		return Entity.class.isAssignableFrom(clazz);
	}

	@Override
	protected Entity readInternal(Class<? extends Entity> clazz, HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		return factory.fromJson(clazz, IOUtils.toString(inputMessage.getBody(), Charsets.UTF_8.name()));
	}

	@Override
	protected void writeInternal(Entity o, HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		try (OutputStreamWriter osw = new OutputStreamWriter(outputMessage.getBody())) {
			osw.write(o.toString());
		}
	}
}
