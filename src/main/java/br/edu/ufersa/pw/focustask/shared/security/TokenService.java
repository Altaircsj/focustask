package br.edu.ufersa.pw.focustask.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

@Service
public class TokenService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final Duration expiration;
    private final Clock clock;

    public TokenService(@Value("${api.security.token.secret}") String secret,
                        @Value("${api.security.token.issuer}") String issuer,
                        @Value("${api.security.token.expiration}") Duration expiration, Clock clock) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        if (issuer.isBlank() || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("JWT issuer and positive expiration are required");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expiration = expiration;
        this.clock = clock;
        this.verifier = ((JWTVerifier.BaseVerification) JWT.require(algorithm).withIssuer(issuer)
                .withClaimPresence("exp").withClaimPresence("iat")
                .withClaimPresence("sub").withClaimPresence("userId")).build(clock);
    }

    public String generate(AuthenticatedUser user) {
        var now = clock.instant();
        return JWT.create().withIssuer(issuer).withSubject(user.getUsername()).withClaim("userId", user.getId())
                .withIssuedAt(now).withExpiresAt(now.plus(expiration)).sign(algorithm);
    }

    public Identity verify(String token) {
        try {
            var jwt = verifier.verify(token);
            String email = jwt.getSubject();
            Long id = jwt.getClaim("userId").asLong();
            if (email == null || email.isBlank() || id == null || id <= 0) {
                throw new BadCredentialsException("Invalid token");
            }
            return new Identity(id, email);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            // Do not retain the JWT library exception: it may contain claims supplied by the client.
            throw new BadCredentialsException("Invalid token");
        }
    }

    public record Identity(Long id, String email) {}
}
