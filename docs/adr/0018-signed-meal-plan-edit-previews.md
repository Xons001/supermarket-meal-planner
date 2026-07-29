# ADR 0018 — Previews de edición firmados

## Decisión

Confirmar ediciones únicamente con tokens Base64URL HMAC-SHA256 de corta vida.
El secreto es externo, obligatorio y de al menos 32 bytes. El payload vincula
operación, plan, objetivo, versión, selección, seed y hash del resultado.

## Consecuencias

No puede confirmarse un `mealTemplateId` aislado ni reutilizarse un preview de
otra versión. Formato o firma inválidos son 400; expiración u obsolescencia son
409.
