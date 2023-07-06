package org.eclipse.tractusx.puris.backend.stock.domain.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DatapullAuthCode {

    private String transferId;

    private String authCode;
    
}
