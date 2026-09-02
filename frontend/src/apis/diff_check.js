const fs = require('fs');
const path = require('path');

// 设置文件路径
const stylePath = 'src/apis';
const interfacePath = 'e:/合件/智能会议室管理系统-接口版/src/apis';

// 获取所有JS文件
const files = fs.readdirSync(stylePath).filter(f => f.endsWith('.js'));

// 存储差异结果
const diffResults = [];

// 遍历文件进行比较
files.forEach(file => {
  try {
    const styleContent = fs.readFileSync(path.join(stylePath, file), 'utf8');
    const interfaceContent = fs.readFileSync(path.join(interfacePath, file), 'utf8');
    
    if (styleContent !== interfaceContent) {
      // 计算差异行数
      const styleLines = styleContent.split('\n');
      const interfaceLines = interfaceContent.split('\n');
      
      // 简单的差异检测
      const diffLines = [];
      const maxLines = Math.max(styleLines.length, interfaceLines.length);
      
      for (let i = 0; i < maxLines; i++) {
        const styleLine = styleLines[i] || '';
        const interfaceLine = interfaceLines[i] || '';
        
        if (styleLine !== interfaceLine) {
          diffLines.push({
            line: i + 1,
            style: styleLine,
            interface: interfaceLine
          });
        }
      }
      
      diffResults.push({
        file: file,
        totalLines: maxLines,
        diffCount: diffLines.length,
        diffLines: diffLines
      });
    }
  } catch (error) {
    console.error(`比较文件 ${file} 时出错:`, error.message);
  }
});

// 生成报告
console.log('=== API一致性检查报告 ===');
console.log(`共检查 ${files.length} 个API文件`);
console.log(`发现 ${diffResults.length} 个文件存在差异\n`);

diffResults.forEach(result => {
  console.log(`\n--- 文件: ${result.file} ---`);
  console.log(`总行数: ${result.totalLines}`);
  console.log(`差异行数: ${result.diffCount}`);
  console.log('差异详情:');
  
  result.diffLines.forEach(diff => {
    console.log(`行 ${diff.line}:`);
    if (diff.style) {
      console.log(`  样式版: ${diff.style}`);
    }
    if (diff.interface) {
      console.log(`  接口版: ${diff.interface}`);
    }
  });
  
  console.log('-----------------------');
});

// 生成JSON报告
const reportJson = {
  totalFiles: files.length,
  diffFiles: diffResults.length,
  results: diffResults
};

fs.writeFileSync('api_consistency_report.json', JSON.stringify(reportJson, null, 2), 'utf8');
console.log('\n报告已保存到 api_consistency_report.json');