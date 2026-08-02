package com.sean.supermarketmealplanner.shared.infrastructure.web;

import com.sean.supermarketmealplanner.catalog.application.ProductNotFoundException;
import com.sean.supermarketmealplanner.catalog.application.InvalidFilterException;
import com.sean.supermarketmealplanner.mealtemplate.application.InvalidMealTemplateFilterException;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateNotFoundException;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateValidationException;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanGenerationException;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingException;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanNotFoundException;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanValidationException;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.sean.supermarketmealplanner.nutrition.application.NutritionException;
import com.sean.supermarketmealplanner.identity.application.IdentityException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.sean.supermarketmealplanner.catalogsync.application.CatalogSyncException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NutritionException.class)
    ResponseEntity<ProblemDetail> nutrition(NutritionException exception, HttpServletRequest request) {
        var problem=createProblem(exception.status(),"Nutrition enrichment failed",exception.getMessage(),request);
        problem.setProperty("code",exception.code());problem.setProperty("errorCode",exception.code());
        return ResponseEntity.status(exception.status()).body(problem);
    }
    @ExceptionHandler(CatalogSyncException.class)
    ResponseEntity<ProblemDetail> catalogSync(CatalogSyncException exception, HttpServletRequest request) {
        var problem=createProblem(exception.status(),"Catalog synchronization failed",exception.getMessage(),request);
        problem.setProperty("code",exception.code()); problem.setProperty("errorCode",exception.code());
        return ResponseEntity.status(exception.status()).body(problem);
    }
    @ExceptionHandler(IdentityException.class)
    ResponseEntity<ProblemDetail> identity(IdentityException exception) {
        var problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle(exception.getMessage());
        problem.setProperty("code", exception.getCode());
        return ResponseEntity.status(exception.getStatus())
                .cacheControl(org.springframework.http.CacheControl.noStore()).body(problem);
    }

    @ExceptionHandler({
            ProductNotFoundException.class,
            MealTemplateNotFoundException.class,
            MealPlanNotFoundException.class
    })
    public ProblemDetail handleNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            InvalidFilterException.class,
            InvalidMealTemplateFilterException.class,
            MealTemplateValidationException.class,
            MealPlanValidationException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ProblemDetail handleBadRequest(Exception exception, HttpServletRequest request) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MealPlanGenerationException.class)
    public ProblemDetail handleGenerationImpossible(
            MealPlanGenerationException exception,
            HttpServletRequest request
    ) {
        var problem = createProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Meal plan generation is impossible",
                exception.getMessage(),
                request
        );
        problem.setProperty("candidateCounts", exception.getCandidateCounts());
        problem.setProperty("rejectedByReason", exception.getRejectedByReason());
        problem.setProperty("conflictingConstraints", exception.getConflictingConstraints());
        problem.setProperty("suggestions", exception.getSuggestions());
        problem.setProperty("errorCode", "MEAL_PLAN_GENERATION_IMPOSSIBLE");
        return problem;
    }

    @ExceptionHandler(ShoppingListException.class)
    public ProblemDetail handleShoppingList(
            ShoppingListException exception,
            HttpServletRequest request
    ) {
        var status = HttpStatus.valueOf(exception.status());
        var problem = createProblem(
                status,
                status == HttpStatus.NOT_FOUND
                        ? "Shopping list resource not found"
                        : "Shopping list request failed",
                exception.getMessage(),
                request
        );
        problem.setProperty("errorCode", exception.errorCode());
        if (exception.productId() != null) {
            problem.setProperty("productId", exception.productId());
            problem.setProperty("productName", exception.productName());
            problem.setProperty("unitsDetected", exception.unitsDetected());
            problem.setProperty("expectedMeasurementType", exception.expectedMeasurementType());
        }
        return problem;
    }

    @ExceptionHandler(MealPlanEditingException.class)
    public ProblemDetail handleMealPlanEditing(
            MealPlanEditingException exception,
            HttpServletRequest request
    ) {
        var status = HttpStatus.valueOf(exception.status());
        var problem = createProblem(
                status,
                switch (status) {
                    case NOT_FOUND -> "Meal-plan editing resource not found";
                    case CONFLICT -> "Meal-plan editing conflict";
                    case UNPROCESSABLE_ENTITY -> "Meal-plan editing rule violation";
                    default -> "Invalid meal-plan editing request";
                },
                exception.getMessage(),
                request
        );
        problem.setProperty("errorCode", exception.errorCode());
        return problem;
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", status.name());
        return problem;
    }
}
