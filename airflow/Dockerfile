FROM apache/airflow:3.3.0-python3.12 AS runtime

ARG APP_VERSION=0.11.0-dev
ARG VCS_REF=unknown
ARG BUILD_TIME=unknown
LABEL org.opencontainers.image.title="Supermarket Meal Planner Airflow" \
      org.opencontainers.image.version="$APP_VERSION" \
      org.opencontainers.image.revision="$VCS_REF" \
      org.opencontainers.image.created="$BUILD_TIME" \
      org.opencontainers.image.licenses="MIT"

COPY airflow/requirements-runtime.txt /requirements-runtime.txt
RUN pip install --no-cache-dir -r /requirements-runtime.txt
ENV APP_VERSION=$APP_VERSION APP_COMMIT=$VCS_REF APP_BUILD_TIME=$BUILD_TIME
USER airflow

FROM runtime AS test
COPY airflow/requirements-test.txt /requirements-test.txt
RUN pip install --no-cache-dir -r /requirements-test.txt
