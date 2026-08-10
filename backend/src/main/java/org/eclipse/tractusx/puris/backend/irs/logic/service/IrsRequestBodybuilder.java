package org.eclipse.tractusx.puris.backend.irs.logic.service;

import java.time.Duration;
import java.time.Instant;

import org.eclipse.tractusx.puris.backend.common.edc.domain.model.JsonLdConstants;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrant;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrsRequestBodybuilder {

    private static final String PURIS_FRAMEWORK_POLICY_ID = "puris-framework-policy";
    private static final String DTR_FRAMEWORK_POLICY_ID = "dtr-framework-policy";

    private static final long FRAMEWORK_POLICY_VALID_YEARS = 10;

    private final VariablesService variablesService;

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Builds the request body for creating a Chain Opening Grant at the IRS.
     * <p>
     * {@link IrsChainOpeningGrant} already carries the wire shape (via its
     * {@code @JsonProperty} annotations), so it is serialized directly instead of being mapped field by field.
     *
     * @param grant the Chain Opening Grant to build the request body for
     * @return the grant creation request body
     */
    public JsonNode buildGrantCreationRequestBody(IrsChainOpeningGrant grant) {
        return objectMapper.valueToTree(grant);
    }

    /**
     * Builds the request body for registering the PURIS framework policy at the IRS Policy Store,
     * @return the request body as a JsonNode
     */
    public JsonNode buildPurisFrameworkPolicyCreationRequestBody() {
        return buildFrameworkPolicyCreationRequestBody(PURIS_FRAMEWORK_POLICY_ID, variablesService.getPurisPurposeWithVersion());
    }

    /**
     * Builds the request body for registering the DTR framework policy at the IRS Policy Store,
     * @return the request body as a JsonNode
     */
    public JsonNode buildDtrFrameworkPolicyCreationRequestBody() {
        return buildFrameworkPolicyCreationRequestBody(DTR_FRAMEWORK_POLICY_ID, "cx.core.digitalTwinRegistry:1");
    }

    /**
     * Builds the request body for registering a framework policy at the IRS Policy Store,
     * so that the IRS accepts contracts requiring the framework agreement and usage purpose during
     * EDC negotiation. No {@code businessPartnerNumber} is set, so the policy applies to every BPN.
     *
     * @param policyId the ID of the policy to be created
     * @param purpose the usage purpose to be set in the policy
     * @return the request body as a JsonNode
     */
    public JsonNode buildFrameworkPolicyCreationRequestBody(String policyId, String purpose) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("validUntil",
            Instant.now().plus(Duration.ofDays(365L * FRAMEWORK_POLICY_VALID_YEARS)).toString());

        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode context = objectMapper.createObjectNode();
        context.put("odrl", JsonLdConstants.ODRL_NAMESPACE);
        payload.set("@context", context);
        payload.put("@id", policyId);

        ObjectNode policy = objectMapper.createObjectNode();
        ArrayNode permissions = objectMapper.createArrayNode();
        ObjectNode permission = objectMapper.createObjectNode();
        permission.put("action", "use");

        ObjectNode constraint = objectMapper.createObjectNode();
        ArrayNode andArray = objectMapper.createArrayNode();
        andArray.add(buildEqConstraint("FrameworkAgreement", variablesService.getPurisFrameworkAgreementWithVersion()));
        andArray.add(buildEqConstraint("UsagePurpose", purpose));
        constraint.set("and", andArray);
        permission.set("constraint", constraint);

        permissions.add(permission);
        policy.set("odrl:permission", permissions);
        payload.set("policy", policy);

        requestBody.set("payload", payload);
        return requestBody;
    }

    private ObjectNode buildEqConstraint(String leftOperand, String rightOperand) {
        ObjectNode constraint = objectMapper.createObjectNode();
        constraint.put("leftOperand", leftOperand);
        ObjectNode operator = objectMapper.createObjectNode();
        operator.put("@id", "odrl:eq");
        constraint.set("operator", operator);
        constraint.put("rightOperand", rightOperand);
        return constraint;
    }
}
