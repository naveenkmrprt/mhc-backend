-- ==============================================================================
-- V1__init.sql
-- Baseline schema and data for MHC Dashboard, including Phase 1 modifications
-- ==============================================================================

-- 1. Exam Rule Sets
CREATE TABLE exam_rule_sets (
    id BIGSERIAL PRIMARY KEY,
    exam_cycle VARCHAR(50) NOT NULL,
    notification_number VARCHAR(100),
    notification_date DATE,
    source_document VARCHAR(255),
    source_url VARCHAR(500),
    source_page VARCHAR(50),
    source_quote TEXT,
    verification_status VARCHAR(50),
    written_total_marks INT,
    part_a_marks INT,
    part_b_marks INT,
    negative_mark_per_wrong_answer DOUBLE PRECISION,
    skill_test_marks INT,
    viva_marks INT,
    part_a_final_merit_included BOOLEAN,
    part_b_final_merit_included BOOLEAN,
    shortlisting_ratio VARCHAR(50),
    shortlisting_ratio_status VARCHAR(50),
    active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 2. Syllabus Categories
CREATE TABLE syllabus_categories (
    id BIGSERIAL PRIMARY KEY,
    part VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    total_marks INT,
    negative_marking BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 3. Syllabus SubTopics
CREATE TABLE syllabus_subtopics (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subtopic_category FOREIGN KEY (category_id) REFERENCES syllabus_categories(id)
);

-- 4. Questions
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT,
    micro_topic VARCHAR(255),
    question_text TEXT NOT NULL,
    option_a TEXT,
    option_b TEXT,
    option_c TEXT,
    option_d TEXT,
    correct_option VARCHAR(1) NOT NULL,
    difficulty_estimate VARCHAR(50) DEFAULT 'MEDIUM',
    difficulty_confidence VARCHAR(50),
    ocr_confidence DOUBLE PRECISION,
    duplicate_hash VARCHAR(255),
    source_document VARCHAR(255),
    source_url VARCHAR(500),
    source_page VARCHAR(50),
    source_question_number VARCHAR(50),
    source_quote TEXT,
    verification_status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_question_category FOREIGN KEY (category_id) REFERENCES syllabus_categories(id)
);

-- 5. Quiz Sessions
CREATE TABLE quiz_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_type VARCHAR(20) DEFAULT 'PRACTICE',
    mock_mode VARCHAR(50),
    rule_set_id BIGINT,
    question_pool VARCHAR(50),
    distribution_method VARCHAR(100),
    distribution_source VARCHAR(255),
    distribution_confidence VARCHAR(50),
    analysis_status VARCHAR(50) DEFAULT 'COMPLETED',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    total_questions INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    wrong_answers INT DEFAULT 0,
    unattempted INT DEFAULT 0,
    raw_score DOUBLE PRECISION DEFAULT 0.0,
    accuracy_pct DOUBLE PRECISION DEFAULT 0.0,
    duration_seconds BIGINT DEFAULT 0,
    weak_topics_json TEXT DEFAULT '[]',
    CONSTRAINT fk_quiz_rule_set FOREIGN KEY (rule_set_id) REFERENCES exam_rule_sets(id)
);

