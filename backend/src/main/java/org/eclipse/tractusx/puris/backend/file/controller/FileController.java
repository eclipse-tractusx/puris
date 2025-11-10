/*
 * Copyright (c) 2025 Volkswagen AG
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.file.controller;

import org.eclipse.tractusx.puris.backend.file.domain.model.DataImportResult;
import org.eclipse.tractusx.puris.backend.file.logic.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private ExcelService excelService;

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @Operation(
        summary = "Import data via excel file -- ADMIN ONLY",
        description =
            "Accepts a multipart/form-data upload of an Excel file with `.xlsx` extension. " +
            "The import supports Demand, Production, Delivery and Stock information. " +
            "The applicable type of information is automatically determined by the server. \n\n" +
            "Should any row of the data fail, no data will be saved. In this case detailed error reports are returned."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data imported successfully", content = @Content(schema = @Schema(implementation = DataImportResult.class))),
        @ApiResponse(responseCode = "400", description = "Bad request (missing file or invalid file type)", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized (missing or invalid API key)", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "422", description = "Invalid data", content = @Content(schema = @Schema(implementation = DataImportResult.class))),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        try {
            String filename = file.getOriginalFilename();
            if (filename != null && !(filename.endsWith(".xlsx"))) {
                return ResponseEntity.badRequest().body("Invalid File Type");
            }
            var result = excelService.readExcelFile(file.getInputStream(), filename);
            if (!result.getErrors().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error: An error occurred while processing the file. Check the server logs for details");
        }
    }
}
