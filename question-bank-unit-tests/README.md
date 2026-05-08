# Unit Tests - Chuc nang Quan ly ngan hang cau hoi

## Tong quan

Thu muc tong hop unit test cho chuc nang **Quan ly ngan hang cau hoi** trong TOEIC Exam Backend.

| Chi so | Gia tri |
|---|---|
| Module | QuestionService + MediaGroupService |
| Framework | Jest + ts-jest |
| Bao cao cau hoi | `reports/Bao_Cao_Unit_Test_Question.md` |
| Bao cao media group | `reports/Bao_Cao_Unit_Test_MediaGroup.md` |

## Cau truc thu muc

```text
question-bank-unit-tests/
├── tests/
│   ├── question.service.spec.ts
│   └── media-group.service.spec.ts
├── reports/
│   ├── Bao_Cao_Unit_Test_Question.md
│   └── Bao_Cao_Unit_Test_MediaGroup.md
├── package.json
└── README.md
```

## File goc co the chay test

Ban copy trong thu muc nay dung de nop/luu tru theo chuc nang. Khi can chay test that, su dung file goc trong backend:

```text
TrinhHieu/toeic-exam-backend/src/application/services/__tests__/question.service.spec.ts
TrinhHieu/toeic-exam-backend/src/application/services/__tests__/media-group.service.spec.ts
```

## Chay test

```bash
npm test
```

Lenh tren se chay Jest tu thu muc `TrinhHieu/toeic-exam-backend` voi 2 file spec cua chuc nang quan ly ngan hang cau hoi.
