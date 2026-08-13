-- =============================================================================
--  PMT - Project Management Tool
--  Script de génération de la base de données (structure + données de test)
--
--  SGBD          : PostgreSQL 15
--  Exécution      : joué automatiquement par le conteneur `db` au premier
--                   démarrage (volume ./pmt/init-db -> /docker-entrypoint-initdb.d)
--  Exécution manuelle :
--      psql -h localhost -p 5433 -U postgres -d pmt_db -f 01_init_db.sql
--
--  Le schéma correspond exactement aux entités JPA du backend
--  (com.exercice.pmt.model). Les libellés de rôles ADMIN / MEMBER / GUEST
--  sont ceux attendus par TaskService et ProjectMemberService.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. STRUCTURE
-- -----------------------------------------------------------------------------

-- Rôles applicables à un membre sur un projet : ADMIN, MEMBER, GUEST
CREATE TABLE IF NOT EXISTS roles (
    id      SERIAL PRIMARY KEY,
    libelle VARCHAR(50) UNIQUE NOT NULL
);

-- Comptes utilisateurs de la plateforme
CREATE TABLE IF NOT EXISTS utilisateurs (
    id       BIGSERIAL PRIMARY KEY,
    email    VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL
);

-- Projets ; `admin_id` est le créateur, administrateur de droit du projet
CREATE TABLE IF NOT EXISTS projects (
    id          SERIAL PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    date_debut  TIMESTAMP,
    admin_id    BIGINT NOT NULL REFERENCES utilisateurs (id),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table d'association projet <-> utilisateur, porteuse du rôle
CREATE TABLE IF NOT EXISTS membres_projet (
    id             SERIAL PRIMARY KEY,
    projet_id      INTEGER NOT NULL REFERENCES projects (id),
    utilisateur_id BIGINT  NOT NULL REFERENCES utilisateurs (id),
    role_id        INTEGER NOT NULL REFERENCES roles (id),
    date_arrivee   DATE    NOT NULL,
    CONSTRAINT uq_membre_par_projet UNIQUE (projet_id, utilisateur_id)
);

-- Tâches d'un projet.
--   assigne_a_membre_id : le membre du projet à qui la tâche est assignée
--   assignee_id         : l'utilisateur correspondant (dénormalisation de confort)
--   date_fin_reelle     : renseignée automatiquement au passage au statut TERMINE
CREATE TABLE IF NOT EXISTS tasks (
    id                  SERIAL PRIMARY KEY,
    nom                 VARCHAR(255) NOT NULL,
    description         TEXT,
    status              VARCHAR(50),
    priorite            VARCHAR(20),
    date_echeance       DATE,
    date_fin_reelle     DATE,
    projet_id           INTEGER NOT NULL REFERENCES projects (id),
    assigne_a_membre_id INTEGER REFERENCES membres_projet (id),
    assignee_id         BIGINT  REFERENCES utilisateurs (id)
);

-- Journal des modifications apportées aux tâches
CREATE TABLE IF NOT EXISTS historique_taches (
    id               SERIAL PRIMARY KEY,
    tache_id         INTEGER NOT NULL REFERENCES tasks (id),
    auteur_membre_id INTEGER NOT NULL REFERENCES membres_projet (id),
    action           VARCHAR(255),
    date_action      TIMESTAMP
);

-- Index sur les clés étrangères les plus sollicitées par l'application
CREATE INDEX IF NOT EXISTS idx_membres_projet_projet ON membres_projet (projet_id);
CREATE INDEX IF NOT EXISTS idx_tasks_projet          ON tasks (projet_id);
CREATE INDEX IF NOT EXISTS idx_historique_tache      ON historique_taches (tache_id);

-- -----------------------------------------------------------------------------
-- 2. DONNÉES DE RÉFÉRENCE (indispensables au fonctionnement)
-- -----------------------------------------------------------------------------

INSERT INTO roles (id, libelle) VALUES
    (1, 'ADMIN'),
    (2, 'MEMBER'),
    (3, 'GUEST')
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3. DONNÉES DE TEST
--    Mots de passe stockés en clair, conformément à l'implémentation actuelle
--    (la sécurisation du backend n'est pas exigée par l'énoncé).
--    Comptes de démonstration : mot de passe = "admin123"
-- -----------------------------------------------------------------------------

INSERT INTO utilisateurs (id, email, password, username) VALUES
    (1, 'admin@pmt.com',        'admin123', 'Administrateur'),
    (2, 'jean.dupont@pmt.com',  'admin123', 'Jean Dupont'),
    (3, 'marie.curie@pmt.com',  'admin123', 'Marie Curie')
ON CONFLICT (id) DO NOTHING;

INSERT INTO projects (id, nom, description, date_debut, admin_id, created_at, updated_at) VALUES
    (1, 'Déploiement Docker', 'Mise en place de l''infrastructure conteneurisée', '2026-03-13 09:00:00', 1, NOW(), NOW()),
    (2, 'Refonte Site Web',   'Migration du site vers Angular et Spring Boot',    '2026-04-02 09:00:00', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Projet 1 : un admin, un membre, un observateur -> couvre les 3 rôles
INSERT INTO membres_projet (id, projet_id, utilisateur_id, role_id, date_arrivee) VALUES
    (1, 1, 1, 1, '2026-03-13'),
    (2, 1, 2, 2, '2026-03-13'),
    (3, 1, 3, 3, '2026-03-13'),
    (4, 2, 1, 1, '2026-04-02'),
    (5, 2, 2, 2, '2026-04-02')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tasks (id, nom, description, status, priorite, date_echeance, date_fin_reelle, projet_id, assigne_a_membre_id, assignee_id) VALUES
    (1, 'Finaliser le Docker Compose', 'Préparer le fichier pour le jury',  'EN_COURS', 'HAUTE',   '2026-03-20', NULL,         1, 1, 1),
    (2, 'Rédiger le README',           'Procédure de déploiement complète', 'A_FAIRE',  'MOYENNE', '2026-03-25', NULL,         1, 2, 2),
    (3, 'Configurer la CI',            'Workflow GitHub Actions + JaCoCo',  'TERMINE',  'HAUTE',   '2026-03-15', '2026-03-14', 1, 1, 1),
    (4, 'Maquettes UI',                'Design des écrans principaux',      'A_FAIRE',  'BASSE',   '2026-04-10', NULL,         2, 5, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO historique_taches (id, tache_id, auteur_membre_id, action, date_action) VALUES
    (1, 1, 1, 'Création de la tâche',              '2026-03-13 09:30:00'),
    (2, 1, 1, 'Changement de statut : EN_COURS',   '2026-03-14 10:15:00'),
    (3, 3, 1, 'Changement de statut : TERMINE',    '2026-03-14 17:45:00')
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4. SYNCHRONISATION DES SÉQUENCES
--    Indispensable après des INSERT avec id explicite, sans quoi le prochain
--    INSERT applicatif entrerait en collision de clé primaire.
-- -----------------------------------------------------------------------------

SELECT setval('roles_id_seq',             (SELECT MAX(id) FROM roles));
SELECT setval('utilisateurs_id_seq',      (SELECT MAX(id) FROM utilisateurs));
SELECT setval('projects_id_seq',          (SELECT MAX(id) FROM projects));
SELECT setval('membres_projet_id_seq',    (SELECT MAX(id) FROM membres_projet));
SELECT setval('tasks_id_seq',             (SELECT MAX(id) FROM tasks));
SELECT setval('historique_taches_id_seq', (SELECT MAX(id) FROM historique_taches));
