import { DirectionType } from '../erp/directionType';

export enum BatchRunStatus {
    IN_PROGRESS = "IN_PROGRESS",
    COMPLETED = "COMPLETED",
    COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS",
    FAILED = "FAILED"
}

export enum BatchRunEntryStatus {
    SUCCESS = "SUCCESS",
    ERROR = "ERROR",
    SKIPPED = "SKIPPED"
}

export enum InformationType {
    STOCK = 'STOCK',
    DEMAND = 'DEMAND',
    PRODUCTION = 'PRODUCTION',
    DAYS_OF_SUPPLY = 'DAYS_OF_SUPPLY',
    DELIVERY = 'DELIVERY',
}

export interface BatchRunDto {
    id: string;
    startTime: string;
    endTime?: string;
    status: BatchRunStatus;
    durationInSeconds: number;
    totalEntries: number;
    totalErrorCount: number;
}

export interface BatchRunEntryDto {
    id: string;
    ownMaterialNumber: string;
    partnerName: string;
    direction: DirectionType;
    informationType: InformationType;
    status: BatchRunEntryStatus;
    errorMessage?: string;
}

export default {};
