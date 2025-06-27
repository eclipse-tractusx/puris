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

import org.eclipse.tractusx.puris.backend.file.logic.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private ExcelService excelService;

    @Operation(
        summary = "Upload an Excel file",
        description = "Accepts a multipart/form-data upload of an Excel file with `.xlsx` or `.xls` extension",
        parameters = {
            @Parameter(name = "X-API-KEY", description = "API key for authentication", required = true)
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request (missing file)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized (missing or invalid API key)"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            String filename = file.getOriginalFilename();
            if (filename != null && !(filename.endsWith(".xlsx"))) {
                return ResponseEntity.badRequest().body("Invalid file type");
            }
            excelService.readExcelFile(file.getInputStream());
            return ResponseEntity.ok("Data imported successfully.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("An error occurred while processing the file. Check the server logs for details");
        }
    }
}

