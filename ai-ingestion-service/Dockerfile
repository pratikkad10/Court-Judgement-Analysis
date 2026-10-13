FROM python:3.11-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY python-common /app/python-common
COPY ai-ingestion-service /app/ai-ingestion-service
RUN pip install --no-cache-dir -r /app/ai-ingestion-service/requirements.txt && pip install --no-cache-dir /app/python-common
WORKDIR /app/ai-ingestion-service
EXPOSE 8091
CMD ["sh","-c","alembic -c alembic.ini upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8091"]
