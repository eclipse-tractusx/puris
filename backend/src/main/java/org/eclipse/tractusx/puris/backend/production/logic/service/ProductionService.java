package org.eclipse.tractusx.puris.backend.production.logic.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.production.domain.model.Production;
import org.eclipse.tractusx.puris.backend.production.domain.repository.ProductionRepository;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ProductionService<T extends Production>  {
    @Autowired
    protected ProductionRepository<T> repository;

    public final List<T> findAll() {
        return repository.findAll();
    }

    public final T findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }    

    public final List<T> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(production -> production.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final List<T> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAll().stream().filter(production -> production.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .toList();
    }

    public final List<T> findAllByFilters(
        Optional<String> ownMaterialNumber,
        Optional<String> bpnl,
        Optional<String> bpns,
        Optional<Date> dayOfCompletion) {
        Stream<T> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(production -> production.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(production -> production.getPartner().getBpnl().equals(bpnl.get()));
        }
        if (bpns.isPresent()) {
            stream = stream.filter(production -> production.getProductionSiteBpns().equals(bpns.get()));
        }
        if (dayOfCompletion.isPresent()) {
            LocalDate localEstimatedTimeOfCompletion = Instant.ofEpochMilli(dayOfCompletion.get().getTime())
                .atOffset(ZoneOffset.UTC)
                .toLocalDate();
            stream = stream.filter(production -> {
                LocalDate productionEstimatedTimeOfCompletion = Instant.ofEpochMilli(production.getEstimatedTimeOfCompletion().getTime())
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate();
                return productionEstimatedTimeOfCompletion.getDayOfMonth() == localEstimatedTimeOfCompletion.getDayOfMonth();
            });
        }
        return stream.toList();
    }

    public final List<Double> getQuantityForDays(String material, String partnerBpnl, String siteBpns, int numberOfDays) {
        List<Double> quantities = new ArrayList<>();
        LocalDate localDate = LocalDate.now();

        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            List<T> productions = findAllByFilters(Optional.of(material), Optional.of(partnerBpnl), Optional.of(siteBpns), Optional.of(date));
            double productionQuantity = getSumOfQuantities(productions);
            quantities.add(productionQuantity);

            localDate = localDate.plusDays(1);
        }
        return quantities;
    }

    public final T update(T production) {
        if (production.getUuid() == null || repository.findById(production.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(production);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    private final double getSumOfQuantities(List<T> productions) {
        double sum = 0;
        for (T production : productions) {
            sum += production.getQuantity();
        }
        return sum;
    }
}
