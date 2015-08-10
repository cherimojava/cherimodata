/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mongodb.client;

import com.mongodb.MongoClient;

/**
 * little Helper class to overcome the package private limitation
 */
public class MongoHelper
{

    public static MongoDatabase getMongoDatabase( MongoClient client, String dbname )
    {
        return client.getDatabase( dbname );
    }
}
