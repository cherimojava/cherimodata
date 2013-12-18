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
package me.philnate.cherimodata.mongo.entities;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.validation.Validation;
import javax.validation.Validator;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;

import me.philnate.cherimodata.mongo.Factory;

import static com.google.common.base.Preconditions.checkArgument;
import static me.philnate.cherimodata.mongo.entities.EntityProperties.Builder;
import static me.philnate.cherimodata.mongo.entities.EntityUtils.getCollectionName;

class EntityPropertyFactory implements Factory<Class<? extends Entity>, EntityProperties> {
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
	 * @return
	 */
	@Override
	public EntityProperties create(Class<? extends Entity> clazz) {
		try {
			return classes.get(clazz);
		} catch (UncheckedExecutionException | ExecutionException e) {
			// unwrap causing exception and rethrow
			throw Throwables.propagate(e.getCause());
		}
	}

	private EntityProperties build(Class<? extends Entity> clazz) {
		Builder builder = new Builder().setEntityClass(clazz).setValidator(validator);

		builder.setCollectionName(getCollectionName(clazz));

		Set<String> setter = Sets.newHashSet();
		List<String> getter = Lists.newArrayList();
		Set<String> foundAllowedMethods = Sets.newHashSet();
		// iterate through all methods and create parameter properties for them
		for (Method m : clazz.getMethods()) {
			if (allowedMethods.contains(m.getName())) {
				// method not conforming to Entity convention, but coming from Object and Entity
				// if we catch a second one we have to fail as custom allowedMethods aren't allowed
				checkArgument(foundAllowedMethods.add(m.getName()),
						"Don't write custom equals, toString, etc. methods, found custom %s", m.getName());
				continue;
			}
			if (m.getName().startsWith("set")) {
				// remember the setter we check it later
				// avoid that we have multiple setter (at least one invalid) for a property won't catch this otherwise
				checkArgument(setter.add(m.getName().replaceFirst("s", "g")), "Multiple setter found for %s",
						m.getName());
				continue;
			}
			checkArgument(m.getName().startsWith("get"),
					"Found method %s, which isn't conform with Entity method convention", m.getName());
			getter.add(m.getName());// remember getter so we can compare it later
			ParameterProperty pp = ParameterProperty.Builder.buildFrom(m, validator);
			builder.addParameter(pp);
		}
		setter.removeAll(getter); // remove all setter which have matching getter methods
		checkArgument(setter.isEmpty(), "Found setter methods which have no matching getter %s", setter);

		return builder.build();
	}
}
