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
package com.github.cherimojava.data.mongo.entity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Validator;

import org.bson.types.ObjectId;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Contains information about a Entity class, like it's collection name, properties etc
 *
 * @author philnate
 */
public final class EntityProperties {
	/**
	 * contains the validator used for validating properties
	 */
	private final Validator validator;
	/**
	 * the class this EntityProperties reflect
	 */
	private final Class<? extends Entity> clazz;

	/**
	 * name of the collection which contains this entity
	 */
	private final String collectionName;

	/**
	 * Stores ParameterProperties linked by their pojo name
	 */
	private final Map<String, ParameterProperty> pojoNames;
	/**
	 * Stores ParameterProperties linked by their mongo name
	 */
	private final Map<String, ParameterProperty> mongoNames;

	/**
	 * list of properties containing validation annotation
	 */
	private final List<ParameterProperty> validationProperties;

	/**
	 * tells if we have an explicitly defined Id or not
	 */
	private final boolean explicitId;

	private EntityProperties(Builder builder) {
		this.clazz = builder.clazz;
		this.collectionName = builder.collectionName;
		boolean explicitId = false;

		ImmutableMap.Builder<String, ParameterProperty> pojo = new ImmutableMap.Builder<>();
		ImmutableMap.Builder<String, ParameterProperty> mongo = new ImmutableMap.Builder<>();
		ImmutableList.Builder<ParameterProperty> valProps = new ImmutableList.Builder<>();
		for (ParameterProperty pp : builder.properties) {
			pojo.put(pp.getPojoName(), pp);
			mongo.put(pp.getMongoName(), pp);
			if (Entity.ID.equals(pp.getMongoName())) {
				explicitId = true;
			}
			if (pp.hasConstraints()) {
				valProps.add(pp);
			}
		}

		// check if we have an explicit id, if we don't, create a property for it
		// TODO add to the validator or so, the capability to validate implicit id too
		if (!explicitId) {
			ParameterProperty id = new ParameterProperty.Builder().setMongoName(Entity.ID).setPojoName(Entity.ID).setType(
					ObjectId.class).setTransient(false).hasConstraints(false).setValidator(builder.validator).build();
			pojo.put(Entity.ID, id);
			mongo.put(Entity.ID, id);
		}

		this.pojoNames = pojo.build();
		this.mongoNames = mongo.build();
		this.validationProperties = valProps.build();
		this.validator = builder.validator;
		this.explicitId = explicitId;
	}

	/**
	 * retrieves the corresponding ParameterProperty from a given method or null if not found
	 *
	 * @param m
	 *            method to retrieve parameter properties from.
	 * @return ParameterProperty if found or null otherwise
	 */
	public ParameterProperty getProperty(Method m) {
		return pojoNames.get(EntityUtils.getPojoNameFromMethod(m));
	}

	/**
	 * retrieves the corresponding ParameterProperty from the given MongoName or null if no such property exists
	 *
	 * @param name
	 *            to load mongo properties from
	 * @return ParameterProperty if found or null otherwise
	 */
	public ParameterProperty getProperty(String name) {
		return mongoNames.get(name);
	}

	/**
	 * returns the entity class which this entity property represents
	 */
	public Class<? extends Entity> getEntityClass() {
		return clazz;
	}

	public List<ParameterProperty> getValidationProperties() {
		return validationProperties;
	}

	/**
	 * returns the name of the collection this Entity will be saved to
	 *
	 * @return
	 */
	public String getCollectionName() {
		return collectionName;
	}

	/**
	 * returns if for this entity an explicit id was defined or not return true if an explicit Id was defined, either
	 * through @Id or @Named("_id")
	 */
	public boolean hasExplicitId() {
		return explicitId;
	}

	static class Builder {
		private Class<? extends Entity> clazz;

		private String collectionName;

		private List<ParameterProperty> properties;

		private Set<String> mongoNames;
		private Validator validator;

		Builder() {
			properties = Lists.newArrayList();
			mongoNames = Sets.newHashSet();
		}

		Builder setCollectionName(String name) {
			this.collectionName = name;
			return this;
		}

		Builder setEntityClass(Class<? extends Entity> clazz) {
			this.clazz = clazz;
			return this;
		}

		Builder addParameter(ParameterProperty pp) {
			checkArgument(mongoNames.add(pp.getMongoName()), "Entity contains already a property whose name is %s",
					pp.getMongoName());
			properties.add(pp);
			return this;
		}

		EntityProperties build() {
			return new EntityProperties(this);
		}

		Builder setValidator(Validator validator) {
			this.validator = validator;
			return this;
		}
	}
}
