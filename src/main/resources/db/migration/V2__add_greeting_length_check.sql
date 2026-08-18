-- A second migration, to show that Flyway versioning works and is exercised by the tests.
ALTER TABLE greeting
    ADD CONSTRAINT chk_greeting_recipient_not_blank
    CHECK (length(btrim(recipient)) > 0);
