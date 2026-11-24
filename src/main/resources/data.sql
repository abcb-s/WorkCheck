INSERT INTO member (email, password, name, created_at, updated_at)
VALUES ('demo@workcheck.com', '$2a$10$zsHRG7P8RmwXtv8fxbNBmOk3cWbh5/d9J5MWfKxdLY/bythOZtNbe', '데모 학생', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO assignment (title, member_id, created_at, updated_at)
VALUES ('과학 탐구 보고서', (SELECT member_id FROM member WHERE email = 'demo@workcheck.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('자바 프로젝트', (SELECT member_id FROM member WHERE email = 'demo@workcheck.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO check_rule (assignment_id, file_type, rule_type, rule_value, created_at, updated_at)
VALUES ((SELECT assignment_id FROM assignment WHERE title = '과학 탐구 보고서'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        'PDF', 'FILE_NAME_SUFFIX', '_보고서.pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT assignment_id FROM assignment WHERE title = '과학 탐구 보고서'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        'PDF', 'PDF_REQUIRED_TEXT', '참고문헌', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT assignment_id FROM assignment WHERE title = '자바 프로젝트'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        'ZIP', 'FILE_NAME_SUFFIX', '_프로젝트.zip', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT assignment_id FROM assignment WHERE title = '자바 프로젝트'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        'ZIP', 'ZIP_REQUIRED_PATH', 'src/main/resources/data.sql', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO check_history (assignment_id, file_name, status, created_at, updated_at)
VALUES ((SELECT assignment_id FROM assignment WHERE title = '과학 탐구 보고서'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        '김데모_보고서_초안.pdf', 'FAIL', DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
       ((SELECT assignment_id FROM assignment WHERE title = '과학 탐구 보고서'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        '김데모_보고서.pdf', 'PASS', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP)),
       ((SELECT assignment_id FROM assignment WHERE title = '자바 프로젝트'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        '김데모_프로젝트.zip', 'FAIL', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP), DATEADD('MINUTE', -30, CURRENT_TIMESTAMP));

INSERT INTO check_result (check_history_id, rule_type, expected, status, message, created_at, updated_at)
VALUES ((SELECT check_history_id FROM check_history WHERE file_name = '김데모_보고서_초안.pdf'),
        'FILE_NAME_SUFFIX', '_보고서.pdf', 'FAIL', '파일 이름이 _보고서.pdf 형식으로 끝나지 않아요.',
        DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
       ((SELECT check_history_id FROM check_history WHERE file_name = '김데모_보고서_초안.pdf'),
        'PDF_REQUIRED_TEXT', '참고문헌', 'FAIL', 'PDF에서 ''참고문헌'' 문구를 찾지 못했어요.',
        DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
       ((SELECT check_history_id FROM check_history WHERE file_name = '김데모_보고서.pdf'),
        'FILE_NAME_SUFFIX', '_보고서.pdf', 'PASS', '파일 이름이 _보고서.pdf 형식으로 끝나요.',
        DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP)),
       ((SELECT check_history_id FROM check_history WHERE file_name = '김데모_보고서.pdf'),
        'PDF_REQUIRED_TEXT', '참고문헌', 'PASS', 'PDF에서 ''참고문헌'' 문구를 찾았어요.',
        DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP)),
       ((SELECT check_history_id FROM check_history WHERE file_name = '김데모_프로젝트.zip'),
        'FILE_NAME_SUFFIX', '_프로젝트.zip', 'PASS', '파일 이름이 _프로젝트.zip 형식으로 끝나요.',
        DATEADD('MINUTE', -30, CURRENT_TIMESTAMP), DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)),
       ((SELECT check_history_id FROM check_history WHERE file_name = '김데모_프로젝트.zip'),
        'ZIP_REQUIRED_PATH', 'src/main/resources/data.sql', 'FAIL', '압축 파일에서 src/main/resources/data.sql 경로를 찾지 못했어요.',
        DATEADD('MINUTE', -30, CURRENT_TIMESTAMP), DATEADD('MINUTE', -30, CURRENT_TIMESTAMP));

INSERT INTO assignment (title, member_id, created_at, updated_at)
VALUES ('데모 ZIP 과제', (SELECT member_id FROM member WHERE email = 'demo@workcheck.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO check_rule (assignment_id, file_type, rule_type, rule_value, created_at, updated_at)
VALUES ((SELECT assignment_id FROM assignment WHERE title = '데모 ZIP 과제'
         AND member_id = (SELECT member_id FROM member WHERE email = 'demo@workcheck.com')),
        'ZIP', 'FILE_NAME_SUFFIX', '.zip', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
