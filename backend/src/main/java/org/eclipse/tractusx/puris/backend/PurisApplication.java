/*
 * Copyright (c) 2022,2024 Volkswagen AG
 * Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class PurisApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PurisApplication.class);
        app.setBanner((environment, sourceClass, out) -> {
            out.println("  ____             _       ____             _                  _");
            out.println(" |  _ \\ _   _ _ __(_)___  | __ )  __ _  ___| | _____ _ __   __| |");
            out.println(" | |_) | | | | '__| / __| |  _ \\ / _` |/ __| |/ / _ \\ '_ \\ / _` |");
            out.println(" |  __/| |_| | |  | \\__ \\ | |_) | (_| | (__|   <  __/ | | | (_| |");
            out.println(" |_|    \\__,_|_|  |_|___/ |____/ \\__,_|\\___|_|\\_\\___|_| |_|\\__,_|");
            out.println();
        });
        app.run(args);
    }

    @Bean
    public ModelMapper getModelMapper() {

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);
        return new ModelMapper();
    }

    @Bean
    public ExecutorService getExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public Validator getValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator();
        }
    }

}
