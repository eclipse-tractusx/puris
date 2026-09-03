/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.irs.logic.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningRootGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningRootGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsJobRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrsJobService {

	private static final String JOB_CREATION_PATH = "irs/recursive/jobs";

	private final IrsJobRepository irsJobRepository;

	private final IrsRequestBodybuilder irsRequestBodybuilder;

	private final IrsRequestQueueService irsRequestQueueService;

	private final ReportedDemandAndCapacityNotificationService reportedNotificationService;

	private final IrsAdapterConfiguration irsAdapterConfiguration;

	private final IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;

	private final MaterialService materialService;

	/**
	 * Persists the given IRS job locally and sends a
	 * job-creation request to the IRS. The local job's request status is updated to
	 * reflect the outcome of the outbound call. The job id and state are left to be
	 * filled in later by the IRS callback.
	 *
	 * @param irsJob the new IRS job to persist and send (must not carry a UUID)
	 * @return the persisted IRS job
	 */
	public IrsJob createAndSend(IrsJob irsJob) {
		if (!irsAdapterConfiguration.isIrsAdapterEnabled()) {
			log.info("IRS adapter is disabled. Skipping IRS Job Creation");
			return null;
		}
		IrsJob saved = create(irsJob);
		return sendAndUpdateStatus(saved);
	}

	/**
	 * Enqueues a job-creation request for the given (already persisted) job to be
	 * sent and retried
	 * asynchronously by {@link IrsRequestQueueWorker}. The job's request status
	 * is set to {@link IrsRequestStatusEnumeration#PENDING} until the worker
	 * resolves the request to a terminal outcome.
	 *
	 * @param saved the already-persisted IRS job to send
	 * @return the persisted IRS job
	 */
	private IrsJob sendAndUpdateStatus(IrsJob saved) {
		String body = irsRequestBodybuilder.buildJobCreationRequestBody(saved).toString();
		irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, JOB_CREATION_PATH, body, null,
				IrsQueuedRequestTypeEnumeration.JOB_CREATE, saved.getUuid());

		log.info("Enqueued IRS job creation request for job {}", saved.getUuid());
		saved.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);

		return update(saved);
	}

	private IrsJob create(IrsJob irsJob) {
		if (irsJob.getUuid() != null) {
			log.error("Could not create IRS job because UUID was provided for a new IRS job");
			throw new IllegalArgumentException("UUID must not be set when creating a new IRS job.");
		}

		assertMaterialEligibleForIrsJob(irsJob.getMaterial());

		return irsJobRepository.save(irsJob);
	}

	private IrsJob update(IrsJob irsJob) {
		if (irsJob.getUuid() == null) {
			log.error("Could not update IRS job because UUID was missing");
			throw new IllegalArgumentException("UUID must be set when updating an IRS job.");
		}

		UUID uuid = Objects.requireNonNull(irsJob.getUuid());

		if (irsJobRepository.findById(uuid).isEmpty()) {
			log.error("Could not update IRS job {} because it did not exist before", uuid);
			throw new NoSuchElementException("IRS job does not exist.");
		}

		return irsJobRepository.save(irsJob);
	}

	/**
     * Creates and sends a new IRS job for every root grant that has the given notification among
     * its reportedNotifications. A grant whose globalAssetId does not resolve to a known material,
     * or whose material is not eligible for an IRS job, is skipped and logged — it does not prevent
     * jobs from being created for the other grants.
     *
     * @param notification the notification whose root grants should each get a new IRS job
     */
    public void createJobsForNotification(ReportedDemandAndCapacityNotification notification) {
        List<IrsChainOpeningRootGrant> grants = irsChainOpeningRootGrantRepository.findAllByReportedNotifications_Uuid(notification.getUuid());
        createJobsForRootGrants(grants);
    }

    private void createJobsForRootGrants(List<IrsChainOpeningRootGrant> grants) {
        for (IrsChainOpeningRootGrant grant : grants) {
            Material material = materialService.findByMaterialNumberCx(grant.getGlobalAssetId());
            if (material == null) {
                log.error("No material found for globalAssetId {} while creating IRS job for sourceDisruptionId {}",
                    grant.getGlobalAssetId(), grant.getSourceDisruptionId());
                continue;
            }

            IrsJob irsJob = new IrsJob();
            irsJob.setMaterial(material);
            irsJob.setSourceDisruptionId(grant.getSourceDisruptionId());

            try {
                createAndSend(irsJob);
            } catch (IllegalArgumentException e) {
                log.error("Failed to create IRS job for globalAssetId {}, sourceDisruptionId {}",
                    grant.getGlobalAssetId(), grant.getSourceDisruptionId(), e);
            }
        }
    }

	/**
	 * Ensures that the given material is eligible to be used for an IRS job.
	 * A material is eligible only if all of the following conditions are met:
	 * <ol>
	 * <li>The material is a product ({@code productFlag == true}).</li>
	 * <li>The material is the parent in at least one currently-valid material
	 * relation.</li>
	 * <li>At least one child material of those valid relations is affected by a
	 * currently-active
	 * reported demand and capacity notification.</li>
	 * </ol>
	 *
	 * @param material the material to check
	 * @throws IllegalArgumentException if the material is not eligible for an IRS
	 *                                  job
	 */
	private void assertMaterialEligibleForIrsJob(Material material) {
		if (material == null) {
			log.error("Could not create IRS job because no material was provided");
			throw new IllegalArgumentException("A material is required to create an IRS job.");
		}

		if (!material.isProductFlag()) {
			log.error("Material {} is not a product and cannot be used for an IRS job",
					material.getOwnMaterialNumber());
			throw new IllegalArgumentException("Material must be a product to be used for an IRS job.");
		}

		if (!reportedNotificationService.isAnyChildAffectedByActiveNotifications(material)) {
			log.error("No child material of material {} is affected by a currently-active notification",
					material.getOwnMaterialNumber());
			throw new IllegalArgumentException(
					"At least one child material must be affected by a currently-active notification to create an IRS job.");
		}
	}

}
