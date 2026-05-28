package com.github.silent.samurai.speedy.advice;

import com.github.silent.samurai.speedy.annotations.SpeedyControllerAdvice;
import com.github.silent.samurai.speedy.annotations.SpeedyExceptionHandler;
import com.github.silent.samurai.speedy.exceptions.TestBusinessException;
import org.springframework.stereotype.Component;

@Component
@SpeedyControllerAdvice
public class TestControllerAdvice {

    @SpeedyExceptionHandler(value = TestBusinessException.class, status = 422)
    public String handleBusinessError(TestBusinessException ex) {
        return "Business rule violated: " + ex.getMessage();
    }

    @SpeedyExceptionHandler(value = IllegalStateException.class, status = 409)
    public String handleIllegalState() {
        return "Resource conflict detected";
    }

    @SpeedyExceptionHandler(value = IllegalArgumentException.class, status = 400)
    public String handleBadArgument(IllegalArgumentException ex) {
        return "Custom argument handler: " + ex.getMessage();
    }
}
