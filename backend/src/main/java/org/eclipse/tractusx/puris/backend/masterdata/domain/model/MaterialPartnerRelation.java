package org.eclipse.tractusx.puris.backend.masterdata.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class MaterialPartnerRelation {

    @EmbeddedId
    Key key;

    private String partnerMaterialNumber;
    private boolean partnerSuppliesMaterial;
    private boolean partnerBuysMaterial;

    @ManyToOne
    @MapsId("ownMaterialNumber")
    @JoinColumn(name = "material_ownMaterialNumber")
    Material material;

    @ManyToOne
    @MapsId("uuid")
    @JoinColumn(name = "partner_uuid")
    Partner partner;

    public MaterialPartnerRelation() {
        this.key = new Key();
    }

    public MaterialPartnerRelation(Material material, Partner partner, String partnerMaterialNumber, boolean partnerSupplies, boolean partnerBuys) {
        this.material = material;
        this.partner = partner;
        this.key = new Key(material.getOwnMaterialNumber(), partner.getUuid());
        this.partnerMaterialNumber = partnerMaterialNumber;
        this.partnerSuppliesMaterial = partnerSupplies;
        this.partnerBuysMaterial = partnerBuys;
    }

    @Override
    public String toString() {
        return "MaterialPartnerRelation{" +
            "key=" + key +
            ", partnerMaterialNumber='" + partnerMaterialNumber + '\'' +
            ", partnerSuppliesMaterial=" + partnerSuppliesMaterial +
            ", partnerBuysMaterial=" + partnerBuysMaterial +
            ", material=" + material.getOwnMaterialNumber() +
            ", partner=" + partner.getBpnl() +
            '}';
    }

    @Embeddable
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Key implements Serializable {

        @Column(name = "material_ownMaterialNumber")
        private String ownMaterialNumber;

        @Column(name = "partner_uuid")
        private UUID partnerUuid;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equals(ownMaterialNumber, key.ownMaterialNumber) && Objects.equals(partnerUuid, key.partnerUuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownMaterialNumber, partnerUuid);
        }
    }
}
