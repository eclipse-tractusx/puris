package org.eclipse.tractusx.puris.backend.stock.logic.service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.tractusx.puris.backend.stock.domain.model.DatapullAuthCode;
import org.springframework.stereotype.Service;

@Service
public class DatapullAuthCodeService {

    // AuthCodes expire after a very short period, therefore it's not
    // useful to persist them in the database since the data is quite voluminous. 
    // Instead we're using a ConcurrrentHashMap. 
    final private ConcurrentHashMap<String, DatapullAuthCode> nonpersistantRepository = new ConcurrentHashMap<>();

    public DatapullAuthCode save(DatapullAuthCode datapullAuthCode) {
        final String transferId = datapullAuthCode.getTransferId();
        nonpersistantRepository.put(transferId, datapullAuthCode);

        // Start timer for deletion in five minutes
        final long timeForDeletion = System.currentTimeMillis() + 5 * 60 * 1000;
        new Thread(()-> {
            while(System.currentTimeMillis() < timeForDeletion){
                Thread.yield();
            }
            nonpersistantRepository.remove(transferId);
        }).start();;
        
        return datapullAuthCode;
    }

    public Optional<DatapullAuthCode> findByTransferId(String transferId) {
        if(nonpersistantRepository.containsKey(transferId))
            return Optional.of(nonpersistantRepository.get(transferId));
        return Optional.empty();
    }
    
}
