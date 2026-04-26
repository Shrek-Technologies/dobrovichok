package ru.dobrovichek.request.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.request.dto.CurrentUser;
import ru.dobrovichek.request.exception.BadRequestException;

import java.util.UUID;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();
            String roleClaim = jwt.getClaimAsString("role");
            if (roleClaim == null || roleClaim.isBlank()) {
                throw new BadRequestException("Invalid JWT: missing role");
            }
            try {
                return new CurrentUser(
                        UUID.fromString(jwt.getSubject()),
                        UserRole.valueOf(roleClaim.trim().toUpperCase())
                );
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Invalid JWT subject or role");
            }
        }

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BadRequestException("Cannot resolve current user");
        }
        throw new BadRequestException("Missing Bearer authentication");
    }
}
