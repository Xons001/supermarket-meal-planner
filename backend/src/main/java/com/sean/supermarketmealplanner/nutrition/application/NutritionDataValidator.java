package com.sean.supermarketmealplanner.nutrition.application;

import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionEntity.NutritionValues;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class NutritionDataValidator {
    public List<String> validate(NutritionValues values){var warnings=new ArrayList<String>();
        check("calories",values.calories(),1000,warnings);check("protein",values.protein(),100,warnings);
        check("carbohydrates",values.carbohydrates(),100,warnings);check("fat",values.fat(),100,warnings);
        check("fiber",values.fiber(),100,warnings);check("sugars",values.sugars(),100,warnings);
        check("salt",values.salt(),100,warnings);check("saturatedFat",values.saturatedFat(),100,warnings);
        if(values.basis()==null||!Set.of("PER_100_GRAMS","PER_100_MILLILITERS","PER_UNIT").contains(values.basis()))
            throw new NutritionException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,"NUTRITION_BASIS_INCOMPATIBLE","La base nutricional no es compatible");
        return warnings;}
    private void check(String name,BigDecimal value,int maximum,List<String>warnings){if(value==null)return;
        if(value.signum()<0)throw new NutritionException(org.springframework.http.HttpStatus.BAD_REQUEST,"NUTRITION_DATA_INVALID",name+" no puede ser negativo");
        if(value.compareTo(BigDecimal.valueOf(maximum))>0)warnings.add(name+" supera el límite razonable por base nutricional");}
}
