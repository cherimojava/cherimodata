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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.google.common.base.Charsets;

/**
 * Converts a JSON HTTPMessage to and from Entity. To enable this converter you need to add it to your
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter}. This can happen through
 * overriding the appropriate {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport}
 * methods like
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#configureMessageConverters} or
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addDefaultHttpMessageConverters}
 *
 * @author philnate
 * @since 1.0.0
 */
public class EntityConverter extends AbstractHttpMessageConverter<Object> implements
		GenericHttpMessageConverter<Object> {

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
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		return fromJson((Class<? extends Entity>) clazz, inputMessage);
	}

	@Override
	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		// as this method is only called after we decided that we can decode the requested type, we only need to check
		// what we have (plain entity/list of entities)
		if (type instanceof Class) {
			// simple class
			return factory.readEntity((Class<? extends Entity>) type, IOUtils.toString(inputMessage.getBody()));
		} else {
			// collection
			return factory.readList((Class<? extends Entity>) ((ParameterizedType) type).getActualTypeArguments()[0],
					IOUtils.toString(inputMessage.getBody()));
		}
	}

	private Entity fromJson(Class<? extends Entity> clazz, HttpInputMessage inputMessage) throws IOException {
		return factory.readEntity(clazz, IOUtils.toString(inputMessage.getBody(), Charsets.UTF_8.name()));
	}

	@Override
	protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		try (OutputStreamWriter osw = new OutputStreamWriter(outputMessage.getBody())) {
			osw.write(o.toString());
		}
	}

	@Override
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
		if (MediaType.APPLICATION_JSON.equals(mediaType)) {
			if (type instanceof Class) {
				// check if this is a simple entity class
				return Entity.class.isAssignableFrom((Class) type);
			}
			if (type instanceof ParameterizedType) {
				// is this a parameterized type
				ParameterizedType pt = (ParameterizedType) type;
				if (pt.getRawType() instanceof Class && Collection.class.isAssignableFrom((Class) pt.getRawType())) {
					// is this rawtype a class and is this class some collection
					Type generic = pt.getActualTypeArguments()[0];
					if (generic instanceof Class && Entity.class.isAssignableFrom((Class) generic)) {
						// is this collection generic an entity
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		// this is rather ugly, would be great if we could get hold of the Type rather than class, so that we can check
		// for the generic type and decide upon this
		return MediaType.APPLICATION_JSON.equals(mediaType)
				&& (Entity.class.isAssignableFrom(clazz) || Collection.class.isAssignableFrom(clazz));
	}
}
