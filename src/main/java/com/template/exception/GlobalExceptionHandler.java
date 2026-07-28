package com.template.exception;

import com.template.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex,
                                          RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        if (ex.getRedirectUrl() != null) {
            return "redirect:" + ex.getRedirectUrl();
        }
        return "redirect:/";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(HttpServletRequest request, org.springframework.ui.Model model) {
        model.addAttribute("message", "You don't have permission to access this page");
        model.addAttribute("requestUrl", request.getRequestURL());
        return "error/403";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(HttpServletRequest request, org.springframework.ui.Model model) {
        model.addAttribute("message", "The page you are looking for does not exist");
        model.addAttribute("requestUrl", request.getRequestURL());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, HttpServletRequest request,
                                org.springframework.ui.Model model) {
        model.addAttribute("message", "An unexpected error occurred");
        model.addAttribute("detail", ex.getMessage());
        model.addAttribute("requestUrl", request.getRequestURL());
        return "error/500";
    }
}
