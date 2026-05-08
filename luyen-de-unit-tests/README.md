# Unit Tests - Chuc nang Luyen de & Kiem tra truc tuyen

## Tong quan

Thu muc tong hop unit test cho chuc nang **Luyen de cua Student** trong TOEIC Exam Backend.

| Chi so | Gia tri |
|---|---|
| Module | ExamService + AttemptService |
| Tong so Test Case | 59 |
| Pass | 48 |
| Fail (Phat hien Bug) | 11 |
| Framework | Jest + ts-jest |
| Bao cao | `reports/Bao_Cao_Unit_Test_LuyenDe.md` |

Trong bo test nay, cac testcase **Fail** duoc dung de danh dau bug/defect theo hanh vi ky vong cua he thong, khong phai loi viet unit test.

## Cau truc thu muc

```text
luyen-de-unit-tests/
├── tests/
│   ├── exam.service.spec.ts
│   └── attempt.service.spec.ts
├── reports/
│   └── Bao_Cao_Unit_Test_LuyenDe.md
├── package.json
└── README.md
```

## File goc co the chay test

Ban copy trong thu muc nay dung de nop/luu tru theo chuc nang. Khi can chay test that, su dung file goc trong backend:

```text
TrinhHieu/toeic-exam-backend/src/application/services/__tests__/exam.service.spec.ts
TrinhHieu/toeic-exam-backend/src/application/services/__tests__/attempt.service.spec.ts
```

## Chay test

```bash
npm test
```

Lenh tren se chay Jest tu thu muc `TrinhHieu/toeic-exam-backend` voi 2 file spec cua chuc nang luyen de.
