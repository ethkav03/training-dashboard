export const DataSource = {
  MANUAL: "MANUAL",
  GOOGLE_FIT: "GOOGLE_FIT",
  WHOOP: "WHOOP",
  MYFITNESSPAL: "MYFITNESSPAL",
  HEALTH_CONNECT: "HEALTH_CONNECT",
} as const;
export type DataSource = (typeof DataSource)[keyof typeof DataSource];

export const IntegrationProvider = {
  WHOOP: "WHOOP",
  HEALTH_CONNECT: "HEALTH_CONNECT",
} as const;
export type IntegrationProvider = (typeof IntegrationProvider)[keyof typeof IntegrationProvider];

export const SyncStatus = {
  IDLE: "IDLE",
  SYNCING: "SYNCING",
  SUCCESS: "SUCCESS",
  ERROR: "ERROR",
} as const;
export type SyncStatus = (typeof SyncStatus)[keyof typeof SyncStatus];

export const UnitSystem = {
  METRIC: "METRIC",
  IMPERIAL: "IMPERIAL",
} as const;
export type UnitSystem = (typeof UnitSystem)[keyof typeof UnitSystem];

export const OnboardingStatus = {
  PENDING: "PENDING",
  SKIPPED: "SKIPPED",
  COMPLETED: "COMPLETED",
} as const;
export type OnboardingStatus = (typeof OnboardingStatus)[keyof typeof OnboardingStatus];

export const ActivityType = {
  GYM: "GYM",
  RUNNING: "RUNNING",
  TEAM_SPORT_TRAINING: "TEAM_SPORT_TRAINING",
  MATCH: "MATCH",
  CYCLING: "CYCLING",
  WALKING: "WALKING",
  RECOVERY_SESSION: "RECOVERY_SESSION",
  OTHER: "OTHER",
} as const;
export type ActivityType = (typeof ActivityType)[keyof typeof ActivityType];

export const MatchResult = {
  WIN: "WIN",
  LOSS: "LOSS",
  DRAW: "DRAW",
} as const;
export type MatchResult = (typeof MatchResult)[keyof typeof MatchResult];

export const GoalType = {
  BODY_WEIGHT: "BODY_WEIGHT",
  CALORIE_INTAKE: "CALORIE_INTAKE",
  PROTEIN_INTAKE: "PROTEIN_INTAKE",
  TRAINING_FREQUENCY: "TRAINING_FREQUENCY",
  EXERCISE_PERFORMANCE: "EXERCISE_PERFORMANCE",
  SPORT_PERFORMANCE: "SPORT_PERFORMANCE",
  SLEEP_RECOVERY: "SLEEP_RECOVERY",
  CUSTOM: "CUSTOM",
} as const;
export type GoalType = (typeof GoalType)[keyof typeof GoalType];

export const GoalDirection = {
  INCREASE: "INCREASE",
  DECREASE: "DECREASE",
} as const;
export type GoalDirection = (typeof GoalDirection)[keyof typeof GoalDirection];

export const GoalStatus = {
  ON_TRACK: "ON_TRACK",
  AT_RISK: "AT_RISK",
  ACHIEVED: "ACHIEVED",
  PAUSED: "PAUSED",
} as const;
export type GoalStatus = (typeof GoalStatus)[keyof typeof GoalStatus];

export const ReadinessLevel = {
  HIGH: "HIGH",
  MODERATE: "MODERATE",
  LOW: "LOW",
} as const;
export type ReadinessLevel = (typeof ReadinessLevel)[keyof typeof ReadinessLevel];

export const AchievementType = {
  STREAK: "STREAK",
  PERSONAL_RECORD: "PERSONAL_RECORD",
  MILESTONE: "MILESTONE",
  WEEKLY_COMPLETION: "WEEKLY_COMPLETION",
} as const;
export type AchievementType = (typeof AchievementType)[keyof typeof AchievementType];

export const EnergyBalanceGranularity = {
  DAY: "day",
  WEEK: "week",
  MONTH: "month",
  YEAR: "year",
} as const;
export type EnergyBalanceGranularity =
  (typeof EnergyBalanceGranularity)[keyof typeof EnergyBalanceGranularity];

export const TimelineEntryKind = {
  WEIGHT: "WEIGHT",
  MEAL: "MEAL",
  TRAINING: "TRAINING",
  RECOVERY: "RECOVERY",
  ACHIEVEMENT: "ACHIEVEMENT",
} as const;
export type TimelineEntryKind = (typeof TimelineEntryKind)[keyof typeof TimelineEntryKind];
