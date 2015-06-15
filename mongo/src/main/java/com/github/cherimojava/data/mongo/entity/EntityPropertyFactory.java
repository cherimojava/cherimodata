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

import static com.github.cherimojava.data.mongo.entity.EntityUtils.getCollectionName;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.getGetterFromAdder;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.getGetterFromSetter;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.getPojoNameFromMethod;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.isAssignableFromClass;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.validation.Validation;
import javax.validation.Validator;

import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * Factory for creating EntityProperties out of a given Interface extending Entity
 *
 * @author philnate
 * @since 1.0.0
 */
class EntityPropertyFactory {
	/**
	 * list of methods allowed although not conforming to Entity convention
	 */
	private static List<String> allowedMethods = ImmutableList.copyOf(Lists.newArrayList("drop", "get", "set",
			"equals", "hashCode", "toString", "save", "seal", "load", "entityClass"));
	/**
	 * builds a validation factory used for validating Entities
	 */
	private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	/**
	 * contains all loaded EntityProperties so far and loads EntityProperties if needed
	 */
	private final LoadingCache<Class<? extends Entity>, EntityProperties> classes = CacheBuilder.newBuilder().build(
			new CacheLoader<Class<? extends Entity>, EntityProperties>() {
				@Override
				public EntityProperties load(Class<? extends Entity> clazz) throws Exception {
					return build(clazz);
				}
			});

	/**
	 * retrieves the entity properties for the given entity class. Multiple invocations will return the same
	 * EntityProperty instance
	 *
	 * @param clazz
	 *            Entity class from which the Entity properties shall be created
	 * @return EntityProperties belonging to the given Entity class
	 */
	public EntityProperties create(Class<? extends Entity> clazz) {
		try {
			return classes.get(clazz);
		} catch (UncheckedExecutionException | ExecutionException e) {
			// unwrap causing exception and rethrow
			throw Throwables.propagate(e.getCause());
		}
	}

	/**
	 * builds entityProperties instance for the given Entity class and verifies that the entity class is valid
	 *
	 * @param clazz
	 *            Entity class to build EntityProperties for
	 * @return entityProperties belonging to the given Entity class
	 */
	private EntityProperties build(Class<? extends Entity> clazz) {
		EntityProperties.Builder builder = new EntityProperties.Builder().setEntityClass(clazz).setValidator(validator);

		builder.setCollectionName(getCollectionName(clazz));

		// iterate through all methods and create parameter properties for them
		for (Method m : clazz.getMethods()) {
			if (allowedMethods.contains(m.getName())) {
				// method is one of the allowed ones, check that no custom implementation is declared (with different
				// params, e.g.)
				checkArgument(m.getDeclaringClass().equals(Entity.class),
						"Don't write custom equals, toString etc. methods. Found custom %s", m.getName());
			} else if (m.getName().startsWith("set")) {
				validateSetter(m);
			} else if (m.getName().startsWith("get")) {
				validateGetter(m);
				builder.addParameter(m);
			} else if (m.getName().startsWith("add")) {
				validateAdder(m);
			} else {
				throw new IllegalArgumentException(format(
						"Found method %s, which isn't conform with Entity method convention", m.getName()));
			}
		}
		return builder.build();
	}

	/*
	 * it's the validate* method responsibility to verify all it's constraints. E.g. it's the validateSetter methods
	 * duty to verify that a Set method is legit, meaning if a property is computed it's forbidden to have a setter for
	 * it
	 */

	/**
	 * validates that a method matches the adder constraints. The constraints are:
	 * <ul>
	 * <li>Adder can be only declared for Properties of type Collection
	 * <li>Adder has only parameter
	 * <li>Parameter type matches the type of the Collection
	 * </ul>
	 *
	 * @param adder
	 */
	void validateAdder(Method adder) {
		checkArgument(adder.getParameterTypes().length == 1,
				"Adder method must define exactly one parameter matching the generic type of the property.");
		Method getter = getGetterFromAdder(adder);
		checkArgument(Collection.class.isAssignableFrom(getter.getReturnType()),
				"Adder method only allowed for properties extending collection, but was %s.", getter.getReturnType());
		Type getterType = ((ParameterizedType) getter.getGenericReturnType()).getActualTypeArguments()[0];
		Type adderType = adder.getParameterTypes()[0];
		checkArgument(getterType.equals(adderType),
				"Collection has a generic type of %s, but adder has parameter of type %s", getterType, adderType);
	}

	/**
	 * validates that a method matches the setter constraints. The constraints are:
	 * <ul>
	 * <li>return type must be void or Supertype of declaring class
	 * <li>Exactly one parameter
	 * <li>Must have a matching getter (setter parameter matches getter return type)
	 * <li>There must be no setter method if the property is computed
	 * </ul>
	 *
	 * @param setter
	 */
	void validateSetter(Method setter) {
		if (setter.getReturnType() != Void.TYPE) {
			checkArgument(isAssignableFromClass(setter),
					"Return type %s of setter %s isn't supported. Only Superclasses of %s are valid",
					setter.getReturnType(), setter.getName(), setter.getDeclaringClass());
		}
		checkArgument(setter.getParameterTypes().length == 1, "Set methods must have one parameter, but had %s",
				Lists.newArrayList(setter.getParameterTypes()));
		Method getter;
		try {
			getter = getGetterFromSetter(setter);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(format(
					"You can only declare setter methods if there's a matching getter. Found %s without getter",
					setter.getName()));
		}
		checkArgument(!getter.isAnnotationPresent(Computed.class),
				"computed property %s cannot have a setter method declared", getPojoNameFromMethod(getter));
	}

	/**
	 * validates that a method matches the getter constraints. These constraints are:
	 * <ul>
	 * <li>Must have return type != Void
	 * <li>@Reference can only be placed on properties being type entity
	 * <li>Can't have parameters
	 * </ul>
	 *
	 * @param getter
	 */
	void validateGetter(Method getter) {
		checkArgument(getter.getReturnType() != Void.TYPE, "Method %s did not declare a return type", getter.getName());
		checkArgument(getter.getParameterTypes().length == 0, "Get methods can't have parameters, but had %s",
				Lists.newArrayList(getter.getParameterTypes()));
		if (getter.isAnnotationPresent(Reference.class)) {
			if (!EntityUtils.isValidReferenceClass(getter)) {
				throw new IllegalArgumentException("Cant declare reference on non entity type or list of entities");
			}
		}
	}
}
