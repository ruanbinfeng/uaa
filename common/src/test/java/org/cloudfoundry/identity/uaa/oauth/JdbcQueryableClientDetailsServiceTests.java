/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.oauth;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.cloudfoundry.identity.uaa.rest.jdbc.JdbcPagingListFactory;
import org.cloudfoundry.identity.uaa.rest.jdbc.LimitSqlAdapter;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.cloudfoundry.identity.uaa.zone.JdbcIdentityZoneProvisioning;
import org.cloudfoundry.identity.uaa.zone.MultitenancyFixture;
import org.cloudfoundry.identity.uaa.zone.MultitenantJdbcClientDetailsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = { "classpath:spring/env.xml", "classpath:spring/data-source.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcQueryableClientDetailsServiceTests {

    private JdbcQueryableClientDetailsService service;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LimitSqlAdapter limitSqlAdapter;

    private JdbcIdentityZoneProvisioning zoneDb;

    private static final String INSERT_SQL = "insert into oauth_client_details (client_id, client_secret, resource_ids, scope, authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity, refresh_token_validity, identity_zone_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE_SQL = "delete from oauth_client_details where client_id = ? and identity_zone_id = ?";

    
    private MultitenantJdbcClientDetailsService delegate;

    private IdentityZone otherZone;

    @Before
    public void setUp() throws Exception {
        IdentityZoneHolder.clear();
        jdbcTemplate = new JdbcTemplate(dataSource);
        delegate = new MultitenantJdbcClientDetailsService(dataSource);
        service = new JdbcQueryableClientDetailsService(delegate, jdbcTemplate, new JdbcPagingListFactory(jdbcTemplate,
                limitSqlAdapter));
        zoneDb = new JdbcIdentityZoneProvisioning(jdbcTemplate);
        otherZone = MultitenancyFixture.identityZone("other-zone-id", "myzone");
        zoneDb.create(otherZone);
    }

    private void addClients() {
        addClient("cf", "secret", "cc", "cc.read,cc.write", "implicit", "myRedirectUri", "cc.read,cc.write", 100, 200);
        addClient("scimadmin", "secret", "uaa,scim", "uaa.admin,scim.read,scim.write", "client_credentials",
                "myRedirectUri", "scim.read,scim.write", 100, 200);
        addClient("admin", "secret", "tokens,clients", "clients.read,clients.write,scim.read,scim.write",
                "client_credentials", "myRedirectUri", "clients.read,clients.write,scim.read,scim.write", 100, 200);
        addClient("app", "secret", "cc", "cc.read,scim.read,openid", "authorization_code", "myRedirectUri",
                "cc.read,scim.read,openid", 100, 500);
    }
    
    private void removeClients() {
        removeClient("cf");
        removeClient("scimadmin");    
        removeClient("admin");
        removeClient("app");
    }

    private void addClient(String id, String secret, String resource, String scope, String grantType,
            String redirectUri, String authority, long accessTokenValidity, long refreshTokenValidity) {
        jdbcTemplate.update(INSERT_SQL, id, secret, resource, scope, grantType, redirectUri, authority,
                accessTokenValidity, refreshTokenValidity, IdentityZoneHolder.get().getId());

    }
    
    private void removeClient(String id) {
        jdbcTemplate.update(DELETE_SQL, id, IdentityZoneHolder.get().getId());

    }

    

    @After
    public void tearDown() throws Exception {
        IdentityZoneHolder.clear();
        removeClients();
        IdentityZoneHolder.set(otherZone);
        removeClients();
        jdbcTemplate.update("delete from identity_zone where id = ?","other-zone-id");
        IdentityZoneHolder.clear();
    }

    @Test
    public void testQueryEquals() throws Exception {
        addClients();
        assertEquals(4, service.retrieveAll().size());
        assertEquals(2, service.query("authorized_grant_types eq \"client_credentials\"").size());
    }

    @Test
    public void testQueryExists() throws Exception {
        addClients();
        assertEquals(4, service.retrieveAll().size());
        assertEquals(4, service.query("scope pr").size());
    }
    
    @Test
    public void testQueryEqualsInAnotherZone() throws Exception {
        testQueryEquals();
        IdentityZoneHolder.set(otherZone);
        testQueryEquals();
        assertEquals(8,delegate.getTotalCount());
        
    }
    
    @Test
    public void testQueryExistsInAnotherZone() throws Exception {
        testQueryExists();
        IdentityZoneHolder.set(otherZone);
        testQueryExists();
        assertEquals(8,delegate.getTotalCount());
    }


}
