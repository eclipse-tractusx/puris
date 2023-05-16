/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageContentDto;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Generated class for Stock of Products of a Supplier. This aspect represents
 * the latest quantities of a supplier's products that are on stock. The stock
 * represent the build-to-order (BTO) stocks already available for the customer.
 */
@Getter
@Setter
public class ProductStockSammDto extends MessageContentDto {

    @NotNull
    private Collection<Position> positions;

    @NotNull
    private String materialNumberCustomer;

    @Pattern(regexp = "(^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)|(^urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)")
    private Optional<String> materialNumberCatenaX;

    //private Optional<String> materialNumberSupplier;

    @JsonCreator
    public ProductStockSammDto(@JsonProperty(value = "positions") Collection<Position> positions,
                               @JsonProperty(value = "materialNumberCustomer") String materialNumberCustomer,
                               @JsonProperty(value = "materialNumberCatenaX") Optional<String> materialNumberCatenaX,
                               @JsonProperty(value = "materialNumberSupplier") Optional<String> materialNumberSupplier) {
        super(

        );
        this.positions = positions;
        this.materialNumberCustomer = materialNumberCustomer;
        this.materialNumberCatenaX = materialNumberCatenaX;
        //this.materialNumberSupplier = materialNumberSupplier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ProductStockSammDto that = (ProductStockSammDto) o;
        return Objects.equals(positions, that.positions)
                && Objects.equals(materialNumberCustomer, that.materialNumberCustomer)
                && Objects.equals(materialNumberCatenaX, that.materialNumberCatenaX);
        //&& Objects.equals(materialNumberSupplier, that.materialNumberSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positions, materialNumberCustomer, materialNumberCatenaX);//,
        // materialNumberSupplier);
    }
}
