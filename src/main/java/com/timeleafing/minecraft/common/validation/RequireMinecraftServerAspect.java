package com.timeleafing.minecraft.common.validation;

import com.timeleafing.minecraft.common.validation.annotation.RequireServerId;
import com.timeleafing.minecraft.config.property.MinecraftProperties;
import com.timeleafing.minecraft.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequireMinecraftServerAspect {

    private final MinecraftProperties props;

    /**
     * 拦截：只要方法任意参数上出现 @RequireMinecraftServer 就会进入
     */
    @Around("execution(* *(.., @com.timeleafing.minecraft.common.validation.annotation.RequireServerId (*), ..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Object[] args = pjp.getArgs();
        Annotation[][] paramAnnotations = sig.getMethod().getParameterAnnotations();

        String serverId = null;

        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation a : paramAnnotations[i]) {
                if (a.annotationType() == RequireServerId.class) {
                    Object v = args[i];
                    serverId = (v == null) ? null : String.valueOf(v);
                    break;
                }
            }
            if (serverId != null || hasRequireAnn(paramAnnotations[i])) {
                // 找到被标注的参数就退出（哪怕它是 null，也要继续校验抛错）
                break;
            }
        }
        // 没有标注参数就直接放行（理论上不会进来，但做个兜底）
        if (serverId == null && !hasAnyRequire(paramAnnotations)) {
            return pjp.proceed();
        }
        // 1) 空校验
        if (serverId == null || serverId.isBlank()) {
            throw new BizException("serverId is blank");
        }
        // 2) 存在性校验（是否在 application.yaml 配置中）
        try {
            var config = props.byId(serverId);
            if (config == null) {
                throw new BizException("Unknown serverId: " + serverId);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Unknown serverId: " + serverId);
        }
        return pjp.proceed();
    }

    private static boolean hasRequireAnn(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a.annotationType() == RequireServerId.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyRequire(Annotation[][] all) {
        for (Annotation[] annotations : all) {
            if (hasRequireAnn(annotations)) {
                return true;
            }
        }
        return false;
    }
}
