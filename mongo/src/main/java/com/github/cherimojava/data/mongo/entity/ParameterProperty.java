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

import static com.github.cherimojava.data.mongo.entity.EntityUtils.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;
import com.github.cherimojava.data.mongo.entity.annotation.Transient;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * Contains information for a parameter like it's mongodb name, if it's transient, etc. This class is immutable, use
 * {@link ParameterProperty.Builder} to create a new Instance of ParameterProperty
 *
 * @author pknobel
 * @since 1.0.0
 */
public final class ParameterProperty {
	private final String mongoName;
	private final String pojoName;
	private final boolean constraints;
	private final Class<?> type;
	private final Class<?> genericType;
	private final Validator validator;
	private final Class<? extends Entity> declaringClass;
	private final boolean tranzient;
	private final Computer computer;
	private final ReferenceType reference;
	private final Map<MethodType, Boolean> typeReturnMap;

	ParameterProperty(Builder builder) {
		checkNotNull(builder.type, "type cannot be null");
		checkNotNull(builder.validator);
		checkArgument(StringUtils.isNotEmpty(builder.pojoName), "pojo name cannot be null or empty string");
		checkArgument(StringUtils.isNotEmpty(builder.mongoName), "mongo name cannot be null or empty string");

		typeReturnMap = Collections.unmodifiableMap(builder.typeReturnMap);
		type = builder.type;
		genericType = builder.genericType;
		pojoName = builder.pojoName;
		mongoName = builder.mongoName;
		constraints = builder.constraints;
		validator = builder.validator;
		declaringClass = builder.declaringClass;
		tranzient = builder.tranzient;
		computer = builder.computer;
		reference = builder.reference;
	}

	/**
	 * gets the name of this parameter which is used on mongodb side
	 */
	public String getMongoName() {
		return mongoName;
	}

	/**
	 * gets the name of this parameter within the pojo, returned name doesn't contain set/get/add or any other prefix
	 */
	public String getPojoName() {
		return pojoName;
	}

	/**
	 * returns if this method allows for fluent API access or not. Returns null if the specified method isn't existent
	 * for this property
	 *
	 * @param type
	 *            add or get method to check
	 */
	public Boolean isFluent(MethodType type) {
		return typeReturnMap.get(type);
	}

	/**
	 * returns the type of the property this instance represents
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * returns if this ParameterProperty has Constraints, which need to be validated on set/save
	 */
	public boolean hasConstraints() {
		return constraints;
	}

	/**
	 * returns if this property isn't intended to be stored to MongoDB
	 *
	 * @return if this property is transient or not
	 */
	public boolean isTransient() {
		return tranzient;
	}

	/**
	 * returns if this property is computed by a computer
	 *
	 * @return if this property is computed
	 */
	public boolean isComputed() {
		return computer != null;
	}

	/**
	 * Returns if this property is only referenced and the actual property is stored in a separate collection. Only
	 * valid if the property is a entity subtype
	 *
	 * @return if this property is a reference to a different entity
	 */
	public boolean isReference() {
		return ReferenceType.NONE != reference;
	}

	/**
	 * Returns if the given property is representing an Entity reference, which is lazily loaded
	 *
	 * @return if this property is loaded on first access to it
	 */
	public boolean isLazyLoaded() {
		return ReferenceType.LAZY == reference;
	}

	/**
	 * gets the computer for this property
	 *
	 * @return computer of this property if the property is computer, null otherwise
	 */
	public Computer getComputer() {
		return computer;
	}

	/**
	 * returns if the property is a collection, meaning the return type needs to be enriched with the actual class the
	 * return type contains.
	 *
	 * @return true if the type is a collection, false otherwise
	 */
	public boolean isCollection() {
		return genericType != null;
	}

	/**
	 * returns the contained type if {@link #isCollection()} is returning true. Otherwise null is returned
	 *
	 * @return
	 */
	public Class<?> getGenericType() {
		return genericType;
	}

	/**
	 * validates the given value if it matches the defined constraints for this property. Throws
	 * ConstraintViolationException if the value doesn't comply with the declared Constraints
	 *
	 * @param value
	 *            property value to check for validity
	 */
	public void validate(Object value) {
		if (hasConstraints()) {
			Set violations = validator.validateValue(declaringClass, pojoName, value, Entity.Special.class);
			if (!violations.isEmpty()) {
				throw new ConstraintViolationException(violations);
			}
		}
	}

	/**
	 * Builder to create a new {@link ParameterProperty}
	 *
	 * @author philnate
	 */
	static class Builder {
		private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

