INSERT INTO member (email, password, name, created_at, updated_at)
VALUES ('demo@workcheck.com', '$2a$10$zsHRG7P8RmwXtv8fxbNBmOk3cWbh5/d9J5MWfKxdLY/bythOZtNbe', '데모 학생', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO assignment (title, created_at, updated_at)
VALUES ('과학 탐구 보고서', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('자바 프로젝트', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO check_rule (assignment_id, file_type, rule_type, rule_value, created_at, updated_at)
VALUES ((SELECT assignment_id FROM assignment WHERE title = '과학 탐구 보고서'),
        'PDF', 'FILE_NAME_SUFFIX', '_보고서.pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT assignment_id FROM assignment WHERE title = '과학 탐구 보고서'),
        'PDF', 'PDF_REQUIRED_TEXT', '참고문헌', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT assignment_id FROM assignment WHERE title = '자바 프로젝트'),
        'ZIP', 'FILE_NAME_SUFFIX', '_프로젝트.zip', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT assignment_id FROM assignment WHERE title = '자바 프로젝트'),
        'ZIP', 'ZIP_REQUIRED_PATH', 'src/main/resources/data.sql', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
