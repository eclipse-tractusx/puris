/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.common.edc.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EDR_Dto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EndpointDataReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * This class contains the endpoint for receiving the authCodes from
 * the counterparty's dataplane. 
 */
@RestController
@Slf4j
public class EndpointDataReferenceReceiver {

    @Autowired
    private EndpointDataReferenceService edcService;

    /**
     * This endpoint awaits incoming authCodes from external
     * partners during a consumer pull transfer. 
     * @param body
     * @return
     */
    @PostMapping("/edrendpoint")
    @Operation(summary = "Endpoint for receiving the authCodes from the counterparty's dataplane",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = {
                @ExampleObject(name = "EDR Token",
                    value = "{\n" +
                        "  \"id\" : \"6c2e5600-294a-488e-8ce1-2073806c1927\",\n" +
                        "  \"endpoint\" : \"http://sokrates-dataplane:8181/api/public\",\n" +
                        "  \"authKey\" : \"Authorization\",\n" +
                        "  \"authCode\" : \"eyJhbGciOiJSUzI1NiJ9.eyJleHAiOjE2OTMyMTgzNzIsImRhZCI6Ik5YY2J1VktDOVFCTWY5eG9BQUFBQWN3RmE2blhIS1JEcllHTnZCeUNlbTdCNTBOVkVWd05vczdsNU1YM0lKL1pVK0NUU3NvcE02ZUVFbzloYmx5ak1aTlJYYnRnOUZpZ04vampRVmVlaDFWQk01RHA1RXUyN2VIbk5TNCt0VE9uMEZFZTVGVGNITjh6dWwwZG5VbW1nbCtsVERxODZhdHUwbjYxZVV5dXNzQ25pbTZDaWhOai9lRXZ0cUZydVhCbXNIWVQvSTNYN3JrYk1FeFZWRFhBOHNPVGFGeFVpTnl3YTI0cURpWkFnc0Nka1FDenBaSFVIWk16ZDQrNURKTTVkVkNDWnEyUHBnRDhuR09wVzVVTTJUYUlmNHdMOTFQbnhEdEQ2a1dWTCtNQUNGSE41S2RyVUt2a3pOeXAyRzVYcWJ2V29waEhhY0VLTTR4UTZDc2dkUHFoMGN6elNnbFZGdy9IWVl3ZDBXQVpCcnVSSTlUekY4WTJkMXBscW5zRFFwcWh1bUUzUUtGTW5UbzUxWVFuVmdzcUZ5ZEpObkpMZjQzWnBwOXNPZ3U0V1Yxd3lxN096QzFXeFZjRk9xNVZQMkJRYk5pS29YeEZINXd3WmZJMzR6dFNCTFE0akUvY3BJTlk0Rks0Tk95YjNicENOYlpDamplbXRYaE1jTXlUQ0tyMU8zS2RvdkhEMnEzMjhVdDk0U3hzZW9jK0FCUXZaTk5EK1hGbm9Bcm01K01jbkVXdkdPVHJPT0NIaG83bWhnTGUxRzVEaHBqRjFaclBVTHFNNitzTmZzU0l4REhSOEtzMW1OMGhwajVwMUJ4Tm9rMDE2bGNJSURTbnVpclhyZWlzVzhEK0NHRDlEREdlUlVNQkk3cVBCSVc4eXY5RmU1eldrNHU0cERzbFAwR0dPYjBpMHVBMnFyS0dFS2JQUmd6ODRPeWZTNW84KzdiQ1dkMElKdmZERlRGK3UwVVgzVWordmFtYlZPREpQK1FmbXpOM0U2NFdaY1ozVDRMQURKZWhGcEZ5WGh6bUM5SnAyK2RYZ2syWE4rUnVzbGZFeGNMbXA0U05DenRxZDVQTXZqVjdOMXFYZnQ5a0hLLytwYWhoVUUxLzBENTEvS04razN4cmxoTDFPVnF3QzYvUjlScDA5WUk4dno3enNXS0V3aEpNOGk2OCtRdHlDMldNay9ucVpjTmtieVQ3T21WZ1R2bFhxYmV5WlpVOTlSdGxVNExJemZjM3hKRlIvUGpDa2xUK2dkZXpsaXFnbGFWMlFLL2EwVHpXbFlncXVDQ21kQzhieGJaR1JFWFdrbVFlNUwwZC9UWWZZYVRCNWdURkRDL1ZDRnYrRUFzNFhXQ2pBQ2NraW5rTDJDYWlZYm5WV0I5Zi9nRkJaWkY1cDhBNHFrMkIvdkNBS3ByckpQZkMwZTZIQXFxUlZxRFZ1VktZRHl1U3V5Umx6Q2hPbXZvNHRoRUhDcmtwVFlCNzZIVlNPeld2ZFJubmJRQmNyOW5YQkVCd2xvbGZkV3F4TGZUY3dmRnFJV2tla0YrT3NzTDE1TVV5Ym1vZXlxVnUxSkdpVGN1Sy90NGhINkxEQXNWeVhOT294NVF4eDE1b0dIN1hoWXVrWEpBb2J6SXduVWtrM2cxOE1GMko1LzNIbVpHVDM3bHU0SUNmR3g3d3JxY0xHSEJnb3l5MndsZDl3WVdGMHB4RXlwbmltZWZHZTQ4di9TbEIwYldRekY5S0UrVGwrcTA3d1c2aHVNbU51VDhUNWJLeXBIUVYzMnlpSkozbGx3WnpTVmc5NWtsWXo3VThxZ3FhNlJCa1plbXovNFhISG4vVXRGTUl5K1VnZkNEbVJ5L2dORmZOQkdmQ2RQSGNidTh3RXRvd2QrOTJ5R0JPLzZIbFgwdysxYlNzVGZYRDI1U21ZdWtRcDhTUlgyTEtTaUtzc3VKQ0QwOTdCbTAvRkorUkxJOVZHd2FJRXBLMTIzOEVyUTFublVIbGNnblByY2xKVE5jdWtXNFp5YkhESUYrT3YwNHFMNnpYa292NXBoYXB0b1dBaHNCaE4vbW5sUWlHYnpSOGN4WWJ1SEpRRDRxd3VuZzloYVR3b2RyeGhxTXh4RVM5SHhVN3UrQ3U2SkhaRkVoL0pEeWpzVnc5ZXg5Uy94aUxFNjNZR21iWnNBK3Q5QXRzdjJPVC9TbS9ZMkk9IiwiY2lkIjoicHJvZHVjdC1zdG9jay1yZXF1ZXN0LWFwaTphMDUwYmY1MS0xYWIxLTRkZTQtODM0Yy1mNWNlMGEzMDMwZWEifQ.oxERHPWzhunY18bJjgTGjlvZhHUGtDm2V_svDVkYz3VulluMjoFV5jm1EDuy46Z3vEgLQmKsFsG-VTsVwHaJKh5pnlx1QEj8SUFYu5JZraIL6vghI1X3cPb0qNfCBX191ztJCRgszyNMsxXGd4GQjkUdnP8J58UtBwaoNTQNWxMOIYgpBaNUuyPr6wSz1ek05B-TahoVjfjFmgAlDoKLjLQ-Ec-ejfM6FaITvifrVJyGUyHGiqzU7v4_Dd29rVHVHSE_F3rr6sLV56PeU30coBAn_q7hnTN6GWdulxg3vjc5uDcqSntxmxGE_STI-paBDHG5aToQzNNgARpv3SJDjg\",\n" +
                        "  \"properties\" : {\n" +
                        "    \"cid\" : \"product-stock-request-api:a050bf51-1ab1-4de4-834c-f5ce0a3030ea\"\n" +
                        "  }\n" +
                        "}\n")
            })
    ))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Received invalid message")
    })
    private ResponseEntity<String> authCodeReceivingEndpoint(@RequestBody JsonNode body) {
        log.debug("Received edr data:\n" + body.toPrettyString());
        String transferId = body.get("id").asText(); 
        String authKey = body.get("authKey").asText();
        String authCode = body.get("authCode").asText();
        String endpoint = body.get("endpoint").asText();
        if (transferId == null || authCode == null) {
            log.warn("authCodes endpoint received invalid message:\n" + body.toPrettyString());
            return ResponseEntity.status(400).build();
        }
        edcService.save(transferId, new EDR_Dto(authKey, authCode, endpoint));
        log.debug("authCodes endpoint stored authCode for " + transferId);
        return ResponseEntity.status(200).build();
    }

}
