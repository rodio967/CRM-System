package com.shift.crm.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException seller(Long id) {
        return new ResourceNotFoundException("Продавец с id=" + id + " не найден");
    }

    public static ResourceNotFoundException transaction(Long id) {
        return new ResourceNotFoundException("Транзакция с id=" + id + " не найдена");
    }
}
