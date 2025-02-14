/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.supply.logic.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.Supply;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class SupplyService<T extends Supply, TReported extends Supply, TRepository extends JpaRepository<TReported, UUID>, TStock extends ItemStock, TStockService extends ItemStockService<TStock>> {
    private TStockService stockService;
    private MaterialService materialService;
    protected PartnerService partnerService;
    protected TRepository repository;

    protected abstract T createSupplyInstance();
    protected abstract List<Double> getAddedValues(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, int numberOfDays);
    protected abstract List<Double> getConsumedValues(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, int numberOfDays);
    protected abstract boolean validate(TReported daysOfSupply);

    protected final Function<TReported, Boolean> validator;

    public SupplyService(TStockService stockService, MaterialService materialService, PartnerService partnerService, TRepository repository) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.materialService = materialService;
        this.stockService = stockService;
        this.validator = this::validate;
    }

    public final TReported createReportedSupply(TReported supply) {
        if (!validator.apply(supply)) {
            throw new IllegalArgumentException("Invalid days of supply");
        }
        if (repository.findAll().stream().anyMatch(d -> d.equals(supply))) {
            throw new KeyAlreadyExistsException("Supply already exists");
        }
        return repository.save(supply);
    }

    public final void deleteReportedSupply(TReported entity) {
        repository.delete(entity);
    }

    /**
     * Calculates the days of supply for a given material, partner, and site over a specified number of days.
     * It combines the projected item stock of a given day and consumed values (outbound deliveries/demands) on the following days
     * to forecast the number of days the stock will last.
     *
     * NOTE: Added values (inbound deliveries/production) are only used for calculation projected stocks and not for the calculation of Days Of Supply
     * @param material the material identifier for which the days of supply are being calculated.
     * @param partnerBpnl The bpnl of the partner.
     * @param siteBpns the bpns of the site where the added values and consumed values are recorded.
     * @param numberOfDays the number of days over which the forecast should be calculated.
     * @return a list of {@link Supply} objects, each containing the calculated days of supply for a specific date.
     */
    public final List<T> calculateDaysOfSupply(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, int numberOfDays) {
        /* 
         * Due to the nature of the Days of Supply calculation logic, calculating Days of Supply for less than 2 days does not make sense
         * since there are no upcoming days to base the Days of Supply on. 
         * 
         * This logic also leads to the length of the List of Supply to always be numberOfDays - 1
         */
        if (numberOfDays < 2) {
            return new ArrayList<T>();
        }
        List<T> supplyList = new ArrayList<>();
        LocalDate localDate = LocalDate.now();
        Partner partner = partnerBpnl.isPresent()? partnerService.findByBpnl(partnerBpnl.get()) : null;

        List<Double> addedValues = getAddedValues(material, partnerBpnl, siteBpns, numberOfDays);
        List<Double> consumedValues = getConsumedValues(material, partnerBpnl, siteBpns, numberOfDays);
        double projectedStockQuantity = stockService.getInitialStockQuantity(material, partnerBpnl, siteBpns);

        for (int i = 0; i < numberOfDays - 1; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            projectedStockQuantity = projectedStockQuantity - consumedValues.get(i) + addedValues.get(i);

            var remainingConsumedValues = consumedValues.subList(i + 1, consumedValues.size());

            double daysOfSupply = getDaysOfSupply(projectedStockQuantity, remainingConsumedValues);

            T supply = createSupplyInstance();
            supply.setMaterial(materialService.findByOwnMaterialNumber(material));
            supply.setDate(date);
            supply.setDaysOfSupply(daysOfSupply);
            if (partner != null){
                supply.setPartner(partner);
            };
            supplyList.add(supply);

            localDate = localDate.plusDays(1);
        }

        return supplyList;
    }

    /**
     * Merges own and reported deliveries into a single list.
     * @param list1 Own deliveries
     * @param list2 Reported deliveries
     * @return a new list containing the summed delivery quantities from the input lists.
     */
    public static List<Double> mergeDeliveries(List<Double> list1, List<Double> list2) {
        if (list1.size() != list2.size()) {
            throw new IllegalArgumentException("Lists must be of the same length");
        }

        List<Double> mergedList = new ArrayList<>(list1.size());

        for (int i = 0; i < list1.size(); i++) {
            mergedList.add(list1.get(i) + list2.get(i));
        }

        return mergedList;
    }

    /**
     * Calculates the number of days of supply based on the current stock quantity and a list of consumed values.
     * @param stockQuantity Current stock amount
     * @param addedValues Remaining list of added values for current iteration
     * @param consumedValues Remaining list of consumed values for current iteration
     * @return The number of days of supply that the stock can cover.
     */
    private double getDaysOfSupply(double stockQuantity, List<Double> consumedValues) {
        double daysOfSupply = 0;

        for (int i = 0; i < consumedValues.size(); i++) {
            Double consumedValue = consumedValues.get(i);

            if ((stockQuantity - consumedValue) >= 0) {
                daysOfSupply += 1;
                stockQuantity = stockQuantity - consumedValue;
            } else if ((stockQuantity - consumedValue) < 0 && stockQuantity > 0) {
                double fractional = stockQuantity / consumedValue;
                daysOfSupply += fractional;
                stockQuantity = stockQuantity - consumedValue;
                break;
            } else {
                break;
            }
        }
        return daysOfSupply;
    }
}
