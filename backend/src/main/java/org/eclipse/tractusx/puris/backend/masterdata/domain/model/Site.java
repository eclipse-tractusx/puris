package org.eclipse.tractusx.puris.backend.masterdata.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Site {

    @Id
    @NotNull
    private String bpns;
    private String name;
    private String streetAndNumber;
    private String zipCodeAndCity;
    private String country;

    public Site(String bpns) {
        this.bpns = bpns;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Site) {
            return bpns.equals(((Site) obj).bpns);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return bpns.hashCode();
    }
}
