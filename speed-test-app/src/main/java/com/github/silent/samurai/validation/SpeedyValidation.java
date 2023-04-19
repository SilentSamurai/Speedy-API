package com.github.silent.samurai.validation;

import com.github.silent.samurai.annotations.SpeedyValidator;
import com.github.silent.samurai.entity.Category;
import com.github.silent.samurai.entity.Supplier;
import com.github.silent.samurai.enums.SpeedyRequestType;
import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;
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