		private String mongoName;
		private String pojoName;
		private Class<?> type;
		private Class<?> genericType;
		private boolean constraints = false;
		private Validator validator;
		private Class<? extends Entity> declaringClass;
		private boolean tranzient;
		private Computer computer;
		private ReferenceType reference;
		private Map<MethodType, Boolean> typeReturnMap = Maps.newHashMap();

		Builder setTransient(boolean tranzient) {
			this.tranzient = tranzient;
			return this;
		}

		Builder setMongoName(String mongoName) {
			this.mongoName = mongoName;
			return this;
		}

		Builder setPojoName(String pojoName) {
			this.pojoName = pojoName;
			return this;
		}

		Builder setFluent(MethodType method, boolean fluent) {
			typeReturnMap.put(method, fluent);
			return this;
		}

		Builder setType(Class<?> type) {
			this.type = type;
			return this;
		}

		Builder setGenericType(Class<?> genericType) {
			this.genericType = genericType;
			return this;
		}

		Builder hasConstraints(boolean constraints) {
			this.constraints = constraints;
			return this;
		}

		Builder setValidator(Validator validator) {
			this.validator = validator;
			return this;
		}

		Builder setDeclaringClass(Class<? extends Entity> declaringClass) {
			this.declaringClass = declaringClass;
			return this;
		}

		Builder setComputer(Computer computer) {
			this.computer = computer;
			return this;
		}

		Builder setReferenceType(ReferenceType reference) {
			this.reference = reference;
			return this;
		}

		ParameterProperty build() {
			return new ParameterProperty(this);
		}

		/**
		 * creates a new {@link ParameterProperty} based on the attributes from the given get Method
		 *
		 * @param m
		 *            to create ParameterProperty from
		 * @return ParameterProperty containing the information from the given method
		 */
		@SuppressWarnings("unchecked")
		static ParameterProperty buildFrom(Method m, Validator validator) {
			Class<? extends Entity> declaringClass = (Class<? extends Entity>) m.getDeclaringClass();
			BeanDescriptor bdesc = validator.getConstraintsForClass(declaringClass);
			Computer computer = null;
			Computed c = m.getAnnotation(Computed.class);
			Builder builder = new Builder();
			if (c != null) {
				try {
					computer = c.value().newInstance();
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			} else {
				// only if this is not a computed property we have a setter for it
				// TODO right now we're failing if there's no setter, but would it make sense to allow that there's no
				// setter defined?
				builder.setFluent(MethodType.SETTER, isAssignableFromClass(getSetterFromGetter(m)));
			}
			if (Collection.class.isAssignableFrom(m.getReturnType())) {
				try {
					// only if we have an adder enabled Property type check for this
					builder.setFluent(MethodType.ADDER, isAssignableFromClass(getAdderFromGetter(m)));
				} catch (IllegalArgumentException e) {
					LOG.debug("No Adder method declared for {} in Entity {}", m.getName(),
							m.getDeclaringClass().getName());
				}
			}
			builder.setType(m.getReturnType()).setPojoName(EntityUtils.getPojoNameFromMethod(m)).setMongoName(
					EntityUtils.getMongoNameFromMethod(m)).hasConstraints(
					bdesc.getConstraintsForProperty(EntityUtils.getPojoNameFromMethod(m)) != null).setValidator(
					validator).setDeclaringClass(declaringClass).setTransient(m.isAnnotationPresent(Transient.class)).setComputer(
					computer);
			if (Collection.class.isAssignableFrom(m.getReturnType())) {
				checkArgument(m.getGenericReturnType().getClass() != Class.class, "Collections need to be generic");
				Type type = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0];
				if (TypeVariable.class.isAssignableFrom(type.getClass())) {
                    //right now I dont know how to provide actual type at runtime...
					builder.setGenericType(Entity.class);
				} else {
					builder.setGenericType((Class) type);
				}
			}
			if (m.isAnnotationPresent(Reference.class)) {
				checkArgument(Entity.class.isAssignableFrom(m.getReturnType()),
						"Reference annotation can only be used for Entity types but was {}", m.getReturnType());
				if (m.getAnnotation(Reference.class).lazy()) {
					builder.setReferenceType(ReferenceType.LAZY);
				} else {
					builder.setReferenceType(ReferenceType.IMMEDIATE);
				}
			} else {
				builder.setReferenceType(ReferenceType.NONE);
			}
			return builder.build();
		}
	}

	/**
	 * Enumeration about Reference Types. This is information is needed for Referenced Entities
	 */
	private static enum ReferenceType {
		NONE, // value is no reference
		IMMEDIATE, // value is reference but immediately loaded
		LAZY// value is lazy loaded reference
	}

	/**
	 * Information about the method being described
	 */
	static enum MethodType {
		ADDER,
		SETTER
	}
}
