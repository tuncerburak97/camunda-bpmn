1. İlk İstek (Controller):

// POST /api/process/start endpoint'ine istek gelir
@PostMapping("/start")
public ResponseEntity<String> startProcess(@RequestBody StartProcessRequest request) {
// Değişkenler hazırlanır
Map<String, Object> variables = new HashMap<>();
variables.put("userId", request.getUserId());
// ... diğer değişkenler

    // Camunda süreci başlatılır
    String processInstanceId = runtimeService.startProcessInstanceByKey("order-process", variables);
    return ResponseEntity.ok(processInstanceId);
}

2. User Validation Task

@Component
public class UserValidationDelegate implements JavaDelegate {
private final UserService userService;

    @Override
    public void execute(DelegateExecution execution) {
        // Process variables'dan değerleri al
        String userId = (String) execution.getVariable("userId");
        String email = (String) execution.getVariable("email");
        String name = (String) execution.getVariable("name");

        // UserService'i çağır
        UserRequest userRequest = new UserRequest();
        userRequest.setUserId(userId);
        userRequest.setEmail(email);
        userRequest.setName(name);

        // Dış servise istek at
        boolean isValid = userService.validateUser(userRequest);
        
        // Sonucu process'e kaydet
        execution.setVariable("isUserValid", isValid);
    }
}

3. Gateway Kontrolü:


Camunda Engine, isUserValid değişkenine göre yolu belirler
isUserValid == true ise Order Processing'e gider
isUserValid == false ise Notification'a gider

4. Order Processing Task (Eğer kullanıcı geçerliyse):


@Component
public class OrderProcessingDelegate implements JavaDelegate {
private final OrderService orderService;

    @Override
    public void execute(DelegateExecution execution) {
        // Process variables'dan değerleri al
        String userId = (String) execution.getVariable("userId");
        String productId = (String) execution.getVariable("productId");
        BigDecimal amount = new BigDecimal(execution.getVariable("amount").toString());
        int quantity = (Integer) execution.getVariable("quantity");

        // OrderService'i çağır
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setProductId(productId);
        orderRequest.setAmount(amount);
        orderRequest.setQuantity(quantity);

        // Dış servise istek at
        String orderId = orderService.processOrder(orderRequest);
        
        // Sonucu process'e kaydet
        execution.setVariable("orderId", orderId);
    }
}

5. Notification Task:


@Component
public class NotificationDelegate implements JavaDelegate {
private final NotificationService notificationService;

    @Override
    public void execute(DelegateExecution execution) {
        // Process variables'dan değerleri al
        String userId = (String) execution.getVariable("userId");
        String orderId = (String) execution.getVariable("orderId");
        boolean isUserValid = (Boolean) execution.getVariable("isUserValid");

        // NotificationService'i çağır
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setUserId(userId);
        notificationRequest.setOrderId(orderId);
        notificationRequest.setNotificationType("ORDER_STATUS");

        // Mesajı hazırla
        String message = isUserValid ? 
            "Siparişiniz başarıyla oluşturuldu: " + orderId :
            "Siparişiniz doğrulama hatası nedeniyle oluşturulamadı.";
        notificationRequest.setMessage(message);

        // Dış servise istek at
        notificationService.sendNotification(notificationRequest);
    }
}

Özet


Akış Sırası:

	API'ye POST isteği gelir
	Controller süreci başlatır
	Camunda Engine sırasıyla:
		User Validation Task'ı çalıştırır
		Gateway'de sonuca göre yön belirler
		Order Processing Task'ı çalıştırır (eğer kullanıcı geçerliyse)
		Notification Task'ı çalıştırır
		Süreci sonlandırır
	Önemli Noktalar:
		Her delegate sınıfı Spring tarafından yönetilir (@Component)
		Her service sınıfı da Spring tarafından yönetilir (@Service)
		Değişkenler process instance boyunca taşınır
		Her task kendi iş mantığını bağımsız olarak yürütür
		Hata durumları try-catch blokları ile yönetilir
		Bu şekilde, tek bir API isteği ile tüm süreç otomatik olarak çalışır ve her adım kendi görevini yerine getirir. Camunda Engine, BPMN diyagramındaki akışa göre hangi task'ın ne zaman çalışacağını yönetir.
