package br.edu.ufersa.pw.focustask;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ServiceArchitectureTests {
    private static final String BASE = "br.edu.ufersa.pw.focustask.features.";
    private static final List<String> FEATURES = List.of("user.User", "project.Project", "task.Task", "focusSession.FocusSession");

    @Test
    void controllersOnlyDependOnLocalApplicationServicesAndApplicationEntriesAreTransactional() throws Exception {
        for (String feature : FEATURES) {
            Class<?> app = Class.forName(BASE + feature + "ApplicationService");
            Class<?> controller = Class.forName(BASE + feature + "Controller");
            assertFalse(Modifier.isPublic(app.getModifiers()));
            assertArrayEquals(new Class<?>[]{app}, controller.getDeclaredConstructors()[0].getParameterTypes());
            assertEquals(1, controller.getDeclaredFields().length);
            assertFalse(controller.isAnnotationPresent(Transactional.class));
            for (var method : app.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                Transactional tx = method.getAnnotation(Transactional.class);
                assertNotNull(tx, method.toString());
                assertEquals(method.getName().startsWith("get"), tx.readOnly(), method.toString());
            }
        }
        for (String feature : List.of("user.User", "task.Task", "focusSession.FocusSession")) {
            Class<?> domain = Class.forName(BASE + feature + "Service");
            assertFalse(domain.isAnnotationPresent(Transactional.class));
            for (var method : domain.getDeclaredMethods()) {
                assertFalse(method.isAnnotationPresent(Transactional.class));
                for (Class<?> input : method.getParameterTypes()) assertFalse(input.getPackageName().endsWith(".dto"));
            }
        }
    }

    @Test
    void applicationDomainAndExceptionsHaveNoWebDependenciesOrHandlers() throws Exception {
        Path root = Path.of("src/main/java/br/edu/ufersa/pw/focustask");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertEquals(file.equals(root.resolve("shared/exception/GlobalExceptionHandler.java")),
                        source.contains("@RestControllerAdvice"), file.toString());
                String name = file.getFileName().toString();
                if (!(name.endsWith("Service.java") || name.endsWith("Exception.java") || name.endsWith("InternalApiImpl.java"))) continue;
                for (String forbidden : List.of("org.springframework.http", "org.springframework.web", "jakarta.servlet", "@ResponseStatus", "ProblemDetail")) {
                    assertFalse(source.contains(forbidden), file + ": " + forbidden);
                }
            }
        }
    }

    @Test
    void featureBeansWireWithoutCyclesEvenWithoutADatabase() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            for (String feature : FEATURES) {
                Class<?> repository = Class.forName(BASE + feature + "Repository");
                context.getBeanFactory().registerSingleton(repository.getSimpleName(), mock(repository));
            }
            context.getBeanFactory().registerSingleton("passwordEncoder", mock(org.springframework.security.crypto.password.PasswordEncoder.class));
            context.getBeanFactory().registerSingleton("authenticationManager", mock(org.springframework.security.authentication.AuthenticationManager.class));
            context.getBeanFactory().registerSingleton("tokenService", mock(br.edu.ufersa.pw.focustask.shared.security.TokenService.class));
            context.scan("br.edu.ufersa.pw.focustask.features");
            context.refresh();
            for (String feature : FEATURES) {
                assertNotNull(context.getBean(Class.forName(BASE + feature + "Controller")));
                assertNotNull(context.getBean(Class.forName(BASE + feature + "ApplicationService")));
                assertNotNull(context.getBean(Class.forName(BASE + feature + "InternalApi")));
            }
        }
    }
    @Test
    void securityStaysOutsideDomainAndOrdinaryApplicationServices() throws Exception {
        Path root = Path.of("src/main/java/br/edu/ufersa/pw/focustask/features");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                String name = file.getFileName().toString();
                if (name.equals("AuthApplicationService.java") || name.equals("UserDetailsServiceImpl.java")) continue;
                if (!(name.endsWith("Service.java") || name.endsWith("InternalApiImpl.java")
                        || name.endsWith("Exception.java") || source.contains("@Entity"))) continue;
                assertFalse(source.contains("org.springframework.security"), file.toString());
                assertFalse(source.contains("SecurityContext"), file.toString());
                assertFalse(source.contains("shared.security"), file.toString());
            }
        }
        Class<?> auth = Class.forName(BASE + "user.AuthApplicationService");
        assertNotNull(auth.getDeclaredMethod("register", br.edu.ufersa.pw.focustask.features.user.dto.RegisterRequestDTO.class)
                .getAnnotation(Transactional.class));
    }
}
