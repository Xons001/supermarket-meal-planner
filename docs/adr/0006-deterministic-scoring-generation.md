# ADR 0006 — Generación determinista basada en scoring

- Estado: aceptado
- Fecha: 2026-07-28

## Contexto

La primera generación semanal debe ser explicable, rápida y reproducible, sin
introducir todavía un solver externo.

## Decisión

Se define `MealPlanGenerationStrategy` y se implementa
`ScoringMealPlanGenerationStrategy` con beam search acotado. Los pesos y límites
son configuración tipada. La seed solo resuelve empates estables.

La futura estrategia `OrToolsMealPlanGenerationStrategy` podrá implementar el
mismo puerto sin cambiar el contrato REST ni el modelo principal.

## Consecuencias

El algoritmo termina en tiempo acotado y cada factor puede explicarse y
probarse. No garantiza óptimo global, y el catálogo pequeño limita lo cerca que
puede quedar de objetivos altos.
