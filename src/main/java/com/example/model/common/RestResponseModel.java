package com.example.model.common;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * REST API çağrıları için kullanılan ortak yanıt modeli.
 * Tüm REST çağrılarının yanıtları bu model üzerinden döner.
 */
@Data
@Builder
public class RestResponseModel<T> {
    
    /**
     * HTTP durum kodu
     */
    private HttpStatus status;
    
    /**
     * Yanıt gövdesi
     * İstek başarılı ise dolu, başarısız ise null olabilir
     */
    private T body;
    
    /**
     * Yanıt header'ları
     */
    private Map<String, String> headers;
    
    /**
     * İstek başarılı mı?
     */
    @Builder.Default
    private boolean success = true;
    
    /**
     * Hata mesajı
     * İstek başarısız ise dolu, başarılı ise null
     */
    private String errorMessage;
    
    /**
     * Hata detayı
     * İstek başarısız ise dolu, başarılı ise null
     */
    private Throwable error;
    
    /**
     * İstek süresi (milisaniye)
     */
    private long requestDuration;
    
    /**
     * Başarılı bir yanıt oluşturur
     */
    public static <T> RestResponseModel<T> success(HttpStatus status, T body, Map<String, String> headers, long duration) {
        return RestResponseModel.<T>builder()
                .status(status)
                .body(body)
                .headers(headers)
                .success(true)
                .requestDuration(duration)
                .build();
    }
    
    /**
     * Başarısız bir yanıt oluşturur
     */
    public static <T> RestResponseModel<T> error(HttpStatus status, String errorMessage, Throwable error, long duration) {
        return RestResponseModel.<T>builder()
                .status(status)
                .success(false)
                .errorMessage(errorMessage)
                .error(error)
                .requestDuration(duration)
                .build();
    }
} 