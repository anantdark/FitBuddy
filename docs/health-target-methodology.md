# Health target methodology

Last evidence review: 2026-09-11

FitBuddy calculates adult calorie and macronutrient targets deterministically on-device. An AI provider may rephrase the explanation for the user's region, but the app rejects an AI response unless every goal and numeric field exactly matches the local calculation.

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

Mifflin–St Jeor remains a practical general-adult starting equation, but individual errors can be material. A 2023 athlete meta-analysis found that equation performance varies by population and that athlete-specific or measured resting energy is preferable when available.

## Macronutrients

Protein uses 1.2–1.6 g/kg according to goal and selected activity. At BMI 30 or above, dosing weight is capped at the weight corresponding to BMI 30 to avoid extreme protein targets from total body weight. Protein is then constrained to 10–30% of target energy. Fat is set near 25% of energy and carbohydrate fills the remainder, keeping the plan within the adult Acceptable Macronutrient Distribution Ranges: carbohydrate 45–65%, fat 20–35%, and protein 10–35%.

The app keeps previously calculated targets stable unless calculated calories differ by at least 150 kcal or protein differs by at least 20 g. Placeholder, non-positive, energy-inconsistent, or out-of-range macro targets are recalculated instead of being preserved. Medical conditions, pregnancy, breastfeeding, eating-disorder history, elite sport, and prescribed diets require individualized professional advice.

## Safety scope

Automated targets require age 18 or older. Child and adolescent energy and weight assessment requires age- and sex-specific growth data and is outside this calculator. NIDDK likewise limits its dynamic Body Weight Planner to adults and excludes pregnancy and breastfeeding.

## Previous method and correction

Previously, an LLM performed the arithmetic from prompt instructions, weight-loss protein was fixed at 1.0 g/kg, target weight could imply unsupported precision, and the app multiplied resting energy by a full activity factor while also subtracting logged exercise from food intake. The new method moves all arithmetic and safety bounds into Kotlin, raises goal/activity-aware protein into the evidence-supported adult range, treats BMI only as screening context, validates any AI echo, and removes exercise double counting.

## Evidence used

- [ADA Standards of Care in Diabetes—2024, obesity and weight management](https://pmc.ncbi.nlm.nih.gov/articles/PMC10725806/) — individualized treatment, clinically meaningful modest loss, and energy-deficit evidence.
- [NIDDK research behind the Body Weight Planner](https://www.niddk.nih.gov/research-funding/at-niddk/labs-branches/laboratory-biological-modeling/integrative-physiology-section/research/body-weight-planner) — dynamic weight change, adult-only scope, and limitations.
- [CDC adult BMI categories and limitations (2024)](https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html) — BMI as a screening measure that must be interpreted with other factors.
- [Comparison of resting metabolic rate equations in adults](https://pubmed.ncbi.nlm.nih.gov/15883556/) — support for Mifflin–St Jeor as a practical general-adult estimate.
- [2023 systematic review and meta-analysis of RMR equations in athletes](https://pmc.ncbi.nlm.nih.gov/articles/PMC10687135/) — population-specific accuracy and substantial individual uncertainty.
- [National Academies macronutrient distribution guidance](https://www.nationalacademies.org/news/report-offers-new-eating-and-physical-activity-targets-to-reduce-chronic-disease-risk) — adult carbohydrate, fat, and protein ranges.
- [International Society of Sports Nutrition position stand on protein and exercise](https://jissn.biomedcentral.com/articles/10.1186/s12970-017-0177-8) — protein needs for healthy exercising adults.

Content was rephrased for compliance with licensing restrictions.
