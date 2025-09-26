package co.com.crediya_solicitud.api.utils;

import co.com.crediya_solicitud.api.dto.ClaismoDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.model.exception.specificexceptions.ForbiddenException;
import co.com.crediya_solicitud.model.exception.specificexceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

class ValidateResponseTokenTest {

    private GlobalLogger logger;
    private ValidateResponseToken validateToken;

    @BeforeEach
    void setUp() {
        logger = mock(GlobalLogger.class);
        validateToken = new ValidateResponseToken(logger);
    }

    @Test
    void validate_returns_claims_when_non_null() {
        ClaismoDto claims = new ClaismoDto("Juan", "j@a.com", "Customer", "f1", "n", "123", "f2", "id");
        StepVerifier.create(validateToken.validate(claims))
                .expectNext(claims)
                .verifyComplete();
    }

    @Test
    void validate_throws_unauthorized_when_null() {
        StepVerifier.create(validateToken.validate(null))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(UnauthorizedException.class))
                .verify();
    }

    @Test
    void isCustomer_ok_when_role_customer() {
        ClaismoDto claims = new ClaismoDto("Juan", "j@a.com", "Customer", "f1", "n", "123", "f2", "id");
        StepVerifier.create(validateToken.isCustomer(claims))
                .expectNext(claims)
                .verifyComplete();
    }

    @Test
    void isCustomer_forbidden_when_other_role() {
        ClaismoDto claims = new ClaismoDto("Juan", "j@a.com", "Adviser", "f1", "n", "123", "f2", "id");
        StepVerifier.create(validateToken.isCustomer(claims))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ForbiddenException.class))
                .verify();
    }

    @Test
    void isAdviser_ok_when_role_adviser() {
        ClaismoDto claims = new ClaismoDto("Ana", "a@b.com", "Adviser", "f1", "n", "456", "f2", "id");
        StepVerifier.create(validateToken.isAdviser(claims))
                .expectNext(claims)
                .verifyComplete();
    }

    @Test
    void isAdviser_forbidden_when_other_role() {
        ClaismoDto claims = new ClaismoDto("Ana", "a@b.com", "Customer", "f1", "n", "456", "f2", "id");
        StepVerifier.create(validateToken.isAdviser(claims))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ForbiddenException.class))
                .verify();
    }

    @Test
    void requireRole_is_case_insensitive() {
        ClaismoDto claims = new ClaismoDto("Pepe", "p@c.com", "customer", "f1", "n", "789", "f2", "id");
        StepVerifier.create(validateToken.requireRole(claims, "Customer"))
                .expectNext(claims)
                .verifyComplete();
    }

    @Test
    void requireRole_forbidden_when_role_null() {
        ClaismoDto claims = new ClaismoDto("Pepe", "p@c.com", null, "f1", "n", "789", "f2", "id");
        StepVerifier.create(validateToken.requireRole(claims, "Customer"))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ForbiddenException.class))
                .verify();
    }

    @Test
    void isOwner_ok_when_document_matches() {
        ClaismoDto claims = new ClaismoDto("Luz", "l@z.com", "Customer", "f1", "n", "111", "f2", "id");
        StepVerifier.create(validateToken.isOwner(claims, "111"))
                .expectNext(claims)
                .verifyComplete();
    }

    @Test
    void isOwner_badRequest_when_document_null() {
        ClaismoDto claims = new ClaismoDto("Luz", "l@z.com", "Customer", "f1", "n", "111", "f2", "id");
        StepVerifier.create(validateToken.isOwner(claims, null))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ForbiddenException.class))
                .verify();
    }

    @Test
    void isOwner_badRequest_when_document_blank() {
        ClaismoDto claims = new ClaismoDto("Luz", "l@z.com", "Customer", "f1", "n", "111", "f2", "id");
        StepVerifier.create(validateToken.isOwner(claims, "  "))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ForbiddenException.class))
                .verify();
    }

    @Test
    void isOwner_forbidden_when_claim_document_differs() {
        ClaismoDto claims = new ClaismoDto("Luz", "l@z.com", "Customer", "f1", "n", "111", "f2", "id");
        StepVerifier.create(validateToken.isOwner(claims, "222"))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ForbiddenException.class))
                .verify();
    }

    @Test
    void isOwner_forbidden_when_claim_document_is_null() {
        ClaismoDto claims = new ClaismoDto("Luz", "l@z.com", "Customer", "f1", "n", null, "f2", "id");
        StepVerifier.create(validateToken.isOwner(claims, "333"))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(NullPointerException.class))
                .verify();
    }
}