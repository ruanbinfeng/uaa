package org.cloudfoundry.identity.uaa.mock.zone;


import org.cloudfoundry.identity.uaa.config.YamlServletProfileInitializer;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IdentityZoneIntegrationTest {

    private static XmlWebApplicationContext webApplicationContext;
    private static MockMvc mockMvc;

    @BeforeClass
    public static void setUp() throws Exception {
        webApplicationContext = new XmlWebApplicationContext();
        webApplicationContext.setServletContext(new MockServletContext());
        new YamlServletProfileInitializer().initialize(webApplicationContext);
        webApplicationContext.setConfigLocation("file:./src/main/webapp/WEB-INF/spring-servlet.xml");
        webApplicationContext.refresh();

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testCreateIdentityZone() throws Exception {
        IdentityZone identityZone = getIdentityZone();

        MockHttpServletRequestBuilder request = post("/identity-zones")
                .content(new ObjectMapper().writeValueAsString(identityZone))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON);

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn();

        IdentityZone resultZone = new ObjectMapper().readValue(result.getResponse().getContentAsString(), IdentityZone.class);
        Assert.assertEquals(identityZone.getName(), resultZone.getName());
        Assert.assertEquals(identityZone.getDomain(), resultZone.getDomain());
        Assert.assertEquals(identityZone.getDescription(), resultZone.getDescription());

        UUID.fromString(resultZone.getId());
    }

    private IdentityZone getIdentityZone() {
        return new IdentityZone(null, "My Zone", "myzone", "A Testing Zone");
    }

    @Test
    public void testDuplicateIdentityZone() throws Exception {
        IdentityZone identityZone = getIdentityZone();

        MockHttpServletRequestBuilder request = post("/identity-zones")
                .content(new ObjectMapper().writeValueAsString(identityZone))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isCreated());
        mockMvc.perform(request).andExpect(status().isConflict());
    }
}
