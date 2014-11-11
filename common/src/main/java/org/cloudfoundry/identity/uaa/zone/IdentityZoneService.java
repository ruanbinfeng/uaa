package org.cloudfoundry.identity.uaa.zone;

import org.cloudfoundry.identity.uaa.rest.jdbc.AbstractQueryable;
import org.cloudfoundry.identity.uaa.rest.jdbc.JdbcPagingListFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IdentityZoneService extends AbstractQueryable<IdentityZone> {
    private static final class IdentityZoneRowMapper implements RowMapper<IdentityZone> {
        @Override
        public IdentityZone mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new IdentityZone(rs.getString(0), rs.getString(1), rs.getString(2), rs.getString(3));
        }
    }

    protected IdentityZoneService(JdbcTemplate jdbcTemplate, JdbcPagingListFactory pagingListFactory) {
        super(jdbcTemplate, pagingListFactory, new IdentityZoneRowMapper());
    }

    @Override
    protected String getBaseSqlQuery() {
        return "select * from identity_zones";
    }

    @Override
    protected String getTableName() {
        return "identity_zones";
    }
}
