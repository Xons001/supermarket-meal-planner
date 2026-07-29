# ADR 0013 — Cálculo de compra compartido

## Estado

Aceptado.

## Decisión

Generador y lista de compra usan un componente puro común para conversiones,
agregación, envases, costes y desperdicio. La lista aplica conflictos estrictos
y el beam puede representar incompatibilidades como cálculo parcial.

## Consecuencias

Las fórmulas no divergen entre estimación y compra persistida. El componente no
depende de JPA, repositorios ni entidades de lista.
