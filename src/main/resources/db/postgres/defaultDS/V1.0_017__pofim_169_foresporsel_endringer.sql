ALTER TABLE FORESPOERSEL
    ADD FORESPOERSEL_TYPE VARCHAR(50)
        NOT NULL DEFAULT 'BESTILT_AV_FAGSYSTEM';

ALTER TABLE FORESPOERSEL ALTER COLUMN SKJAERINGSTIDSPUNKT DROP NOT NULL;
ALTER TABLE FORESPOERSEL ALTER COLUMN FAGSYSTEM_SAKSNUMMER DROP NOT NULL;

COMMENT ON COLUMN FORESPOERSEL.FORESPOERSEL_TYPE IS 'Hvilken hendelse som ledet til opprettelse av forespørselen';
