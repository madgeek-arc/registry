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
