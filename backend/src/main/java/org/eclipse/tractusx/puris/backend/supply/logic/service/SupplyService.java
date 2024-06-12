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

import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.Supply;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SupplyService<T extends Supply> {
    @Autowired
    private MaterialItemStockService stockService;
    @Autowired
    private MaterialService materialService;

    protected abstract T createSupplyInstance();
    protected abstract List<Double> getAddedValues(String material, String partnerBpnl, String siteBpns, int numberOfDays);
    protected abstract List<Double> getConsumedValues(String material, String partnerBpnl, String siteBpns, int numberOfDays);

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

        List<Double> addedValues = getAddedValues(material, partnerBpnl, siteBpns, numberOfDays);
        List<Double> consumedValues = getConsumedValues(material, partnerBpnl, siteBpns, numberOfDays);
        double stockQuantity = stockService.getInitialStockQuantity(material, partnerBpnl);

        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (i == numberOfDays - 1) {
                stockQuantity += addedValues.get(i);
            }

            double daysOfSupply = getDaysOfSupply(stockQuantity, consumedValues.subList(i, consumedValues.size()));

            T supply = createSupplyInstance();
            supply.setMaterial(materialService.findByOwnMaterialNumber(material));
            supply.setDate(date);
            supply.setDaysOfSupply(daysOfSupply);
            supplyList.add(supply);

            stockQuantity = stockQuantity - consumedValues.get(i) + addedValues.get(i);

            localDate = localDate.plusDays(1);
        }

        return supplyList;
    }

    /**
     * Calculates the number of days of supply based on the current stock quantity and a list of consumed values.
     * @param stockQuantity Current stock amount
     * @param demands Sublist of consumed values for current iteration
     * @return The number of days of supply that the stock can cover.
     */
    private final double getDaysOfSupply(double stockQuantity, List<Double> demands) {
        double daysOfSupply = 0;

        for (double demand : demands) {
            if (stockQuantity >= demand) {
                daysOfSupply += 1;
                stockQuantity = stockQuantity - demand;
            } else if (stockQuantity < demand && stockQuantity > 0) {
                double fractional = stockQuantity / demand;
                daysOfSupply = daysOfSupply + fractional;
                break;
            } else {
                break;
            }
        }
        return daysOfSupply;
    }
}
