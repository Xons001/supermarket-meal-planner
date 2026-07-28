package com.sean.supermarketmealplanner.shared.infrastructure.web;

import com.sean.supermarketmealplanner.catalog.application.ProductNotFoundException;
import com.sean.supermarketmealplanner.catalog.application.InvalidFilterException;
import com.sean.supermarketmealplanner.mealtemplate.application.InvalidMealTemplateFilterException;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateNotFoundException;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateValidationException;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanGenerationException;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanNotFoundException;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

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
