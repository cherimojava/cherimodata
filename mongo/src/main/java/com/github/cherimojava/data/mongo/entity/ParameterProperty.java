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

import static com.github.cherimojava.data.mongo.entity.EntityUtils.getAdderFromGetter;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.getSetterFromGetter;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.isAssignableFromClass;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.annotation.Final;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;
import com.github.cherimojava.data.mongo.entity.annotation.Transient;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;

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
	private final ReferenceLoadingTime referenceLoadingTime;
	private final ReferenceType referenceType;
	private final Map<MethodType, Boolean> typeReturnMap;
	private final boolean finl;

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
		finl = builder.finl;
		computer = builder.computer;
		referenceLoadingTime = builder.referenceLoadingTime;
		referenceType = builder.referenceType;
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
	 * returns if this property is final or not. Meaning that after the value was peristed once it's not meant to be
	 * changed again
	 * 
	 * @return
	 */
	public boolean isFinal() {
		return finl;
	}

	/**
	 * Returns if this property is only referenced and the actual property is stored in a separate collection. Only
	 * valid if the property is a entity subtype
	 *
	 * @return if this property is a reference to a different entity
	 */
	public boolean isReference() {
		return ReferenceType.NONE != referenceType;
	}

	/**
	 * Returns if this property is supposed to be stored as Mongo style DBRef. Is true only when the property is a
	 * reference and value DBRef is true
	 * 
	 * @return
	 */
	public boolean isDBRef() {
		return ReferenceType.DBREF == referenceType;
	}

	/**
	 * Returns if the given property is representing an Entity reference, which is lazily loaded
	 *
	 * @return if this property is loaded on first access to it
	 */
	public boolean isLazyLoaded() {
		return ReferenceLoadingTime.LAZY == referenceLoadingTime;
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
		if (value != null) {
			if (!type.isAssignableFrom(value.getClass())) {
				throw new ClassCastException(format("Can't cast from '%s' to '%s'",
						value.getClass().getCanonicalName(), type.getCanonicalName()));
			}
		}
		if (hasConstraints()) {
			Set<? extends ConstraintViolation<? extends Entity>> violations = validator.validateValue(declaringClass,
					pojoName, value, Entity.Special.class);
			if (!violations.isEmpty()) {
				StringBuilder msg = new StringBuilder().append("Found errors while validating property ").append(
						declaringClass.getCanonicalName()).append("#").append(pojoName).append(":\n");
				for (ConstraintViolation violation : violations) {
					msg.append("*").append(violation.getMessage()).append("\n");
				}
				throw new ConstraintViolationException(msg.toString(), violations);
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
		private boolean finl;
		private Computer computer;
		private ReferenceLoadingTime referenceLoadingTime;
		private ReferenceType referenceType = ReferenceType.NONE;
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

		Builder setReferenceLoadingTime(ReferenceLoadingTime reference) {
			this.referenceLoadingTime = reference;
			return this;
		}

		Builder setReferenceType(ReferenceType type) {
			this.referenceType = type;
			return this;
		}

		Builder setFinal(boolean finl) {
			this.finl = finl;
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
			Class<?> returnType = m.getReturnType();
			Builder builder = new Builder();
			if (c != null) {
				try {
					computer = c.value().newInstance();
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			} else {
				// only if this is not a computed property we have a setter for it
				builder.setFluent(MethodType.SETTER, isAssignableFromClass(getSetterFromGetter(m)));
			}
			if (Collection.class.isAssignableFrom(m.getReturnType())) {
				try {
					// only if we have an adder enabled Property type check for this
					builder.setFluent(MethodType.ADDER, isAssignableFromClass(getAdderFromGetter(m)));
				} catch (IllegalArgumentException e) {
					LOG.info("No Adder method declared for {} in Entity {}", m.getName(),
							m.getDeclaringClass().getName());
				}
			}
			// check if something marks this property as final (no modification after initial save
			boolean finl = m.isAnnotationPresent(Final.class);
			if (!finl) {
				for (Annotation a : Lists.newArrayList(m.getAnnotations())) {
					if (a.annotationType().isAnnotationPresent(Final.class)) {
						finl = true;
						break;
					}
				}
			}
			if (finl) {
				// check that final is only on primitives
				checkArgument(ClassUtils.isPrimitiveOrWrapper(returnType) || String.class.equals(returnType)
						|| ObjectId.class.equals(returnType) || DateTime.class.equals(returnType),
						"Final is only supported for primitive types, jodatime DateTime and bson ObjectId but was %s",
						returnType);
			}

			String mongoName = EntityUtils.getMongoNameFromMethod(m);
			checkArgument(!mongoName.startsWith("_") || Entity.ID.equals(mongoName),
					"Property can't start with '_' as this is reserved, but got '%s'", mongoName);

			builder.setType(returnType.isPrimitive() ? Primitives.wrap(returnType) : returnType).setPojoName(
					EntityUtils.getPojoNameFromMethod(m)).setMongoName(mongoName).hasConstraints(
					bdesc.getConstraintsForProperty(EntityUtils.getPojoNameFromMethod(m)) != null).setValidator(
					validator).setDeclaringClass(declaringClass).setTransient(m.isAnnotationPresent(Transient.class)).setComputer(
					computer).setFinal(finl);
			if (Collection.class.isAssignableFrom(m.getReturnType())) {
				checkArgument(m.getGenericReturnType().getClass() != Class.class, "Collections need to be generic");
				Type type = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0];
				if (TypeVariable.class.isAssignableFrom(type.getClass())) {
					// right now I dont know how to provide actual type at runtime...
					builder.setGenericType(Entity.class);
				} else {
					builder.setGenericType((Class) type);
				}
			}
			if (m.isAnnotationPresent(Reference.class)) {
				checkArgument(EntityUtils.isValidReferenceClass(m),
						"Reference annotation can only be used for Entity types but was {}", m.getReturnType());
				builder.setReferenceLoadingTime(m.getAnnotation(Reference.class).lazy() ? ReferenceLoadingTime.LAZY
						: ReferenceLoadingTime.IMMEDIATE);
				builder.setReferenceType(m.getAnnotation(Reference.class).asDBRef() ? ReferenceType.DBREF
						: ReferenceType.SIMPLE);
			} else {
				builder.setReferenceType(ReferenceType.NONE);
			}
			return builder.build();
		}
	}

	/**
	 * Enumeration about Reference LoadingTimes. This is information is needed for Referenced Entities loading
	 */
	private static enum ReferenceLoadingTime {
		IMMEDIATE, // value is reference but immediately loaded
		LAZY// value is lazy loaded reference
	}

	/**
	 * type of references the framework can handle
	 */
	public static enum ReferenceType {
		NONE, // no reference
		SIMPLE, // only the id is stored
		DBREF, // Mongo DBRef style
	}

	/**
	 * Information about the method being described
	 */
	static enum MethodType {
		ADDER,
		SETTER
	}
}
