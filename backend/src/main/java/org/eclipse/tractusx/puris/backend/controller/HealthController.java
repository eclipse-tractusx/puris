package org.eclipse.tractusx.puris.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller used for health and readiness probes.
 */
@RestController
@RequestMapping("health")
public class HealthController {

    /**
     * Return 200 OK status for health and readiness probes.
     *
     * @return 200 OK if healthy.
     */
    @GetMapping("/")
    @CrossOrigin
    public ResponseEntity<?> getHealth() {
        return ResponseEntity.ok().build();
    }

}