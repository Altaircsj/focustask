package br.edu.ufersa.pw.focustask.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTests {
    private static final String SECRET = "unit-test-only-secret-32-bytes-or-more";
    private final Instant now = Instant.parse("2026-09-29T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final TokenService service = new TokenService(SECRET, "focustask-api", Duration.ofMinutes(15), clock);
    private final AuthenticatedUser user = new AuthenticatedUser(7L, "student@example.com", null, List.of());

    @Test void generatesRequiredClaimsAndExpiresAtFifteenMinutes() {
        String token = service.generate(user);
        var jwt = JWT.decode(token);
        assertEquals("HS256", jwt.getAlgorithm());
        assertEquals("focustask-api", jwt.getIssuer());
        assertEquals(user.getUsername(), jwt.getSubject());
        assertEquals(7L, jwt.getClaim("userId").asLong());
        assertEquals(now, jwt.getIssuedAtAsInstant());
        assertEquals(now.plusSeconds(900), jwt.getExpiresAtAsInstant());
        assertEquals(new TokenService.Identity(7L, user.getUsername()), service.verify(token));
        TokenService expired = new TokenService(SECRET, "focustask-api", Duration.ofMinutes(15),
                Clock.fixed(now.plusSeconds(901), ZoneOffset.UTC));
        assertThrows(BadCredentialsException.class, () -> expired.verify(token));
    }

    @Test void rejectsMissingOrInvalidClaimsIssuerSignatureAndAlgorithm() {
        var algorithm = Algorithm.HMAC256(SECRET);
        for (String invalid : List.of(
                JWT.create().withIssuer("focustask-api").withSubject("student@example.com").withClaim("userId", 7L).sign(algorithm),
                JWT.create().withIssuer("focustask-api").withIssuedAt(now).withExpiresAt(now.plusSeconds(900)).sign(algorithm),
                JWT.create().withIssuer("other").withSubject("student@example.com").withClaim("userId", 7L).withIssuedAt(now).withExpiresAt(now.plusSeconds(900)).sign(algorithm),
                JWT.create().withIssuer("focustask-api").withSubject("student@example.com").withClaim("userId", -1L).withIssuedAt(now).withExpiresAt(now.plusSeconds(900)).sign(algorithm),
                JWT.create().withIssuer("focustask-api").withSubject("student@example.com").withClaim("userId", "invalid").withIssuedAt(now).withExpiresAt(now.plusSeconds(900)).sign(algorithm),
                JWT.create().withIssuer("focustask-api").withSubject(" ").withClaim("userId", 7L).withIssuedAt(now).withExpiresAt(now.plusSeconds(900)).sign(algorithm),
                JWT.create().withIssuer("focustask-api").withSubject("student@example.com").withClaim("userId", 7L).withIssuedAt(now).withExpiresAt(now.plusSeconds(900)).sign(Algorithm.HMAC256("wrong-secret")),
                JWT.create().withIssuer("focustask-api").withSubject("student@example.com").withClaim("userId", 7L).withIssuedAt(now).withExpiresAt(now.plusSeconds(900)).sign(Algorithm.HMAC512(SECRET)),
                "not-a-token")) {
            assertThrows(BadCredentialsException.class, () -> service.verify(invalid));
        }
    }

    @Test void missingOrWeakSecretFailsWithoutExposingItsValue() {
        for (String secret : new String[]{null, "", "private-short-secret"}) {
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> new TokenService(secret, "focustask-api", Duration.ofMinutes(15), clock));
            if (secret != null && !secret.isEmpty()) assertFalse(exception.getMessage().contains(secret));
        }
    }
}
