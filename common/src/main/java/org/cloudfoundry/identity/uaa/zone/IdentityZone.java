package org.cloudfoundry.identity.uaa.zone;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class IdentityZone {
    String id;
    String name;
    String domain;
    String description;

    public IdentityZone() {
    }

    public IdentityZone(String id, String name, String domain, String description) {
        this.id = id;
        this.name = name;
        this.domain = domain;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
