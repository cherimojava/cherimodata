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

import org.junit.extensions.dynamicsuite.ClassPath;
import org.junit.extensions.dynamicsuite.Filter;
import org.junit.extensions.dynamicsuite.TestClassFilter;
import org.junit.extensions.dynamicsuite.suite.DynamicSuite;
import org.junit.runner.RunWith;

@RunWith(DynamicSuite.class)
@ClassPath
@Filter(Suite.class)
public class Suite implements TestClassFilter {

	@Override
	public boolean include(String className) {
		String[] parts = className.split("\\.");
		return parts.length != 0 && parts[parts.length - 1].startsWith("_");
	}

	@Override
	public boolean include(@SuppressWarnings("rawtypes") Class clazz) {
		return (TestBase.class.isAssignableFrom(clazz));
	}
}
