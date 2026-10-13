FROM python:3.11-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY python-common /app/python-common
COPY ai-rag-service /app/ai-rag-service
RUN pip install --no-cache-dir -r /app/ai-rag-service/requirements.txt && pip install --no-cache-dir /app/python-common
WORKDIR /app/ai-rag-service
EXPOSE 8092
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8092"]
