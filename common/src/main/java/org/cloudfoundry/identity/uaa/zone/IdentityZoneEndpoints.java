package org.cloudfoundry.identity.uaa.zone;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/identity-zones")
public class IdentityZoneEndpoints {

    @RequestMapping(method = POST)
    @ResponseStatus(CREATED)
    public IdentityZone createZone(@RequestBody IdentityZone identityZone) {
        identityZone.setId(UUID.randomUUID().toString());
        return identityZone;
    }
}
