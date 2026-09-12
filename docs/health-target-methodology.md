# Health target methodology

Last evidence review: 2026-09-11

FitBuddy calculates all adult calorie and macronutrient candidates deterministically on-device. When repeated body and nutrition data are sufficient, the app may generate one bounded trend-adjusted alternative. A configured AI provider can select only between those exact local candidates and add a short regional coaching note; it cannot supply or alter target numbers. Without AI, the conservative local default is used.

## What the app estimates

There is no scientifically valid universal “ideal weight.” BMI is a population screening tool, not a diagnosis or an individualized prescription. FitBuddy therefore shows the adult BMI 18.5–24.9 weight range only as context and uses a single target weight only when it has a defensible role as a goal milestone:

- For weight loss at BMI 25 or above, the first milestone is up to 5% below current weight, without crossing below the upper healthy-BMI boundary.
- For an adult below BMI 18.5 whose goal is gain, the lower healthy-BMI boundary is shown as a milestone and professional guidance is encouraged.
- For healthy-range muscle gain or recomposition, FitBuddy does not invent a scale-weight target. Users may set one manually.
- Targets are rounded to 0.5 kg to avoid false 0.1 kg precision.

The up-to-5% milestone reflects evidence that modest sustained loss can improve health markers. It is a first checkpoint, not a claim that everyone should reach a particular BMI.

## Energy calculation

1. Resting energy is estimated with Mifflin–St Jeor using age, height, weight, and sex. If sex is not provided, FitBuddy transparently uses the midpoint of the two equation constants rather than silently assuming female or male.
2. Resting energy is multiplied by the selected full-day activity factor: 1.2, 1.375, 1.55, 1.725, or 1.9.
3. Goal adjustment:
   - loss: 15% deficit, bounded to 250–500 kcal/day;
   - recomposition: estimated maintenance;
   - muscle gain: 8% surplus, bounded to 150–300 kcal/day.
4. Unsupervised targets are not set below 1,200 kcal/day and are rounded to 50 kcal.

These are starting estimates, not measured metabolism. The selected activity factor already represents average exercise, so FitBuddy compares food intake directly with the target. Logged exercise remains visible but is not credited back 1:1, avoiding the prior double count and reducing sensitivity to inaccurate burn estimates. Users should reassess from a 2–4 week weight trend.

### Activity-level guidance and workout recommendation

The activity selector describes each factor in terms of typical overall movement, planned workouts, and physical work. When the user explicitly requests a target recommendation, FitBuddy also reviews logged workouts from the current 28-day window. It normalizes workout days, session count, and duration to weekly values, without using estimated calories burned:

- very active: at least 6 workout days, 9 sessions, and 600 minutes per week;
- active: at least 6 workout days or 300 minutes per week;
- moderately active: at least 3 workout days or 150 minutes per week;
- lightly active: at least 1 workout day or 60 minutes per week;
- sedentary: some workout history exists but remains below those thresholds.

The strict very-active threshold is evaluated first, followed by each lower level. The recommendation is applied to the proposed calorie and macro calculation before it is shown, but it is never changed silently: the proposal displays the suggested level and its supporting workout totals. If there are no logged workouts in the 28-day window, FitBuddy makes no activity recommendation and preserves the selected level.

This is a conservative workout-history suggestion, not a complete measurement of total energy expenditure. It cannot observe a physical job, walking and other daily movement, workout intensity, or unlogged exercise. Users should keep another level when it better represents their typical life, then reassess targets against their 2–4 week weight trend.

### Repeated body-trend personalization

FitBuddy never adjusts a target from one smart-scale reading. It reviews up to 42 recent calendar days, collapses multiple readings on the same day to their median, and uses a robust Theil–Sen slope so isolated values have less influence. Smart-scale readings and optional composition fields are not required: no readings or an insufficient history always retain the normal deterministic formula plan, while consistent weight-only history can still support cautious feedback. A numeric trend is considered usable only when all of these local quality gates pass:

- at least 4 measurement days spanning at least 14 days;
- the newest reading is no more than 10 days old;
- no gap between measurement days exceeds 14 days;
- weight residual variation remains within a small weight-relative tolerance.

Body-fat and muscle-mass directions are included only as supporting evidence when each metric has at least 3 valid readings spanning 14 days, its newest value is no more than 10 days old, no gap exceeds 14 days, and residual variation is bounded. Consumer bioimpedance changes with hydration, meals, exercise, temperature, and device algorithms, so composition does not directly calculate calories and is never treated as a diagnosis.

A calorie adjustment also requires food logs on at least 21 of the previous 28 days, with average logged intake within 10% or 150 kcal of the current target. This reduces the risk of changing the target when the apparent result is more likely explained by incomplete adherence data. When coverage is sufficient, the app may generate one locally calculated step of no more than 100 kcal/day:

