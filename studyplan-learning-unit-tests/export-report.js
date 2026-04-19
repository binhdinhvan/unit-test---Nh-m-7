const fs = require('fs');
const path = require('path');
const ExcelJS = require('exceljs');

async function createExcel() {
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet('Unit Test Cases');
  
  sheet.columns = [
    { header: 'TestcaseID', key: 'id', width: 20 },
    { header: 'Chức năng/use case', key: 'usecase', width: 30 },
    { header: 'Lớp', key: 'class', width: 30 },
    { header: 'Phương thức', key: 'method', width: 40 },
    { header: 'Mục tiêu kiểm thử', key: 'objective', width: 50 },
    { header: 'Input (Dữ liệu mock)', key: 'input', width: 50 },
    { header: 'Expected output', key: 'expected', width: 50 },
    { header: 'Kết quả', key: 'result', width: 15 },
    { header: 'Ghi chú', key: 'notes', width: 40 }
  ];

  sheet.getRow(1).font = { bold: true, size: 11, color: { argb: 'FFFFFFFF' } };
  sheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF4472C4' } };

  // 14 failed tests extracted from Bug constraints
  const failedTests = [
    'TC_SPL_002', 'TC_SPL_006', 'TC_SPL_007', 'TC_SPL_008', 'TC_SPL_009',
    'TC_SPL_010', 'TC_SPL_011', 'TC_SPL_013', 'TC_SPL_014', 'TC_SPL_015',
    'TC_SPL_016', 'TC_SPL_025', 'TC_SPL_026', 'TC_SPL_034'
  ];

  const content = fs.readFileSync(
    path.join(__dirname, '../MaiHieu/src/test/java/com/mxhieu/doantotnghiep/StudyPlanLearningFeatureTests.java'), 
    'utf8'
  );

  const regex = /\/\*\*\s*\n\s*\*\s*(TC_SPL_\d+):\s*([^\n]+)\s*\n[\s\S]*?Test Objective:\s*([^\n]+)\s*\n\s*\*\s*Input:\s*([^\n]+)\s*\n\s*\*\s*Expected Output:\s*([^\n]+)\s*\n(?:\s*\*\s*Notes:\s*([^\n]+)\s*\n)?/g;

  let count = 0;
  let match;
  const testCases = [];
  while ((match = regex.exec(content)) !== null) {
    const id = match[1].trim();
    const objective = match[3].trim();
    const input = match[4].trim();
    const expected = match[5].trim();
    const notes = match[6] ? match[6].trim() : 'N/A';
    
    const numId = parseInt(id.split('_')[2], 10);
    let usecase = 'Lập lịch học (StudyPlanService)';
    let clazz = 'StudyPlanServiceImpl';
    if(numId >= 25 && numId <= 31) { usecase = 'Học tập bải giảng (LessonProgress)'; clazz = 'LessonProgressServiceImpl'; }
    if(numId >= 32 && numId <= 38) { usecase = 'Điều hướng bài học (Lesson)'; clazz = 'LessonServiceImpl'; }
    if(numId >= 39) { usecase = 'Học tập Bài thi TOEIC'; clazz = 'TestProgressServiceImpl'; }
    
    const method = 'Tự động map từ Java';
    const result = failedTests.includes(id) ? 'Fail' : 'Pass';

    testCases.push({
      numId,
      rowData: {
        id,
        usecase,
        class: clazz,
        method,
        objective,
        input,
        expected,
        result,
        notes
      }
    });
  }

  testCases.sort((a, b) => a.numId - b.numId);

  testCases.forEach(tc => {
    sheet.addRow(tc.rowData);
    count++;
  });

  sheet.eachRow((row, rowNumber) => {
    row.eachCell((cell, colNumber) => {
      cell.border = { top: {style:'thin'}, left: {style:'thin'}, bottom: {style:'thin'}, right: {style:'thin'} };
      if (rowNumber > 1) {
        cell.alignment = { wrapText: true, vertical: 'top' };
        if (colNumber === 8) {
          if (cell.value === 'Fail') {
            cell.font = { color: { argb: 'FFFF0000' }, bold: true };
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFCE4EC' } };
          } else {
            cell.font = { color: { argb: 'FF00B050' }, bold: true };
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE8F5E9' } };
          }
        } else if (rowNumber % 2 === 0) {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF5F5F5' } };
        }
      } else {
        cell.alignment = { vertical: 'middle', horizontal: 'center' };
      }
    });
  });

  sheet.views = [{ state: 'frozen', ySplit: 1 }];
  const outputPath = 'Unit_Testing_Report_StudyPlanLearning.xlsx';
  await workbook.xlsx.writeFile(outputPath);
  console.log(`✅ Successfully created ${outputPath} with ${count} test cases.`);
}

createExcel().catch(console.error);