-- 6. Quiz Answers
CREATE TABLE quiz_answers (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option VARCHAR(1),
    is_correct BOOLEAN,
    is_guess BOOLEAN,
    is_skipped BOOLEAN,
    confidence_level VARCHAR(20),
    error_type VARCHAR(50),
    time_spent_seconds INT,
    review_note TEXT,
    classification_completed BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_answer_session FOREIGN KEY (session_id) REFERENCES quiz_sessions(id),
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

-- 7. Topic Progress
CREATE TABLE topic_progress (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    last_practiced_at TIMESTAMP,
    mastery_level VARCHAR(50) DEFAULT 'UNSEEN',
    accuracy_pct DOUBLE PRECISION DEFAULT 0.0,
    attempts INT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_progress_subtopic FOREIGN KEY (topic_id) REFERENCES syllabus_subtopics(id)
);

-- 8. Daily Logs
CREATE TABLE daily_logs (
    id BIGSERIAL PRIMARY KEY,
    log_date DATE UNIQUE NOT NULL,
    questions_attempted INT DEFAULT 0,
    time_spent_minutes INT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_questions_category ON questions(category_id);
CREATE INDEX idx_questions_verification ON questions(verification_status);
CREATE INDEX idx_quiz_answers_session ON quiz_answers(session_id);
CREATE INDEX idx_subtopics_category ON syllabus_subtopics(category_id);


-- ==============================================================================
-- OFFICIAL SEED DATA
-- ==============================================================================

INSERT INTO exam_rule_sets (
    exam_cycle, notification_number, notification_date, source_document, source_url,
    source_page, source_quote, verification_status, written_total_marks, part_a_marks,
    part_b_marks, negative_mark_per_wrong_answer, skill_test_marks, viva_marks,
    part_a_final_merit_included, part_b_final_merit_included, shortlisting_ratio,
    shortlisting_ratio_status, active, created_at, updated_at
) VALUES (
    'MHC_AP_2025', '171/2025', '2025-08-10', 'Notification No. 171/2025',
    'https://mhc.tn.gov.in/recruitment/docs/NOTIFICATION%20171%20of%20%202025%20-%20ASSISTANT%20PROGRAMMER.pdf',
    'Page 11', 'The objective type test will consist of 120 multiple choice questions... 50 marks for Tamil Eligibility Test... 70 marks for General Knowledge and Subject tests. 1/4th mark will be deducted for each incorrect answer.',
    'OFFICIAL_CONFIRMED', 120, 50, 70, 0.25, 50, 25, FALSE, TRUE, NULL, 'OFFICIAL_NOT_FOUND', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO syllabus_categories (id, part, name, total_marks, negative_marking, created_at, updated_at) VALUES
(1, 'A', 'Tamil Eligibility Test', 50, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'B', '(a) Basics of Computer Networks and Operating System', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'B', '(b) Application Development', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'B', '(c) Internet & Web Technologies', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'B', '(d) Software Tools and Techniques', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'B', '(e) Cloud Computing', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'B', '(f) Analytical and Reasoning Skills', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'B', '(g) General Intelligence', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'C', 'Skill Test', 50, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'D', 'Viva-Voce', 25, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reset sequence after explicit ID inserts
SELECT setval('syllabus_categories_id_seq', 10);

INSERT INTO syllabus_subtopics (category_id, name, is_completed) VALUES
(1, 'Grammar: 1. Grammar of letters: type and number of Tamil letters, letters Birth, Initials & Genre, Pronouns & Genre, Sexuality, Language Initials, Finals, Ethnic Alphabets, Cursive Alphabets, Interrogative Alphabets, Hyphens.', FALSE),
(1, 'Grammar: 2. Word Grammar: Noun & Categories, Verb & Categories, Interjections, adjectives, epithets, contrasts, adjectives. Bisyllabic, Stratified, Monosyllabic Monolingual, Parsing, Parsing, Three Types of Languages, Case.', FALSE),
(1, 'Grammar: 3. General Grammar: Slip, slip position; Smoothness, Quantitative Series, Quantitative Series, Question, Answer Types, Object & Categories.', FALSE),
(1, 'Grammar: 4. Subject Grammar: Subjunctive, Subjunctive.', FALSE),
(1, 'Grammar: 5. Yapu Grammar: Yapu Elements, Classification, Pa Type (Venpa. Asiriyappa General Grammar).', FALSE),
(1, 'Grammar: 6. Group Grammar: Figurative Group, Figurative Group, Deductive Group, Contrast Team, following standard & types, Sublingual Team, Bilingual Team, Optimization Team, Insular Team, Programming Team.', FALSE),
(1, 'Grammar: 7. Linguistics: vallinam mikum place, mika place, continuous grammar.', FALSE),
(1, 'Grammar: 8. Split writing, writing together, writing antonyms, finding matching words, correcting errors, finding the Tamil equivalent of an English word.', FALSE),
(1, 'Section - b Literature: 1. Thirukkural, Tholkappiyam, Kambaramayanam, Ettuyya, Datupattu, Aimperungappiyam, Ainchirukapiyam, Aranunum, Bhakti literature. Filling news, quotes, nicknames, series related to short fiction, folk literature, new poetry, translated books.', FALSE),
(1, 'Section - C Tamil scholars and Tamil charity: 1. News and quotes related to Tamil Scholars, Tamil Antiquity, Tamil Culture, Tamil Prose, Tamil Charity, Community Charity.', FALSE),
(2, 'LAN, WAN, Wireless Networks, WLAN, Wi-Fi', FALSE),
(2, 'LAN Testing, LAN Proxy Server', FALSE),
(2, 'OSI Layers', FALSE),
(2, 'Network Protection and Security', FALSE),
(2, 'Basic Working Knowledge of Windows / Linux Operating Systems', FALSE),
(2, 'Open source Operating Systems', FALSE),
(3, 'Requirement Analysis & Engineering', FALSE),
(3, 'Software analysis & Design', FALSE),
(3, 'Flow Charts, DFD', FALSE),
(3, 'Concepts of OOPs', FALSE),
(3, 'Software Change Management', FALSE),
(3, 'Deployment of web based applications', FALSE),
(3, 'IDE Tools, Reporting Tools', FALSE),
(3, 'Documentation and User manuals', FALSE),
(3, 'Unit and Integrated Testing', FALSE),
(4, 'HTML5, CSS3, Java Script', FALSE),
(4, 'JSON, AJAX, XML', FALSE),
(4, 'Web Servers', FALSE),
(4, 'Server Programming Language — Java, PHP, Python', FALSE),
(4, 'Web Design Tools', FALSE),
(4, 'Mail Clients, DNS and Web Hosting', FALSE),
(4, 'Static and Dynamic Web Development', FALSE),
(4, 'Responsive Web Design', FALSE),
(4, 'API/Web Services, W3C Standards', FALSE),
(5, 'Office tools', FALSE),
(5, 'Working knowledge of Learning Management System (LMS)/Content Management System (CMS)', FALSE),
(5, 'Document Management System (DMS)', FALSE),
(5, 'Database Management System overview — Database design', FALSE),
(5, 'Data Analysis, ER Diagrams', FALSE),
(5, 'Database Server — Industry Standard DBMS including postgresql', FALSE),
(5, 'Overview of SQL Statements', FALSE),
(5, 'Programming Concepts and Snippets', FALSE),
(5, 'Testing Tools and Performance Monitoring tools', FALSE),
(6, 'Introduction to Cloud Computing — Definition of Cloud — Evolution of Cloud Computing', FALSE),
(6, 'Underlying Principles of Parallel and Distributed Computing', FALSE),
(6, 'Cloud Characteristics — Elasticity in Cloud — On-demand Provisioning', FALSE),
(6, 'Service Oriented Architecture — REST and Systems of Systems — Web Services — Publish-Subscribe Model', FALSE),
(6, 'Basics of Virtualization — Types of Virtualization — Implementation Levels of Virtualization — Virtualization Structures', FALSE),
(6, 'Tools and Mechanisms — Virtualization of CPU, Memory, I/O Devices', FALSE),
(6, 'Virtualization Support and Disaster Recovery — Layered Cloud Architecture Design', FALSE),
(6, 'NIST Cloud Computing Reference Architecture', FALSE),
(6, 'Public, Private and Hybrid Clouds', FALSE),
(6, 'IaaS — PaaS — SaaS', FALSE),
(6, 'Architectural Design Challenges', FALSE),
(6, 'Cloud Storage — Storage-as-a-Service — Advantages of Cloud Storage — Cloud Storage Providers — S3', FALSE),
(6, 'Inter Cloud Resource Management — Resource Provisioning and Resource Provisioning Methods', FALSE),
(6, 'Global Exchange of Cloud Resources', FALSE),
(6, 'Security Overview — Cloud Security Challenges — Software-as-a-Service Security', FALSE),
(6, 'Security Governance — Virtual Machine Security — IAM — Security Standards', FALSE),
(6, 'Hadoop — MapReduce — Virtual Box — Open Stack', FALSE),
(6, 'Federation in the Cloud — Four Levels of Federation — Federated Services and Applications — Future of Federation', FALSE),
(7, 'Logical Reasoning & Analogies', FALSE),
(7, 'Numerical Aptitude & Number Series', FALSE),
(7, 'Data Interpretation & Data Sufficiency', FALSE),
(7, 'Verbal Reasoning & Critical Thinking', FALSE),
(8, 'General Intelligence — Pattern Recognition', FALSE),
(8, 'Coding-Decoding & Blood Relations', FALSE),
(8, 'Clocks, Calendars & Directions', FALSE),
(9, 'Skill Test — Programming in Java / Python / PHP', FALSE),
(9, 'Skill Test — HTML5/CSS3/JavaScript Frontend', FALSE),
(9, 'Skill Test — Database Queries', FALSE),
(10, 'Viva — Computer Programming Expertise', FALSE),
(10, 'Viva — Knowledge in Coding & Security', FALSE);
