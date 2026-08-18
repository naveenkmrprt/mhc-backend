-- First delete progress data associated with these subtopics to prevent foreign key violations
DELETE FROM topic_progress WHERE topic_id IN (SELECT id FROM syllabus_subtopics WHERE category_id = 1);

-- Then delete the old subtopics
DELETE FROM syllabus_subtopics WHERE category_id = 1;

-- Now insert the new updated detailed syllabus subtopics
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
(1, 'Section - C Tamil scholars and Tamil charity: 1. News and quotes related to Tamil Scholars, Tamil Antiquity, Tamil Culture, Tamil Prose, Tamil Charity, Community Charity.', FALSE);
