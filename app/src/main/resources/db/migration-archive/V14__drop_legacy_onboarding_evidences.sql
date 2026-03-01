-- Legacy table cleanup: onboarding_evidences is no longer used
-- Evidence model is split into onboarding_evidence_bundles / onboarding_evidence_files

DROP TABLE IF EXISTS onboarding_evidences;
