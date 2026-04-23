package ru.dobrovichek.user.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.security.ServiceHeaders;
import ru.dobrovichek.user.application.BadRequestException;

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
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BadRequestException("Cannot resolve current user");
        }

        String userIdHeader = request.getHeader(ServiceHeaders.USER_ID);
        String userRoleHeader = request.getHeader(ServiceHeaders.USER_ROLE);
        if (userIdHeader == null || userIdHeader.isBlank() || userRoleHeader == null || userRoleHeader.isBlank()) {
            throw new BadRequestException("Missing authentication headers");
        }

        try {
            return new CurrentUser(
                    UUID.fromString(userIdHeader),
                    UserRole.valueOf(userRoleHeader.trim().toUpperCase())
            );
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid authentication headers");
        }
    }
}
