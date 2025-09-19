create table if not exists estados
(
    state_id    VARCHAR(100) PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    description TEXT
);

create table if not exists tipo_prestamo
(
    loan_type_id         VARCHAR(100) PRIMARY KEY,
    name                 VARCHAR(50)    NOT NULL,
    minimum_amount       NUMERIC(15, 2) NOT NULL,
    maximum_amount       NUMERIC(15, 2) NOT NULL,
    interest_rate        NUMERIC(5, 2)  NOT NULL,
    automatic_validation BOOLEAN        NOT NULL
);

create table if not exists solicitud
(
    solicitud_id VARCHAR(100) PRIMARY KEY,
    amount       NUMERIC(15, 2) NOT NULL,
    term         INTEGER        NOT NULL,
    email        VARCHAR(100)   NOT NULL,
    state_id     VARCHAR(100)   NOT NULL,
    loan_type_id VARCHAR(100)   NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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

-- 8 emails que TENDRÁN solicitudes aprobadas (uno por usuario)
WITH approved_emails(email) AS (
    VALUES
        ('ana.gonzalez@ejemplo.com'),
        ('carlos.sanchez@ejemplo.com'),
        ('sofia.ramirez@ejemplo.com'),
        ('luis.torres@ejemplo.com'),
        ('maria.lopez@ejemplo.com'),
        ('pedro.castillo@ejemplo.com'),
        ('laura.mendoza@ejemplo.com'),
        ('diego.alvarez@ejemplo.com')
),
     ae AS (
         SELECT ROW_NUMBER() OVER () AS rn, email
         FROM approved_emails
     )
INSERT INTO solicitud (solicitud_id, amount, term, email, state_id, loan_type_id)
SELECT
    'sol-' || lpad(rn::text, 3, '0') AS solicitud_id,
    CASE lt
        WHEN 'prestamo-001' THEN ROUND( (1000   + random()*9000  )::numeric, 2)
        WHEN 'prestamo-002' THEN ROUND( (5000   + random()*45000 )::numeric, 2)
        ELSE                      ROUND( (20000  + random()*280000)::numeric, 2)
        END AS amount,
    (ARRAY[6,12,24,36,48])[CEIL(random()*5)] AS term,
  ae.email,
  'estado-004' AS state_id,
  lt AS loan_type_id
FROM ae
    CROSS JOIN LATERAL (
    SELECT (ARRAY['prestamo-001','prestamo-002','prestamo-003'])[CEIL(random()*3)] AS lt
    ) t;

WITH allmails(email) AS (
    VALUES
        ('juan.perez@ejemplo.com'), ('ana.gonzalez@ejemplo.com'), ('carlos.sanchez@ejemplo.com'),
        ('sofia.ramirez@ejemplo.com'), ('luis.torres@ejemplo.com'), ('maria.lopez@ejemplo.com'),
        ('pedro.castillo@ejemplo.com'), ('laura.mendoza@ejemplo.com'), ('diego.alvarez@ejemplo.com'),
        ('valentina.rios@ejemplo.com'), ('andres.garcia@ejemplo.com'), ('camila.herrera@ejemplo.com'),
        ('jorge.martinez@ejemplo.com'), ('daniela.cruz@ejemplo.com'), ('sebastian.ortega@ejemplo.com'),
        ('natalia.vargas@ejemplo.com'), ('fernando.morales@ejemplo.com'), ('patricia.navarro@ejemplo.com'),
        ('ricardo.pineda@ejemplo.com'), ('monica.salazar@ejemplo.com'), ('julian.gomez@ejemplo.com'),
        ('carolina.vega@ejemplo.com'), ('esteban.ruiz@ejemplo.com')
),
     shuffled AS (
         SELECT email, ROW_NUMBER() OVER (ORDER BY random()) AS rn FROM allmails
     ),
     g AS (SELECT generate_series(1,92) AS i),
     c AS (SELECT COUNT(*)::int AS cnt FROM shuffled)
INSERT INTO solicitud (solicitud_id, amount, term, email, state_id, loan_type_id)
SELECT
    'sol-' || lpad((g.i+8)::text, 3, '0'),
    CASE lt
        WHEN 'prestamo-001' THEN ROUND((1000 + random()*9000 )::numeric, 2)
        WHEN 'prestamo-002' THEN ROUND((5000 + random()*45000)::numeric, 2)
        ELSE                      ROUND((20000+ random()*280000)::numeric, 2)
        END,
    (ARRAY[6,12,24,36,48])[1 + floor(random()*5)::int],
  (SELECT email FROM shuffled s, c WHERE s.rn = 1 + ((g.i-1) % c.cnt)),
  (ARRAY['estado-001','estado-002','estado-003'])[1 + floor(random()*3)::int],
  lt
FROM g
    CROSS JOIN LATERAL (
    SELECT (ARRAY['prestamo-001','prestamo-002','prestamo-003'])[1 + floor(random()*3)::int]
    ) t(lt);
