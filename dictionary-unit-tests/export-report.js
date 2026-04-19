const fs = require('fs');
const path = require('path');
const ExcelJS = require('exceljs');

async function createExcel() {
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet('Unit Test Cases');
  
  sheet.columns = [
    { header: 'TestcaseID', key: 'id', width: 25 },
    { header: 'Chức năng/use case', key: 'usecase', width: 30 },
    { header: 'Lớp', key: 'class', width: 30 },
    { header: 'Phương thức', key: 'method', width: 30 },
    { header: 'Mục tiêu kiểm thử', key: 'objective', width: 45 },
    { header: 'Input (Dữ liệu mock)', key: 'input', width: 40 },
    { header: 'Expected output', key: 'expected', width: 40 },
    { header: 'Kết quả', key: 'result', width: 15 },
    { header: 'Ghi chú', key: 'notes', width: 30 }
  ];

  sheet.getRow(1).font = { bold: true, size: 11, color: { argb: 'FFFFFFFF' } };
  sheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF4472C4' } };

  // Fail cases (the 4 bug constraints)
  const failedTests = [
    'DICT_012_TC', 'DICT_012_TC_testSearch_EmptyAudioThrowsIndexOutOfBounds',
    'DICT_013_TC', 'DICT_013_TC_testSearch_MissingTextNodeThrowsNullPointer',
    'DICT_014_TC', 'DICT_014_TC_testSearch_EmptyResponseSavesEmptyEntity',
    'DICT_015_TC', 'DICT_015_TC_testSaveStudentDictionary_DuplicateFlashcard'
  ];

  const content = fs.readFileSync(
    path.join(__dirname, '../MaiHieu/src/test/java/com/mxhieu/doantotnghiep/DictionaryFeatureTests.java'), 
    'utf8'
  );

  const regex = /\/\*\*\s*\n\s*\*\s*(DICT_[A-Z0-9_]+_TC[a-zA-Z0-9_]*):\s*([^\n]+)\s*\n[\s\S]*?Test Objective:\s*([^\n]+)\s*\n\s*\*\s*Input:\s*([^\n]+)\s*\n\s*\*\s*Expected Output:\s*([^\n]+)\s*\n(?:\s*\*\s*Notes:\s*([^\n]+)\s*\n)?/g;

  let count = 0;
  let match;
  const testCases = [];
  while ((match = regex.exec(content)) !== null) {
    const fullId = match[1].trim(); 
    const objective = match[3].trim();
    const input = match[4].trim();
    const expected = match[5].trim();
    const notes = match[6] ? match[6].trim() : 'N/A';
    
    let numStr = fullId.match(/DICT_(\d+)_TC/);
    const numId = numStr ? parseInt(numStr[1], 10) : count;

    let usecase = 'Tra Từ Điển & API Cache';
    let clazz = 'DictionaryServiceImpl';
    if(numId >= 7 && numId <= 11 || numId === 15) { usecase = 'Lưu Flashcard Học Tập'; clazz = 'StudentDictionaryServiceImpl'; }
    
    const method = 'Search/Save Dictionary';
    const result = failedTests.includes(fullId) ? 'Fail' : 'Pass';

    testCases.push({
      numId,
      rowData: {
        id: fullId,
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

  const outputPath = 'Unit_Testing_Report_Dictionary.xlsx';
  await workbook.xlsx.writeFile(outputPath);
  console.log(`✅ Successfully created ${outputPath} with ${count} test cases.`);
}

createExcel().catch(console.error);
