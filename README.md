# Camunda BPMN Sipariş Süreci

Bu proje, Camunda BPM kullanarak bir sipariş sürecini yönetir. Süreç, kullanıcı doğrulama, sipariş işleme ve bildirim gönderme adımlarını içerir.

## Teknolojiler

- Java 17
- Spring Boot
- Camunda BPM
- H2 Database
- REST API

## Proje Yapısı

```
src/main/java/com/tuncerburak/
├── controller/          # REST API controller'ları
├── delegate/           # Camunda service task delegate'leri
├── model/             # DTO ve request/response modelleri
├── service/           # İş mantığı servisleri
└── config/            # Konfigürasyon sınıfları
```

## Süreç Akışı

1. **API İsteği**
   - Endpoint: POST `/api/process/start`
   - Request body örneği:
   ```json
   {
       "userId": "user123",
       "email": "user@example.com",
       "name": "John Doe",
       "productId": "prod456",
       "amount": 100.50,
       "quantity": 2
   }
   ```

2. **Kullanıcı Doğrulama**
   - `UserValidationDelegate` çalışır
   - Dış servis çağrısı yapılır
   - Sonuç process variable'a kaydedilir

3. **Sipariş İşleme**
   - Kullanıcı geçerliyse `OrderProcessingDelegate` çalışır
   - Sipariş bilgileri dış servise gönderilir
   - Sipariş ID alınır ve process variable'a kaydedilir

4. **Bildirim Gönderme**
   - `NotificationDelegate` çalışır
   - Süreç sonucu kullanıcıya bildirilir

## Servis Entegrasyonları

- User Service: `http://user-service/api/users`
- Order Service: `http://order-service/api/orders`
- Notification Service: `http://notification-service/api/notifications`

## Kurulum ve Çalıştırma

1. Projeyi klonlayın
```bash
git clone [repo-url]
```

2. Maven ile derleyin
```bash
mvn clean install
```

3. Uygulamayı başlatın
```bash
mvn spring-boot:run
```

4. Camunda Cockpit'e erişin
```
http://localhost:8080/camunda/app/
```

## Süreç İzleme

1. **Camunda Cockpit**
   - Süreç instance'larını görüntüleme
   - Süreç değişkenlerini inceleme
   - Hata durumlarını görüntüleme

2. **REST API**
   - Süreç durumu sorgulama:
   ```bash
   GET http://localhost:8080/engine-rest/process-instance/{processInstanceId}
   ```

## Hata Yönetimi

- Her service task'ta try-catch blokları bulunur
- Hatalar loglara kaydedilir
- Kullanıcıya bildirim gönderilir

## Geliştirme Notları

1. **Yeni Service Task Ekleme**
   - Delegate sınıfı oluştur
   - Service sınıfı oluştur
   - BPMN diyagramını güncelle

2. **Dış Servis Entegrasyonu**
   - Service URL'lerini application.yaml'da tanımla
   - RestTemplate kullan
   - Hata yönetimini ekle 