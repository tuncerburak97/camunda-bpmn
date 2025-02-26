package com.example.model.common;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API çağrıları için kullanılan ortak istek modeli.
 * Tüm REST çağrıları bu model üzerinden yapılmalıdır.
 */
@Data
@Builder
public class RestRequestModel<T> {
    
    /**
     * API endpoint URL'i
     */
    private String url;
    
    /**
     * HTTP metodu (GET, POST, PUT, DELETE, vb.)
     */
    private HttpMethod method;
    
    /**
     * İstek gövdesi (body)
     * Null olabilir (örn. GET istekleri için)
     */
    private Object body;
    
    /**
     * İstek header'ları
     * Null olabilir, varsayılan header'lar kullanılır
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    
    /**
     * URL query parametreleri
     * Null olabilir
     */
    private MultiValueMap<String, String> queryParams;
    
    /**
     * Yanıt için beklenen sınıf tipi
     * Null ise String olarak döner
     */
    private Class<T> responseType;
    
    /**
     * İstek zaman aşımı (milisaniye)
     * 0 veya negatif değer varsayılan zaman aşımını kullanır
     */
    @Builder.Default
    private int timeout = 0;
    
    /**
     * Hata durumunda yeniden deneme sayısı
     */
    @Builder.Default
    private int maxRetries = 0;
    
    /**
     * Yeniden denemeler arasındaki bekleme süresi (milisaniye)
     */
    @Builder.Default
    private int retryDelay = 1000;
    
    /**
     * Hata durumunda başarısız olup olmayacağı
     * true ise hata fırlatır, false ise null döner
     */
    @Builder.Default
    private boolean failOnError = true;
} 