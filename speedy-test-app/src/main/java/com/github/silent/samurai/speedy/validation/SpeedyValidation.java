package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.annotations.SpeedyValidator;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Supplier;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.springframework.stereotype.Component;

@Component
public class SpeedyValidation implements ISpeedyCustomValidation {

    @SpeedyValidator(value = Category.class, requests = SpeedyRequestType.CREATE)
    public boolean validateCategory(SpeedyEntity category) throws Exception {
        EntityMetadata entityMetadata = category.getMetadata();
        FieldMetadata name = entityMetadata.field("name");
        String nameValue = category.get(name).asText();
        return nameValue.length() > 0;
    }


    @SpeedyValidator(Supplier.class)
    public boolean validateCustomer(SpeedyEntity supplier) throws Exception {
        return true;
    }

}
