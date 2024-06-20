package org.eclipse.tractusx.puris.backend.production.domain.repository;

import java.util.UUID;

import org.eclipse.tractusx.puris.backend.production.domain.model.Production;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionRepository<T extends Production> extends JpaRepository<T, UUID> {
    
}
