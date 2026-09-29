package br.edu.ufersa.pw.focustask;

import br.edu.ufersa.pw.focustask.features.user.UserController;
import br.edu.ufersa.pw.focustask.features.project.ProjectController;
import br.edu.ufersa.pw.focustask.features.task.TaskController;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionController;
import jakarta.persistence.Entity;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpDtoContractTests {
    private static final List<Class<?>> CONTROLLERS = List.of(UserController.class,
            ProjectController.class, TaskController.class, FocusSessionController.class);

    @Test
    void all27EndpointsExposeOnlyHttpDtosAndAll12BodiesUseValid() {
        int endpoints = 0;
        int bodies = 0;
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) continue;
                endpoints++;
                assertDtoPayload(method.getGenericReturnType());
                assertNoEntities(method.getGenericReturnType(), new HashSet<>());
                for (Parameter parameter : method.getParameters()) {
                    assertNoEntities(parameter.getParameterizedType(), new HashSet<>());
                    if (!parameter.isAnnotationPresent(RequestBody.class)) continue;
                    bodies++;
                    assertTrue(parameter.isAnnotationPresent(Valid.class), method.toString());
                    assertTrue(parameter.getType().isRecord(), method.toString());
                    assertDtoPayload(parameter.getParameterizedType());
                }
            }
        }
        assertEquals(27, endpoints);
        assertEquals(12, bodies);
    }

    private static void assertDtoPayload(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            assertTrue(raw == ResponseEntity.class || Collection.class.isAssignableFrom(raw)
                    || raw == Optional.class || Page.class.isAssignableFrom(raw), type.toString());
            for (Type argument : parameterized.getActualTypeArguments()) assertDtoPayload(argument);
            return;
        }
        assertInstanceOf(Class.class, type);
        Class<?> clazz = (Class<?>) type;
        if (clazz == void.class || clazz == Void.class) return;
        assertTrue(clazz.isRecord() && clazz.getPackageName().endsWith(".dto"), type.toString());
    }

    private static void assertNoEntities(Type type, Set<Type> visited) {
        if (!visited.add(type)) return;
        if (type instanceof ParameterizedType parameterized) {
            assertNoEntities(parameterized.getRawType(), visited);
            for (Type argument : parameterized.getActualTypeArguments()) assertNoEntities(argument, visited);
        } else if (type instanceof GenericArrayType array) {
            assertNoEntities(array.getGenericComponentType(), visited);
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) assertNoEntities(bound, visited);
            for (Type bound : wildcard.getLowerBounds()) assertNoEntities(bound, visited);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) assertNoEntities(bound, visited);
        } else if (type instanceof Class<?> clazz) {
            assertFalse(clazz.isAnnotationPresent(Entity.class), "Entity in HTTP contract: " + clazz.getName());
            if (clazz.isArray()) assertNoEntities(clazz.getComponentType(), visited);
            if (clazz.isRecord()) {
                for (RecordComponent component : clazz.getRecordComponents()) {
                    assertNoEntities(component.getGenericType(), visited);
                    Class<?> componentClass = component.getType();
                    if (componentClass.isEnum()) {
                        assertTrue(componentClass.getPackageName().endsWith(".dto"), component.toString());
                    }
                }
            }
        } else {
            fail("Unsupported HTTP contract type: " + type);
        }
    }

    @Test
    void auditDetectsEntitiesInsideWrappersAndNestedRecords() throws Exception {
        Class<?> entity = Class.forName("br.edu.ufersa.pw.focustask.features.task.Task");
        for (Type type : List.of(entity, parameterized(ResponseEntity.class, entity),
                parameterized(List.class, entity), parameterized(Optional.class, entity),
                parameterized(Page.class, entity), parameterized(NestedLeak.class, parameterized(List.class, entity)))) {
            assertThrows(AssertionError.class, () -> assertNoEntities(type, new HashSet<>()));
        }
    }

    private static ParameterizedType parameterized(Class<?> raw, Type argument) {
        return new ParameterizedType() {
            public Type[] getActualTypeArguments() { return new Type[] {argument}; }
            public Type getRawType() { return raw; }
            public Type getOwnerType() { return null; }
        };
    }

    record NestedLeak<T>(List<T> entities) {}
}
