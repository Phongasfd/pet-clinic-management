-- Same as the repo's postgres-init.sql (kustomize cannot read files outside k8s/).
-- Runs only when the data volume is empty, like in docker compose.
CREATE DATABASE petclinic_auth;
CREATE DATABASE pet_clinic_pets;
CREATE DATABASE pet_clinic_billing;
CREATE DATABASE pet_clinic_analytics;
