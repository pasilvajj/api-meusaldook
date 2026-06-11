CREATE TABLE budget_goal (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    goal_year INT NOT NULL,
    goal_month INT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_budget_goal UNIQUE (user_id, category_id, goal_year, goal_month, kind),
    CONSTRAINT fk_budget_goal_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_goal_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);

CREATE INDEX idx_budget_goal_period ON budget_goal (user_id, goal_year, goal_month, kind);
