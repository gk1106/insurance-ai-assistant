CREATE TABLE customers (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    date_of_birth DATE NOT NULL,
    address_line VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_customers_email UNIQUE (email)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    customer_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_customer_id UNIQUE (customer_id),
    CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE TABLE policies (
    id UUID PRIMARY KEY,
    policy_number VARCHAR(50) NOT NULL,
    customer_id UUID NOT NULL,
    policy_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    coverage_amount NUMERIC(19,2) NOT NULL,
    premium_amount NUMERIC(19,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_policies_policy_number UNIQUE (policy_number),
    CONSTRAINT fk_policies_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_policies_dates CHECK (end_date > start_date),
    CONSTRAINT chk_policies_coverage_positive CHECK (coverage_amount > 0),
    CONSTRAINT chk_policies_premium_positive CHECK (premium_amount > 0)
);
CREATE INDEX idx_policies_customer_id ON policies (customer_id);
CREATE INDEX idx_policies_status ON policies (status);
CREATE INDEX idx_policies_end_date ON policies (end_date);

CREATE TABLE claims (
    id UUID PRIMARY KEY,
    claim_number VARCHAR(50) NOT NULL,
    policy_id UUID NOT NULL,
    claim_amount NUMERIC(19,2) NOT NULL,
    approved_amount NUMERIC(19,2),
    status VARCHAR(20) NOT NULL,
    incident_date DATE NOT NULL,
    description VARCHAR(2000) NOT NULL,
    rejection_reason VARCHAR(500),
    filed_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_claims_claim_number UNIQUE (claim_number),
    CONSTRAINT fk_claims_policy FOREIGN KEY (policy_id) REFERENCES policies (id),
    CONSTRAINT chk_claims_amount_positive CHECK (claim_amount > 0)
);
CREATE INDEX idx_claims_policy_id ON claims (policy_id);
CREATE INDEX idx_claims_status ON claims (status);

CREATE TABLE renewals (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL,
    previous_end_date DATE NOT NULL,
    new_end_date DATE NOT NULL,
    revised_premium_amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_renewals_policy FOREIGN KEY (policy_id) REFERENCES policies (id),
    CONSTRAINT chk_renewals_dates CHECK (new_end_date > previous_end_date)
);
CREATE INDEX idx_renewals_policy_id ON renewals (policy_id);
CREATE INDEX idx_renewals_status ON renewals (status);
