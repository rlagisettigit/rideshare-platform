-- Google/Apple ID tokens carry no phone number, so a brand-new user provisioned via
-- "Sign in with Google/Apple" has none to store at signup time. The uq_users_mobile unique
-- index still holds under InnoDB with mobile NULL - multiple NULLs are allowed in a unique
-- index, only duplicate non-NULL values are rejected.
ALTER TABLE users MODIFY COLUMN mobile VARCHAR(20) NULL;
