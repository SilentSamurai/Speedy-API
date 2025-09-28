package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.annotations.SpeedyValidator;
import com.github.silent.samurai.speedy.enums.SpeedyValidationRequestType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Supplier;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.springframework.stereotype.Component;

@Component
public class SpeedyValidation implements ISpeedyCustomValidation {

    @SpeedyValidator(entity = "Category", requests = SpeedyValidationRequestType.CREATE)
    public boolean validateCategoryCreate(Category category) {
        return category.getName() != null && !category.getName().isEmpty();
    }

    @SpeedyValidator(entity = "Category", requests = SpeedyValidationRequestType.UPDATE)
    public boolean validateCategoryUpdate(Category category) {
        // During update, if name field is supplied, it must not be empty
        return category.getName() == null || !category.getName().isEmpty();
    }

    @SpeedyValidator(entity = "Category", requests = SpeedyValidationRequestType.DELETE)
    public boolean validateCategoryDelete(Category category) {
        return category.getId() != null && !category.getId().isEmpty();
    }


    @SpeedyValidator(entity = "Supplier", requests = SpeedyValidationRequestType.CREATE)
    public boolean validateSupplier(Supplier supplier) {
        return supplier.getName() != null && !supplier.getName().isEmpty()
                && supplier.getPhoneNo() != null && !supplier.getPhoneNo().isEmpty();
    }

    @SpeedyValidator(entity = "Product", requests = SpeedyValidationRequestType.CREATE)
    public boolean preventInvalidProductName(com.github.silent.samurai.speedy.entity.Product product) {
        return product.getName() == null || !"invalid-trigger".equalsIgnoreCase(product.getName());
    }

}
