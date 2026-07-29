package com.template.service.audit;

import com.template.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(com.template.service.audit.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        String action = auditable.action();
        String entityType = auditable.entityType();
        String descriptionSpel = auditable.description();

        Object result = joinPoint.proceed();

        String description = resolveSpel(descriptionSpel, signature, joinPoint.getArgs());

        Object[] args = joinPoint.getArgs();
        Long entityId = null;
        if (result != null && result instanceof Number num) {
            entityId = num.longValue();
        } else if (args.length > 0 && args[0] != null && args[0] instanceof Number num) {
            entityId = num.longValue();
        }

        auditService.record(action, entityType, entityId, description);

        return result;
    }

    private String resolveSpel(String expression, MethodSignature signature, Object[] args) {
        if (expression == null || expression.isEmpty() || !expression.contains("#")) {
            return expression;
        }
        try {
            String[] paramNames = signature.getParameterNames();
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    if (args[i] != null) {
                        context.setVariable(paramNames[i], args[i]);
                    }
                }
            }
            return parser.parseExpression(expression).getValue(context, String.class);
        } catch (Exception e) {
            return expression;
        }
    }
}
