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
package com.github.cherimojava.data.mongo;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.validation.constraints.NotNull;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.annotation.Computed;
import com.github.cherimojava.data.mongo.entity.Computer;
import com.github.cherimojava.data.mongo.entity.annotation.Id;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;

/**
 * Class holding Entity based interfaces shared across test classes
 */
public class CommonInterfaces {

	/**
	 * interface only containing primitive properties
	 */
	public static interface PrimitiveEntity extends Entity<PrimitiveEntity> {
		public PrimitiveEntity setString(String s);

		@NotNull(groups = Special.class)
		public String getString();

		public Entity setInteger(Integer i);

		@Named("Integer")
		public Integer getInteger();
	}

	/**
	 * containing primitive and Entity properties
	 */
	@Named("Nested")
	public static interface NestedEntity extends Entity<NestedEntity> {
		public String getString();

		public void setString(String s);

		public PrimitiveEntity getPE();

		public void setPE(PrimitiveEntity pe);
	}

	/**
	 * containing arrays and lists, map
	 */
	public static interface CollectionEntity extends Entity<CollectionEntity> {
		public List<String> getStrings();

		public void setStrings(List<String> strings);

		public String[] getArrayStrings();

		public void setArrayStrings(String[] strings);

		public Map<String, String> getMap();

		public void setMap(Map<String, String> map);
	}

	public static interface ExplicitIdEntity extends Entity {
		@Id
		public String getName();

		public ExplicitIdEntity setName(String name);
	}

	public static interface ImplicitIdEntity extends Entity {
		public String getId();

		public ImplicitIdEntity setId(String id);
	}

	public static interface ComputedPropertyEntity extends PrimitiveEntity {
		@Computed(StringComputer.class)
		public String getComputed();
	}

	public static interface ReferencingEntity extends PrimitiveEntity {
		@Reference
		public PrimitiveEntity getPE();

		public ReferencingEntity setPE(PrimitiveEntity pe);
	}

	public static interface LazyLoadingEntity extends PrimitiveEntity {
		@Reference(lazy = true)
		public PrimitiveEntity getPE();

		public LazyLoadingEntity setPE(PrimitiveEntity pe);
	}

	public static class StringComputer implements Computer<PrimitiveEntity, String> {

		@Override
		public String compute(PrimitiveEntity pe) {
			return pe.getString() + pe.getInteger();
		}
	}
}
