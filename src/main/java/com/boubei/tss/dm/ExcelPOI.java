/* ==================================================================
 * Created [2018-06-22] by Jon.King
 * ==================================================================
 * TSS
 * ==================================================================
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018
 * ==================================================================
 */

package com.boubei.tss.dm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.boubei.tss.PX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

/**
 * 1.org.apache.poi.ss.usermodel.Workbook 对应Excel文档；
 * 　　2.org.apache.poi.hssf.usermodel.HSSFWorkbook  对应xls格式的Excel文档； 65536
 * 　　3.org.apache.poi.xssf.usermodel.XSSFWorkbook  对应xlsx格式的Excel文档；1048576
 * 　　4.org.apache.poi.ss.usermodel.Sheet  对应Excel文档中的一个sheet；
 * 　　5.org.apache.poi.ss.usermodel.Row    对应一个sheet中的一行；
 * 　　6.org.apache.poi.ss.usermodel.Cell   对应一个单元格。
 */
public class ExcelPOI extends Excel {

    protected String _csv2Excel(String sourceFile, String charSet) {
        File csvFile = new File(sourceFile);
        String csvName = FileHelper.getFileNameNoSuffix(csvFile.getName());

        String targetFile = DataExport.getExportPath() + "/" + csvName + XLSX_FIX; // POI 一律输出 XLSX;
        Workbook wb = null;
        FileOutputStream ios = null;
        try {
            wb = new SXSSFWorkbook(5000);  // 替代XSSFWorkbook，批量（满5000）输出到文件里，防止OOM
            Sheet ws = wb.createSheet(csvName);

            String dataStr = FileHelper.readFile(csvFile, charSet);
            String[] rows = EasyUtils.split(dataStr, "\n");

            for (int i = 0; i < rows.length; i++) {
                String[] rowData = rows[i].split(",");
                Row row = ws.createRow(i);

                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j, CellType.STRING);
                    cell.setCellValue(rowData[j]);
                }
            }
            wb.write(ios = new FileOutputStream(targetFile));
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
        } finally {
            try {
                wb.close();
            } catch (Exception e) {
            }
            try {
                ios.close();
            } catch (Exception e) {
            }
        }

        return targetFile;
    }

    protected Map<String, Object> readExcel(String filepath) {
        List<String> headers = new ArrayList<String>();
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        InputStream is = null;
        Workbook wb = null;
        try {
            is = new FileInputStream(filepath);
            boolean isXLS = isXLS(filepath);

            checkExcelSize(is, isXLS);

            ZipSecureFile.setMinInflateRatio(-1.0d);
            wb = isXLS ? new HSSFWorkbook(is) : new XSSFWorkbook(is);

            int index = 0;  //HIDDEN,VISIBLE
            while (HIDDEN.equals(wb.getSheetVisibility(index).toString())) {
                index++;
            }

            Sheet sheet1 = wb.getSheetAt(index);   // 获取第一张Sheet表
            Row row0 = sheet1.getRow(0);      // 获取第一行

            int rsColumns = row0.getPhysicalNumberOfCells();  // 获取Sheet表中所包含的总列数
            int rsRows = sheet1.getPhysicalNumberOfRows();   // 获取Sheet表中所包含的总行数

            for (int j = 0; j < rsColumns; j++) {
                Cell cell = row0.getCell(j);
                String value = getCellVal(cell, 0, j);
                headers.add(value);
            }
            EasyUtils.fixRepeat(headers);

            // 获取指定单元格的对象引用
            for (int i = 1; i <= rsRows; i++) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                Row _row = sheet1.getRow(i);
                if (_row == null) continue;

                for (int j = 0; j < rsColumns; j++) {
                    Cell cell = _row.getCell(j);
                    String value = getCellVal(cell, i, j);
                    row.put(headers.get(j), value);
                }

                data.add(row);
            }
        } catch (Exception e) {
            throw new BusinessException("readExcel error: " + e.getMessage(), e.getCause());
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
            try {
                wb.close();
            } catch (Exception e) {
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("data", data);
        result.put("headers", headers);
        return result;
    }

    public static void checkExcelSize(InputStream is, boolean isXLS) throws IOException {
        int MAX_XLSX_SIZE = EasyUtils.obj2Int(ParamConfig.getAttribute(PX.MAX_XLSX_SIZE, "1024"));
        MAX_XLSX_SIZE = Math.max(MAX_XLSX_SIZE, 256);

        int max_size = 1024 * MAX_XLSX_SIZE * (isXLS ? 5 : 1);
        if (is.available() > max_size) { // 1M xlsx 约等于 1万行*20列
            throw new BusinessException("导入文件过大，已超过" + Math.round(max_size * 10.0 / 1024 / 1024) / 10.0 + "M，请将数据分开多次导入");
        }
    }

    public static String getCellVal(Cell cell, int i, int j) {
        if (cell == null) return "";

        try {
            switch (cell.getCellTypeEnum()) { // 判断cell类型
                case NUMERIC:
                    // 判断cell是否为日期格式
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        Date dateCellVal = cell.getDateCellValue();
                        return DateUtil.formatCare2Second(dateCellVal);
                    } else { // 数字,常规类型的数字会自动多出 .0（因转换后是double类型），需要格式化掉
                        double cellVal = cell.getNumericCellValue();

                        NumberFormat f = NumberFormat.getInstance();
                        f.setMaximumFractionDigits(8); // 最多保留8位小数
                        String val = f.format(cellVal);

                        return val.replace(",", "");
                    }
                case FORMULA:
                    if (cell instanceof XSSFCell) {
                        return ((XSSFCell) cell).getCTCell().getV();
                    } else {
                        return ((HSSFCell) cell).getCellFormula();
                    }
                case STRING:
                    return cell.getStringCellValue();
                default:
                    return cell.toString();
            }
        } catch (Exception e) {
            throw new BusinessException("Excel.getCellVal error, location = [" + i + "," + j + "], cell = " + cell, e);
        }
    }
}
