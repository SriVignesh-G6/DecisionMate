CREATE DATABASE IF NOT EXISTS decisionmate
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE decisionmate;

-- Tables are managed by Spring Data JPA on first startup (ddl-auto=update).
-- This file creates the database and documents the expected table relationships.
-- decisions (1) ---- (many) criteria
-- decisions (1) ---- (many) alternatives
-- alternatives (1) ---- (many) scores
-- criteria (1) ---- (many) scores
