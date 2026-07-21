import type {
  ActivityType,
  AchievementType,
  DataSource,
  GoalDirection,
  GoalStatus,
  GoalType,
  MatchResult,
  OnboardingStatus,
  ReadinessLevel,
  TimelineEntryKind,
  UnitSystem,
} from "./enums.js";

export interface UserDto {
  id: string;
  email: string;
  name: string;
  avatarUrl: string | null;
  unitSystem: UnitSystem;
  dateOfBirth: string | null;
  heightCm: number | null;
  estimatedDailyBurnKcal: number | null;
  onboardingStatus: OnboardingStatus;
  createdAt: string;
}

export interface OnboardingRequest {
  dateOfBirth?: string;
  heightCm?: number;
  currentWeightKg?: number;
  unitSystem?: UnitSystem;
  trainingFrequencyPerWeek?: number;
  primaryGoal?: {
    type: GoalType;
    title: string;
    targetValue: number;
    targetUnit: string;
    direction: GoalDirection;
    targetDate?: string;
  };
}

export interface WeightEntryDto {
  id: string;
  date: string;
  weightKg: number;
  note: string | null;
  source: DataSource;
  isManuallyEdited: boolean;
}

export interface WeightTrendPointDto {
  date: string;
  value: number;
}

export interface WeightTrendDto {
  raw: WeightEntryDto[];
  movingAverage: WeightTrendPointDto[];
  rateOfChangeKgPerWeek: number | null;
  latestWeightKg: number | null;
  goalWeightKg: number | null;
}

export interface NutritionEntryDto {
  id: string;
  date: string;
  mealName: string | null;
  calories: number;
  proteinG: number | null;
  carbsG: number | null;
  fatG: number | null;
  notes: string | null;
  source: DataSource;
  isManuallyEdited: boolean;
}

export interface NutritionSummaryDto {
  date: string;
  totalCalories: number;
  totalProteinG: number;
  totalCarbsG: number;
  totalFatG: number;
  targetCalories: number | null;
  targetProteinG: number | null;
  estimatedBurn: {
    baselineKcal: number;
    trainingKcal: number;
    totalKcal: number;
    isEstimate: true;
  };
  balanceKcal: number | null;
}

export interface WorkoutSetDto {
  id?: string;
  setNumber: number;
  reps: number;
  weightKg: number;
  rpe: number | null;
  isWarmup: boolean;
}

export interface WorkoutExerciseDto {
  id?: string;
  name: string;
  orderIndex: number;
  sets: WorkoutSetDto[];
}

export interface MatchDetailDto {
  opponent: string | null;
  competition: string | null;
  position: string | null;
  minutesPlayed: number | null;
  result: MatchResult | null;
  performanceRating: number | null;
  keyStats: Record<string, number> | null;
  injuryNotes: string | null;
  reflection: string | null;
}

export interface TrainingSessionDto {
  id: string;
  type: ActivityType;
  date: string;
  durationMin: number;
  intensity: number;
  caloriesBurned: number | null;
  trainingLoad: number;
  notes: string | null;
  source: DataSource;
  isManuallyEdited: boolean;
  workout: { exercises: WorkoutExerciseDto[] } | null;
  matchDetail: MatchDetailDto | null;
}

export interface TrainingSessionWriteDto {
  type: ActivityType;
  date: string;
  durationMin: number;
  intensity: number;
  caloriesBurned?: number | null;
  notes?: string | null;
  workout?: { exercises: WorkoutExerciseDto[] };
  matchDetail?: MatchDetailDto;
}

export interface ExerciseProgressionPointDto {
  date: string;
  bestWeightKg: number;
  estimated1RM: number;
  volume: number;
  isPr: boolean;
}

export interface RecoveryRecordDto {
  id: string;
  date: string;
  sleepHours: number | null;
  sleepQuality: number | null;
  restingHr: number | null;
  hrv: number | null;
  soreness: number | null;
  energy: number | null;
  readinessScore: number;
  readinessLevel: ReadinessLevel;
  recommendation: string;
  notes: string | null;
  source: DataSource;
}

export interface GoalDto {
  id: string;
  type: GoalType;
  title: string;
  targetValue: number | null;
  targetUnit: string | null;
  direction: GoalDirection;
  startValue: number | null;
  currentValue: number | null;
  progressPercent: number | null;
  startDate: string;
  targetDate: string | null;
  status: GoalStatus;
  relatedExerciseName: string | null;
  notes: string | null;
  achievedAt: string | null;
}

export interface AchievementDto {
  id: string;
  type: AchievementType;
  title: string;
  description: string | null;
  value: number | null;
  achievedAt: string;
}

export interface InsightDto {
  id: string;
  headline: string;
  detail: string;
  trend: "up" | "down" | "flat";
  metrics: {
    current: number;
    previous: number;
    unit: string;
    windowDays: number;
  };
  generatedAt: string;
}

export interface TimelineEntryDto {
  date: string;
  kind: TimelineEntryKind;
  id: string;
  title: string;
  subtitle: string | null;
  refId: string;
}

export interface DashboardTodayDto {
  readiness: RecoveryRecordDto | null;
  weightSummary: {
    latestWeightKg: number | null;
    movingAverageKg: number | null;
    goalWeightKg: number | null;
    rateOfChangeKgPerWeek: number | null;
  };
  nutritionSummary: NutritionSummaryDto;
  trainingLoadSummary: {
    weeklyLoad: number;
    acwr: number | null;
    sessionsThisWeek: number;
  };
  timelineToday: TimelineEntryDto[];
  goalsStrip: GoalDto[];
  gamification: {
    loggingStreakDays: number;
    trainingStreakDays: number;
    weeklyCompletionScore: number;
    recentAchievements: AchievementDto[];
  };
  topInsights: InsightDto[];
}
