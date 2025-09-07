CREATE TABLE estados
(
    state_id    VARCHAR(100) PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    description TEXT
);

CREATE TABLE tipo_prestamo
(
    loan_type_id         VARCHAR(100) PRIMARY KEY,
    name                 VARCHAR(50)    NOT NULL,
    minimum_amount       NUMERIC(15, 2) NOT NULL,
    maximum_amount       NUMERIC(15, 2) NOT NULL,
    interest_rate        NUMERIC(5, 2)  NOT NULL,
    automatic_validation BOOLEAN        NOT NULL
);

CREATE TABLE solicitud
(
    solicitud_id VARCHAR(100) PRIMARY KEY,
    amount       NUMERIC(15, 2) NOT NULL,
    term         INTEGER        NOT NULL,
    email        VARCHAR(100)   NOT NULL,
    state_id     VARCHAR(100)   NOT NULL,
    loan_type_id VARCHAR(100)   NOT NULL,
    FOREIGN KEY (state_id) REFERENCES estados (state_id),
    FOREIGN KEY (loan_type_id) REFERENCES tipo_prestamo (loan_type_id)
);


INSERT INTO estados (state_id, name, description)
VALUES ('estado-001', 'Pendiente de revisión', 'La solicitud está pendiente de ser revisada'),
       ('estado-002', 'Rechazadas', 'La solicitud está rechazada'),
       ('estado-003', 'Revision manual', 'La solicitud está pendiente por revision manual'),
       ('estado-004', 'Aprobadas', 'La solicitud está aprobada');

INSERT INTO tipo_prestamo (loan_type_id, name, minimum_amount, maximum_amount, interest_rate, automatic_validation)
VALUES ('prestamo-001', 'Crédito Personal', 1000.00, 10000.00, 12.50, TRUE),
       ('prestamo-002', 'Crédito Vehicular', 5000.00, 50000.00, 9.75, FALSE),
       ('prestamo-003', 'Crédito Hipotecario', 20000.00, 300000.00, 7.20, TRUE);