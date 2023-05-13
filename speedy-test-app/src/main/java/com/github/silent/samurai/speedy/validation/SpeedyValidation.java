package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.annotations.SpeedyValidator;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Supplier;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import org.springframework.stereotype.Component;

@Component
public class SpeedyValidation implements ISpeedyCustomValidation {

    @SpeedyValidator(value = Category.class, requests = SpeedyRequestType.CREATE)
    public boolean validateCategory(Category category) throws Exception {
        return category.getName().length() > 0;
    }


    @SpeedyValidator(Supplier.class)
    public boolean validateCustomer(Supplier customer) throws Exception {
        return true;
    }

}
