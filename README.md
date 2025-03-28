# Camunda BPMN Integration Project

Bu proje, Camunda BPM platformu ile kurumsal servis entegrasyonlarını yönetmek için geliştirilmiş bir Spring Boot uygulamasıdır.

## Özellikler

- BPMN süreçlerini deploy etme ve yönetme
- Task-API mapping yönetimi
- Dinamik API entegrasyonu
- Süreç izleme ve yönetim

## Teknolojiler

- Spring Boot 2.7.9
- Camunda BPM 7.19.0
- H2 Database
- Java 17

## Postgres İle Db Kurulum

docker run -d --name camunda -p 8080:8080 \
-e SPRING_DATA_SOURCE_URL=jdbc:postgresql://{host}:{port}/postgres \
-e SPRING_DATA_SOURCE_USERNAME={dbUser} \
-e SPRING_DATA_SOURCE_PASSWORD={dbPass} \
-e SPRING_DATA_SOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
-e SPRING_JPA_HIBERNATE_DDL_AUTO=create \
-e DB_DRIVER=org.postgresql.Driver \
-e DB_URL=jdbc:postgresql://{host}/{dbName} \
-e DB_USERNAME={dbUser} \
-e DB_PASSWORD={dbPass} \
-e WAIT_FOR={host}/{port} \
-e WAIT_FOR_TIMEOUT=30 \
camunda/camunda-bpm-platform:latest


## Kurulum

1. Projeyi klonlayın:
```bash
git clone https://github.com/tuncerburak97/camunda-bpmn.git
```

2. Projeyi derleyin:
```bash
mvn clean install
```

3. Uygulamayı çalıştırın:
```bash
mvn spring-boot:run
```

## API Endpoints

### BPMN Process Management

- `POST /api/bpmn/deploy` - BPMN sürecini deploy eder
- `GET /api/bpmn/processes` - Tüm süreçleri listeler
- `GET /api/bpmn/process/{processKey}` - Belirli bir süreci getirir

### Process Execution

- `POST /api/process/start/{processKey}` - Süreci başlatır
- `POST /api/process/task/{taskId}/execute` - Task'ı execute eder
- `GET /api/process/instance/{processInstanceId}/tasks` - Aktif task'ları listeler

### Task API Mapping

- `POST /api/task-mapping` - Task-API mapping oluşturur
- `GET /api/task-mapping/process/{processId}` - Süreç mapping'lerini listeler
- `PUT /api/task-mapping/{id}` - Mapping günceller
- `DELETE /api/task-mapping/{id}` - Mapping siler

## Kullanım

1. BPMN dosyasını deploy edin
2. Task'lar için API mapping'leri oluşturun
3. Süreci başlatın
4. Task'ları execute edin

## Lisans

MIT 