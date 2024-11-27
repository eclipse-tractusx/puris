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
import java.util.UUID;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.Supply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class SupplyService<T extends Supply, TReported extends Supply, TRepository extends JpaRepository<TReported, UUID>, TStock extends ItemStock, TStockService extends ItemStockService<TStock>> {
    @Autowired
    private TStockService stockService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    protected PartnerService partnerService;
    protected final TRepository repository;

    protected abstract T createSupplyInstance();
    protected abstract List<Double> getAddedValues(String material, String partnerBpnl, String siteBpns, int numberOfDays);
    protected abstract List<Double> getConsumedValues(String material, String partnerBpnl, String siteBpns, int numberOfDays);
    protected abstract boolean validate(TReported daysOfSupply);

    protected final Function<TReported, Boolean> validator;

    public SupplyService(TRepository repository, PartnerService partnerService, MaterialService materialService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.materialService = materialService;
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
     * It combines added values (deliveries/productions), and consumed values (deliveries/demands) to forecast the number of days the stock will last.
     *
     * @param material the material identifier for which the days of supply are being calculated.
     * @param partnerBpnl The bpnl of the partner.
     * @param siteBpns the bpns of the site where the added values and consumed values are recorded.
     * @param numberOfDays the number of days over which the forecast should be calculated.
     * @return a list of {@link Supply} objects, each containing the calculated days of supply for a specific date.
     */
    public final List<T> calculateDaysOfSupply(String material, String partnerBpnl, String siteBpns, int numberOfDays) {
        List<T> supplyList = new ArrayList<>();
        LocalDate localDate = LocalDate.now();
        Partner partner = partnerService.findByBpnl(partnerBpnl);

        List<Double> addedValues = getAddedValues(material, partnerBpnl, siteBpns, numberOfDays);
        List<Double> consumedValues = getConsumedValues(material, partnerBpnl, siteBpns, numberOfDays);
        double stockQuantity = stockService.getInitialStockQuantity(material, partnerBpnl);

        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            var remainingAddedValues = addedValues.subList(i, addedValues.size());
            var remainingConsumedValues = consumedValues.subList(i, consumedValues.size());
            double daysOfSupply = getDaysOfSupply(stockQuantity, remainingAddedValues, remainingConsumedValues);

            T supply = createSupplyInstance();
            supply.setMaterial(materialService.findByOwnMaterialNumber(material));
            supply.setDate(date);
            supply.setDaysOfSupply(daysOfSupply);
            supply.setPartner(partner);
            supplyList.add(supply);

            stockQuantity = stockQuantity - consumedValues.get(i) + addedValues.get(i);

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
    private final double getDaysOfSupply(double stockQuantity, List<Double> addedValues, List<Double> consumedValues) {
        double daysOfSupply = 0;

        for (int i = 0; i < addedValues.size(); i++) {
            Double addedValue = addedValues.get(i);
            Double consumedValue = consumedValues.get(i);

            if ((stockQuantity + addedValue - consumedValue) >= 0) {
                daysOfSupply += 1;
                stockQuantity = stockQuantity + addedValue - consumedValue;
            } else if ((stockQuantity + addedValue - consumedValue) < 0 && stockQuantity > 0) {
                double fractional = (stockQuantity + addedValue) / consumedValue;
                daysOfSupply += fractional;
                stockQuantity = stockQuantity + addedValue - consumedValue;
                break;
            } else {
                break;
            }
        }
        return daysOfSupply;
    }
}
