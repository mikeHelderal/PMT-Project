

CREATE TABLE IF NOT EXISTS roles (
                                     id SERIAL PRIMARY KEY,
                                     libelle VARCHAR(50) UNIQUE NOT NULL
    );

CREATE TABLE IF NOT EXISTS utilisateurs (
                                            id SERIAL PRIMARY KEY,
                                            email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL
    );

CREATE TABLE IF NOT EXISTS projects (
                                        id SERIAL PRIMARY KEY,
                                        nom VARCHAR(255) NOT NULL,
    description TEXT,
    date_debut DATE,
    admin_id INTEGER REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS membres_projet (
                                              id SERIAL PRIMARY KEY,
                                              projet_id INTEGER REFERENCES projects(id),
    utilisateur_id INTEGER REFERENCES utilisateurs(id),
    role_id INTEGER REFERENCES roles(id),
    date_arrivee DATE
    );

CREATE TABLE IF NOT EXISTS tasks (
                                     id SERIAL PRIMARY KEY,
                                     nom VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    description TEXT,
    priorite VARCHAR(20),
    date_echeance DATE,
    projet_id INTEGER REFERENCES projects(id),
    assigne_a_membre_id INTEGER REFERENCES utilisateurs(id)
    );

---
--- 2. INSERTION DES DONNÉES DE RÉFÉRENCE (Roles & Users)
---

INSERT INTO roles (id, libelle) VALUES
                                    (1, 'ADMIN'),
                                    (2, 'MEMBER'),
                                    (3, 'GUEST')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO utilisateurs (id, email, password, username) VALUES
                                                             (1, 'admin@pmt.com', 'admin123', 'Administrateur'),
                                                             (2, 'jean.dupont@pmt.com', 'admin123', 'Jean Dupont'),
                                                             (3, 'marie.curie@pmt.com', 'admin123', 'Marie Curie')
    ON CONFLICT (id) DO NOTHING;

---
--- 3. INSERTION DES DONNÉES DE TEST (Projets & Tâches)
---

INSERT INTO projects (id, nom, description, date_debut, admin_id, created_at) VALUES
    (1, 'Déploiement Docker', 'Mise en place de l infrastructure conteneurisée', '2026-03-13', 1, NOW())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO membres_projet (id, projet_id, utilisateur_id, role_id, date_arrivee) VALUES
                                                                                      (1, 1, 1, 1, '2026-03-13'),
                                                                                      (2, 1, 2, 2, '2026-03-13'),
                                                                                      (3, 1, 3, 2, '2026-03-13')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO tasks (id, nom, status, description, priorite, date_echeance, projet_id, assigne_a_membre_id) VALUES
    (1, 'Finaliser le Docker Compose', 'EN_COURS', 'Préparer le fichier pour le jury', 'HAUTE', '2026-03-20', 1, 1)
    ON CONFLICT (id) DO NOTHING;

---
--- 4. SYNCHRONISATION DES SÉQUENCES (Indispensable pour l'auto-incrément)
---

SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('utilisateurs_id_seq', (SELECT MAX(id) FROM utilisateurs));
SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));
SELECT setval('membres_projet_id_seq', (SELECT MAX(id) FROM membres_projet));
SELECT setval('tasks_id_seq', (SELECT MAX(id) FROM tasks));