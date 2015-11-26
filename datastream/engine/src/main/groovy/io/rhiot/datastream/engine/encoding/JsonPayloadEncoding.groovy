/**
 * Licensed to the Rhiot under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rhiot.datastream.engine.encoding

import com.fasterxml.jackson.databind.ObjectMapper
import io.rhiot.bootstrap.classpath.Bean

@Bean
class JsonPayloadEncoding implements PayloadEncoding {

    private final ObjectMapper objectMapper = new ObjectMapper()

    @Override
    byte[] encode(Object payload) {
        objectMapper.writeValueAsBytes([payload: payload])
    }

    @Override
    def <T> T decode(byte[] payload, Class<T> type) {
        def response = objectMapper.readValue(payload, Map.class)
        objectMapper.convertValue(response.payload, type)
    }

}