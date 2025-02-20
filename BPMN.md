# Sipariş Süreci BPMN Dokümantasyonu

Bu dokümanda, sipariş sürecinin BPMN (Business Process Model and Notation) diyagramı ve akışı detaylı olarak açıklanmaktadır.

## Süreç Özeti

Bu BPMN süreci, bir müşterinin sipariş vermesinden siparişin tamamlanmasına kadar olan tüm adımları kapsar. Süreç üç ana aşamadan oluşur:
1. Kullanıcı Doğrulama
2. Sipariş İşleme
3. Bildirim Gönderme

## BPMN Elemanları ve Görevleri

### 1. Start Event (Başlangıç Olayı)
- **ID**: `start-event`
- **Görevi**: Sürecin başlangıç noktası
- **Tetikleyici**: REST API üzerinden gelen sipariş isteği
- **Çıktı**: Kullanıcı bilgileri ve sipariş detayları

### 2. User Validation Task (Kullanıcı Doğrulama Görevi)
- **ID**: `user-validation-task`
- **Tip**: Service Task
- **Delegate**: `UserValidationDelegate`
- **Girdiler**:
  - userId: Kullanıcı ID
  - email: Kullanıcı e-posta
  - name: Kullanıcı adı
- **Çıktılar**:
  - isUserValid: Kullanıcı geçerli mi? (true/false)

### 3. User Validation Gateway (Kullanıcı Doğrulama Geçidi)
- **ID**: `user-validation-gateway`
- **Tip**: Exclusive Gateway (XOR)
- **Karar Mantığı**:
  - isUserValid == true → Sipariş işlemeye git
  - isUserValid == false → Bildirim göndermeye git
- **Amaç**: Geçersiz kullanıcıları süreçten çıkarmak

### 4. Order Processing Task (Sipariş İşleme Görevi)
- **ID**: `order-processing-task`
- **Tip**: Service Task
- **Delegate**: `OrderProcessingDelegate`
- **Koşul**: Sadece geçerli kullanıcılar için çalışır
- **Girdiler**:
  - userId: Kullanıcı ID
  - productId: Ürün ID
  - amount: Sipariş tutarı
  - quantity: Ürün miktarı
- **Çıktılar**:
  - orderId: Oluşturulan sipariş ID

### 5. Notification Task (Bildirim Görevi)
- **ID**: `notification-task`
- **Tip**: Service Task
- **Delegate**: `NotificationDelegate`
- **Girdiler**:
  - userId: Kullanıcı ID
  - orderId: Sipariş ID (başarılı siparişlerde)
  - isUserValid: Doğrulama sonucu
- **Özellik**: İki farklı durumda çalışır:
  1. Kullanıcı doğrulama başarısız olduğunda
  2. Sipariş başarıyla tamamlandığında

### 6. End Event (Bitiş Olayı)
- **ID**: `end-event`
- **Görevi**: Sürecin sonlandırılması
- **Öncesi**: Bildirim gönderme işlemi

## Akış Yolları (Sequence Flows)

1. **Flow_1**: Start Event → User Validation Task
   - Sürecin başlangıcı
   - Kullanıcı bilgilerini doğrulamaya taşır

2. **Flow_2**: User Validation Task → User Validation Gateway
   - Doğrulama sonucunu gateway'e taşır

3. **Flow_3**: User Validation Gateway → Order Processing Task
   - Koşul: `${isUserValid == true}`
   - Geçerli kullanıcıları sipariş işlemeye yönlendirir

4. **Flow_4**: User Validation Gateway → Notification Task
   - Koşul: `${isUserValid == false}`
   - Geçersiz kullanıcıları bildirime yönlendirir

5. **Flow_5**: Order Processing Task → Notification Task
   - Başarılı siparişleri bildirime yönlendirir

6. **Flow_6**: Notification Task → End Event
   - Süreci sonlandırır

## Süreç Değişkenleri (Process Variables)

1. **Giriş Değişkenleri**:
   - userId: String
   - email: String
   - name: String
   - productId: String
   - amount: Double
   - quantity: Integer

2. **Süreç İçi Değişkenler**:
   - isUserValid: Boolean
   - orderId: String

## Hata Yönetimi

Her service task kendi içinde hata yönetimi yapar:

1. **User Validation Task**:
   - Servis çağrısı hatası → isUserValid = false

2. **Order Processing Task**:
   - Hata durumunda RuntimeException fırlatır
   - Süreç bu noktada durur ve manuel müdahale gerekir

3. **Notification Task**:
   - Hata durumunda RuntimeException fırlatır
   - Bildirim gönderilemese bile süreç tamamlanır

## Süreç İzleme ve Yönetim

1. **Camunda Cockpit'te Görüntüleme**:
   - Aktif süreçleri listele
   - Süreç detaylarını görüntüle
   - Değişkenleri incele
   - Hata durumlarını kontrol et

2. **Müdahale İmkanları**:
   - Failed job'ları yeniden dene
   - Süreci manuel olarak ilerlet
   - Değişkenleri güncelle
   - Süreci iptal et

## Best Practices

1. **Süreç Başlatma**:
   - Tüm zorunlu değişkenleri kontrol et
   - Geçerli veri tipleri kullan
   - İş kurallarına uygunluğu doğrula

2. **Süreç İzleme**:
   - Düzenli olarak failed job'ları kontrol et
   - Performans metriklerini izle
   - Süreç tamamlanma oranlarını takip et

3. **Hata Yönetimi**:
   - Anlamlı hata mesajları kullan
   - Retry mekanizmalarını yapılandır
   - Kritik hataları bildir 