- loss: reduce by 100 kcal when weight is flat or increasing (at least −0.10%/week), or increase by 100 kcal when loss exceeds 1.00%/week;
- muscle gain: increase by 100 kcal when gain is no more than 0.05%/week, or reduce by 100 kcal when gain reaches 0.50%/week;
- recomposition: increase by 100 kcal when loss reaches 0.50%/week, or reduce by 100 kcal when gain reaches 0.50%/week.

These thresholds are conservative FitBuddy feedback rules, not clinical diagnoses or claims that a particular weekly rate is universally optimal. Every request reconstructs the unadjusted formula baseline before creating a candidate, so repeated requests cannot stack the same feedback step. The adjusted calories are still subject to the 1,200 kcal floor, and macros are recalculated within the same adult distribution ranges.

The app creates a formula candidate and, only when the gates support it, a trend-adjusted candidate. If AI is configured, it receives aggregated trend evidence—not authority to generate numbers—and may select only one exact candidate ID. It is instructed to retain the formula candidate when repeated body-composition trends credibly suggest favorable recomposition despite scale weight. Unknown IDs, malformed output, or provider failure fall back to the deterministic local default. Without AI, the trend-adjusted candidate is the conservative default when available. The proposal shows the sample count, span, trend, food-log coverage, composition support, and calorie step before the user applies it.

Mifflin–St Jeor remains a practical general-adult starting equation, but individual errors can be material. A 2023 athlete meta-analysis found that equation performance varies by population and that athlete-specific or measured resting energy is preferable when available.

## Macronutrients

Protein uses 1.2–1.6 g/kg according to goal and selected activity. At BMI 30 or above, dosing weight is capped at the weight corresponding to BMI 30 to avoid extreme protein targets from total body weight. Protein is then constrained to 10–30% of target energy. Fat is set near 25% of energy and carbohydrate fills the remainder, keeping the plan within the adult Acceptable Macronutrient Distribution Ranges: carbohydrate 45–65%, fat 20–35%, and protein 10–35%.

The app normally keeps previously calculated targets stable unless calculated calories differ by at least 150 kcal or protein differs by at least 20 g. A quality-gated trend recommendation is an explicit exception: it may propose one 100 kcal feedback step after sufficient repeated measurements and food-log coverage. Placeholder, non-positive, energy-inconsistent, or out-of-range macro targets are recalculated instead of being preserved. Medical conditions, pregnancy, breastfeeding, eating-disorder history, elite sport, and prescribed diets require individualized professional advice.

## Safety scope

Automated targets require age 18 or older. Child and adolescent energy and weight assessment requires age- and sex-specific growth data and is outside this calculator. NIDDK likewise limits its dynamic Body Weight Planner to adults and excludes pregnancy and breastfeeding.

## Previous method and correction

Previously, an LLM performed the arithmetic from prompt instructions, weight-loss protein was fixed at 1.0 g/kg, target weight could imply unsupported precision, and the app multiplied resting energy by a full activity factor while also subtracting logged exercise from food intake. The first deterministic method moved arithmetic and safety bounds into Kotlin, raised goal/activity-aware protein, treated BMI only as screening context, and removed exercise double counting. The current method adds robust repeated-reading feedback without restoring free-form AI arithmetic: AI can select only between exact bounded candidates produced and validated on-device.

## Evidence used

- [ADA Standards of Care in Diabetes—2024, obesity and weight management](https://pmc.ncbi.nlm.nih.gov/articles/PMC10725806/) — individualized treatment, clinically meaningful modest loss, and energy-deficit evidence.
- [NIDDK research behind the Body Weight Planner](https://www.niddk.nih.gov/research-funding/at-niddk/labs-branches/laboratory-biological-modeling/integrative-physiology-section/research/body-weight-planner) — dynamic weight change, adult-only scope, and limitations.
- [CDC adult BMI categories and limitations (2024)](https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html) — BMI as a screening measure that must be interpreted with other factors.
- [Comparison of resting metabolic rate equations in adults](https://pubmed.ncbi.nlm.nih.gov/15883556/) — support for Mifflin–St Jeor as a practical general-adult estimate.
- [2023 systematic review and meta-analysis of RMR equations in athletes](https://pmc.ncbi.nlm.nih.gov/articles/PMC10687135/) — population-specific accuracy and substantial individual uncertainty.
- [National Academies macronutrient distribution guidance](https://www.nationalacademies.org/news/report-offers-new-eating-and-physical-activity-targets-to-reduce-chronic-disease-risk) — adult carbohydrate, fat, and protein ranges.
- [International Society of Sports Nutrition position stand on protein and exercise](https://jissn.biomedcentral.com/articles/10.1186/s12970-017-0177-8) — protein needs for healthy exercising adults.

Content was rephrased for compliance with licensing restrictions.
