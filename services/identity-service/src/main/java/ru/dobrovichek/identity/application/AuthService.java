package ru.dobrovichek.identity.application;

import org.springframework.stereotype.Service;
import ru.dobrovichek.contracts.PersonNameFormat;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.identity.api.AuthResponse;
import ru.dobrovichek.identity.api.LoginRequest;
import ru.dobrovichek.identity.api.RegisterRequest;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthService {

    private final ConcurrentMap<String, RegisteredUser> usersByPhone = new ConcurrentHashMap<>();

    public AuthResponse register(RegisterRequest request) {
        UserRole role = request.role() == null ? UserRole.WARD : request.role();
        String patronymic = blankToNull(request.patronymic());
        RegisteredUser user = new RegisteredUser(
                UUID.randomUUID(),
                request.firstName().trim(),
                request.lastName().trim(),
                patronymic,
                normalizePhone(request.phone()),
                request.password(),
                role
        );
        RegisteredUser existing = usersByPhone.putIfAbsent(user.phone(), user);
        if (existing != null) {
            throw new AuthConflictException("Пользователь с таким телефоном уже существует");
        }
        return user.toResponse();
    }

    public AuthResponse login(LoginRequest request) {
        RegisteredUser user = usersByPhone.get(normalizePhone(request.phone()));
        if (user == null || !user.password().equals(request.password())) {
            throw new AuthUnauthorizedException("Неверный телефон или пароль");
        }
        return user.toResponse();
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9+]", "");
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private record RegisteredUser(
            UUID userId,
            String firstName,
            String lastName,
            String patronymic,
            String phone,
            String password,
            UserRole role
    ) {
        AuthResponse toResponse() {
            return new AuthResponse(
                    userId,
                    firstName,
                    lastName,
                    patronymic,
                    PersonNameFormat.fullFormal(firstName, patronymic, lastName),
                    phone,
                    role
            );
        }
    }
}
