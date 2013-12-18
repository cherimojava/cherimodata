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
import java.util.Locale;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import me.philnate.cherimodata.mongo.entities.annotations.Id;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * Utility Class holding commonly used functionality to work with Entities
 *
 * @author philnate
 */
public class EntityUtils {
	/**
	 * decapitalizes a String. E.g. CamelCase will become camelCase while URL will stay URL, but URLe becomes uRLe.
	 *
	 * @param name
	 *            string to decapitalize. For invertion see capitalize(String)
	 * @return decapitalized string
	 */
	public static String decapitalize(String name) {
		if (name.length() == 1) {
			return name.toLowerCase(Locale.ENGLISH);
		}
		if (!isAllUpperCase(name)) {
			return uncapitalize(name);
		} else {
			return name;
		}
	}

	/**
	 * Capitalizes a String. I.e. the first Letter will be converted into uppercase, all other letters will stay as is.
	 * For invertion see decapitalize(String)
	 *
	 * @param name
	 *            string to capitalize.
	 * @return capitalized string
	 */
	public static String capitalize(String name) {
		return StringUtils.capitalize(name);
	}

	/**
	 * Retrieves the pojo name from the given method if this is a valid set/Get method
	 *
	 * @param m
	 *            method to retrieve name from
	 * @return propertyname derived from the given method
	 */
	// TODO this might be better placed within ParameterProperty
	public static String getPojoNameFromMethod(Method m) {
		checkArgument((m.getName().startsWith("set") || m.getName().startsWith("get")) && m.getName().length() > 3,
				"Don't know how to retrieve name from this method, got [%s]", m.getName());
		return decapitalize(m.getName().substring(3));
	}

	/**
	 * retrieves the name of the property which is used on mongodb side. Accepts only get methods, will throw an
	 * exception for all other methods
	 *
	 * @param m
	 *            method to retrieve mongo name from
	 * @return mongo name for the given getter method (parameter)
	 */
	public static String getMongoNameFromMethod(Method m) {
		checkArgument(m.getName().startsWith("get"), "Mongo name can only be retrieved from get methods, but was %s",
				m.getName());
		checkArgument(!(m.isAnnotationPresent(Named.class) && m.isAnnotationPresent(Id.class)),
				"You can not annotate a property with @Name and @Id");
		if (m.isAnnotationPresent(Named.class)) {
			checkArgument(!Entity.ID.equals(m.getAnnotation(Named.class).value()),
					"It's not allowed to use @Name annotation to declare id field, instead use @Id annotation");
			return m.getAnnotation(Named.class).value();
		}
		if (m.isAnnotationPresent(Id.class) || "id".equals(getPojoNameFromMethod(m).toLowerCase(Locale.US))) {
			return Entity.ID;
		}
		return decapitalize(m.getName().substring(3));
	}

	/**
	 * retrieves the name for the Entity, which might be different from the clazz name if the @Named annotation is
	 * present
	 *
	 * @param clazz
	 *            Entity from which to retrieve the collection name
	 * @return the name of the Entity as declared with the Named annotation or the clazz name if annotation isn't
	 *         present
	 */
	public static String getCollectionName(Class<? extends Entity> clazz) {
		Named name = clazz.getAnnotation(Named.class);
		if (name != null && isNotEmpty(name.value())) {
			return name.value();
		}
		return uncapitalize(clazz.getSimpleName());
	}
}
