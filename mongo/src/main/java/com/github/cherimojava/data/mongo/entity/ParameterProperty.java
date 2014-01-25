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

import java.lang.reflect.Method;
import java.util.Set;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;

import org.apache.commons.lang3.StringUtils;

import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;
import com.github.cherimojava.data.mongo.entity.annotation.Transient;
import com.google.common.base.Throwables;

import static com.github.cherimojava.data.mongo.entity.EntityUtils.getSetterFromGetter;
import static com.github.cherimojava.data.mongo.entity.EntityUtils.isAssignableFromClass;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains information for a parameter like it's mongodb name, if it's transient, etc. This class is immutable, use
 * ParameterProperty.Builder to create a new Instance of ParameterProperty
 *
 * @author pknobel
 * @since 1.0.0
 */
public final class ParameterProperty {
	private final String mongoName;
	private final String pojoName;
	private final boolean fluent;
	private final boolean constraints;
	private final Class<?> type;
	private final Validator validator;
	private final Class<? extends Entity> declaringClass;
	private final boolean tranzient;
	private final Computer computer;
	private final ReferenceType reference;

	ParameterProperty(Builder builder) {
		checkNotNull(builder.type, "type cannot be null");
		checkNotNull(builder.validator);
		checkArgument(StringUtils.isNotEmpty(builder.pojoName), "pojo name cannot be null or empty string");
		checkArgument(StringUtils.isNotEmpty(builder.mongoName), "mongo name cannot be null or empty string");
		fluent = builder.fluent;
		type = builder.type;
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
	 * returns if this method allows for fluent API access or not
	 */
	public Boolean isFluent() {
		return fluent;
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
	 * Builder to create a new ParameterProperty
	 *
	 * @author philnate
	 */
	static class Builder {
		private String mongoName;
		private String pojoName;
		private boolean fluent = false;
		private Class<?> type;
		private boolean constraints = false;
		private Validator validator;
		private Class<? extends Entity> declaringClass;
		private boolean tranzient;
		private Computer computer;
		private ReferenceType reference;

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

		Builder setFluent(boolean fluent) {
			this.fluent = fluent;
			return this;
		}

		Builder setType(Class<?> type) {
			this.type = type;
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
		 * creates a new ParameterProperty based on the attributes from the given get Method
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
				builder.setFluent(isAssignableFromClass(getSetterFromGetter(m)));
			}
			builder.setType(m.getReturnType()).setPojoName(EntityUtils.getPojoNameFromMethod(m)).setMongoName(
					EntityUtils.getMongoNameFromMethod(m)).hasConstraints(
					bdesc.getConstraintsForProperty(EntityUtils.getPojoNameFromMethod(m)) != null).setValidator(
					validator).setDeclaringClass(declaringClass).setTransient(m.isAnnotationPresent(Transient.class)).setComputer(
					computer);
			if (m.isAnnotationPresent(Reference.class)) {
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

	private static enum ReferenceType {
		NONE, // value is no reference
		IMMEDIATE, // value is reference but immediately loaded
		LAZY// value is lazy loaded reference
	}
}
