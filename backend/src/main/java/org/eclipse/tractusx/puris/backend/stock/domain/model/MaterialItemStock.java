package org.eclipse.tractusx.puris.backend.stock.domain.model;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
public class MaterialItemStock extends ItemStock {
}
