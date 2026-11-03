-- V1__init_schema.sql
-- Drug Inventory and Supply Chain Tracking System (DIS) Schema

CREATE TABLE IF NOT EXISTS drug_master (
    drug_id VARCHAR(50) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    unit_cost DECIMAL(10, 2) NOT NULL,
    service_level DECIMAL(4, 2) NOT NULL DEFAULT 0.95
);

CREATE TABLE IF NOT EXISTS supplier (
    supplier_id VARCHAR(50) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(255) NOT NULL,
    lead_time_days INT NOT NULL
);

CREATE TABLE IF NOT EXISTS batch (
    batch_id VARCHAR(100) NOT NULL PRIMARY KEY,
    drug_id VARCHAR(50) NOT NULL,
    supplier_id VARCHAR(50) NOT NULL,
    manufacturing_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    status VARCHAR(20) NOT NULL, -- ACTIVE, CLOSED
    CONSTRAINT fk_batch_drug FOREIGN KEY (drug_id) REFERENCES drug_master(drug_id),
    CONSTRAINT fk_batch_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id)
);

CREATE INDEX idx_batch_drug_expiry ON batch(drug_id, expiry_date);

CREATE TABLE IF NOT EXISTS transaction (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL, -- INBOUND, OUTBOUND, EXPIRED_WRITE_OFF, RETURN, ADJUSTMENT
    quantity INT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    location VARCHAR(100) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_transaction_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id)
);

CREATE INDEX idx_transaction_batch_timestamp ON transaction(batch_id, timestamp);

CREATE TABLE IF NOT EXISTS alert (
    alert_id VARCHAR(100) NOT NULL PRIMARY KEY,
    drug_id VARCHAR(50) NOT NULL,
    batch_id VARCHAR(100) NULL,
    type VARCHAR(30) NOT NULL, -- LOW_STOCK, NEAR_EXPIRY, HIGH_RISK
    severity VARCHAR(20) NOT NULL, -- WARNING, CRITICAL
    status VARCHAR(20) NOT NULL, -- OPEN, ACK, RESOLVED
    risk_score DECIMAL(5, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP NULL,
    CONSTRAINT fk_alert_drug FOREIGN KEY (drug_id) REFERENCES drug_master(drug_id),
    CONSTRAINT fk_alert_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id)
);

CREATE INDEX idx_alert_status_type ON alert(status, type);

CREATE TABLE IF NOT EXISTS risk_snapshot (
    snapshot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    drug_id VARCHAR(50) NOT NULL,
    batch_id VARCHAR(100) NULL,
    stock_risk DECIMAL(5, 4) NOT NULL,
    expiry_risk DECIMAL(5, 4) NOT NULL,
    demand_variability DECIMAL(5, 4) NOT NULL,
    risk_index DECIMAL(5, 2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- SAFE, WATCH, CRITICAL
    reorder_point INT NOT NULL,
    safety_stock INT NOT NULL,
    CONSTRAINT fk_snapshot_drug FOREIGN KEY (drug_id) REFERENCES drug_master(drug_id),
    CONSTRAINT fk_snapshot_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id)
);

CREATE INDEX idx_risk_snapshot_drug_date ON risk_snapshot(drug_id, snapshot_date);

CREATE TABLE IF NOT EXISTS app_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL, -- PHARMACIST, WAREHOUSE, ADMIN
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(100) NOT NULL PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL
);

-- Seed Initial System Configurations
INSERT INTO system_config (config_key, config_value) VALUES
('weight.stock', '0.4'),
('weight.expiry', '0.4'),
('weight.variability', '0.2'),
('expiry.warn.days', '90'),
('expiry.critical.days', '30'),
('spike.ratio', '1.5'),
('band.watch', '30'),
('band.critical', '60'),
('route.LOW_STOCK', 'PHARMACIST,ADMIN'),
('route.NEAR_EXPIRY', 'PHARMACIST,WAREHOUSE'),
('route.HIGH_RISK', 'ADMIN,PHARMACIST')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);
