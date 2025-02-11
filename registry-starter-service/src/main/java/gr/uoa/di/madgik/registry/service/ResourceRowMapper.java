/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.Resource;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResourceRowMapper implements RowMapper<Resource> {

    @Override
    public Resource mapRow(ResultSet rs, int rowNum) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getString("id"));
        resource.setCreationDate(rs.getDate("creation_date"));
        resource.setModificationDate(rs.getDate("modification_date"));
        resource.setPayload(rs.getString("payload"));
        resource.setPayloadFormat(rs.getString("payloadformat"));
        resource.setVersion(rs.getString("version"));
        resource.setResourceTypeName(rs.getString("fk_name"));
        return resource;
    }
}
