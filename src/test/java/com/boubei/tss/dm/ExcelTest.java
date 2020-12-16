package com.boubei.tss.dm;

import java.io.File;
import java.io.FileInputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

import junit.framework.Assert;

public class ExcelTest {

    String dir = DataExport.getExportPath();
    File csvFile;
    StringBuffer content;

    @Before
    public void setUp() {

        // 1、create csv
        csvFile = FileHelper.createFile(dir + "/inv.csv");

        content = new StringBuffer();
        content.append("ID,日期,日期时间,字符串,身份证,金额").append("\n");
        content.append("1,2018-06-26,2018-06-26 13:59:00,我只一句中文1,00332624198305124211,10011.8").append("\n");
        content.append("2,2018-06-27,2018-06-27 13:59:00,我只一句中文2,00332624198305124211,10011.8").append("\n");
        content.append("3,2018-06-28,2018-06-28 13:59:00,我只一句中文3,00332624198305124211,10013.8").append("\n");

        FileHelper.writeFile(csvFile, content.toString());
        Assert.assertEquals(content.toString(), FileHelper.readFile(csvFile));  // 后台写文件默认 UTF-8
    }

    @Test
    public void testXLSX() {

        Excel.instance = new ExcelPOI();

        // 2、csv --> excel
        ExcelPOI.csv2Excel(csvFile.getPath()); // inv.xls 乱码
        String xlsFilePath = Excel.csv2Excel(csvFile.getPath(), DataExport.CSV_UTF8);
        Assert.assertTrue(Excel.isXLSX(xlsFilePath));

        // 3、excel --> csv
        String csvFilePath = Excel.excel2CSV(xlsFilePath); // 默认以GBK输出
        Assert.assertTrue(Excel.isCSV(csvFilePath));

        Assert.assertEquals(content.toString(), FileHelper.readFile(new File(csvFilePath), DataExport.SYS_CHAR_SET));

        DataExport.downloadFileByHttp(new MockHttpServletResponse(), csvFilePath, DataExport.SYS_CHAR_SET, false);
        DataExport.downloadFileByHttp(new MockHttpServletResponse(), csvFilePath, DataExport.SYS_CHAR_SET, true);

        Assert.assertEquals("", ExcelPOI.getCellVal(null, 1, 1));

        // test error
        Excel.csv2Excel(dir + "/" + System.currentTimeMillis() + ".csv", DataExport.CSV_UTF8);
        try {
            Excel.excel2CSV(dir + "/inv_2.xlsx");
            Assert.fail("should throw ex");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().indexOf("No such file") > 0);
        }

        String path = URLUtil.getResourceFileUrl("testdata/test.xlsx").getPath();
        path = ExcelPOI.excel2CSV(path);
        System.out.println(FileHelper.readFile(new File(path), DataExport.CSV_GBK));
    }

    @Test
    public void testXLS() {
        // 2、csv --> xls
        Excel.instance = new ExcelJXL();

        String xlsFilePath = Excel.csv2Excel(csvFile.getPath(), DataExport.CSV_UTF8);
        Assert.assertTrue(Excel.isXLS(xlsFilePath));

        // 3、excel --> csv
        Excel.instance = new ExcelPOI();

        String csvFilePath = Excel.excel2CSV(xlsFilePath); // 默认以GBK输出
        Assert.assertTrue(Excel.isCSV(csvFilePath));

        Assert.assertEquals(content.toString(), FileHelper.readFile(new File(csvFilePath), DataExport.CSV_GBK));

        try {
            HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(xlsFilePath));
            HSSFCell cell = wb.getSheetAt(0).getRow(1).createCell(10, CellType.FORMULA);
            Assert.assertEquals("", ExcelPOI.getCellVal(cell, 1, 1));
            Assert.fail();
            wb.close();
        } catch (Exception e1) {
        }
    }

    @Test
    public void testXLS_JXL() {

        Excel.instance = new ExcelJXL();

        // 2、csv --> excel
        Excel.csv2Excel(csvFile.getPath()); // inv.xls 乱码
        String xlsFilePath = Excel.csv2Excel(csvFile.getPath(), DataExport.CSV_UTF8);
        Assert.assertTrue(Excel.isXLS(xlsFilePath));

        // 3、excel --> csv
        String csvFilePath = Excel.excel2CSV(xlsFilePath); // 默认以GBK输出
        Assert.assertTrue(Excel.isCSV(csvFilePath));

        Assert.assertEquals(content.toString(), FileHelper.readFile(new File(csvFilePath), DataExport.CSV_GBK));

        DataExport.downloadFileByHttp(new MockHttpServletResponse(), csvFilePath, DataExport.SYS_CHAR_SET, false);
        DataExport.downloadFileByHttp(new MockHttpServletResponse(), csvFilePath, DataExport.SYS_CHAR_SET, true);

        // test error
        Excel.csv2Excel(dir + "/" + System.currentTimeMillis() + ".csv", DataExport.CSV_UTF8);
        Excel.excel2CSV(dir + "/inv_2.xls");

        String path = URLUtil.getResourceFileUrl("testdata/test.xls").getPath();
        path = Excel.excel2CSV(path);
        System.out.println(FileHelper.readFile(new File(path), DataExport.CSV_GBK));

        Excel.instance = new ExcelPOI();
    }

    @Test
    public void testXLSX2() {
        // 测试Excel编码
        String path = URLUtil.getResourceFileUrl("testdata/test_gbk.xlsx").getPath();
        path = Excel.excel2CSV(path);
        System.out.println(FileHelper.readFile(new File(path), DataExport.CSV_GBK));

        // 测试Excel公式解析
        path = URLUtil.getResourceFileUrl("testdata/test_sales.xlsx").getPath();
        path = Excel.excel2CSV(path);
        System.out.println(FileHelper.readFile(new File(path), DataExport.CSV_GBK));

        // 测试其它 Excel
//		path = URLUtil.getResourceFileUrl("testdata/1530782716166.xlsx").getPath();
//		path = Excel.excel2CSV(path);
//		System.out.println( FileHelper.readFile( new File(path), DataExport.CSV_GBK ) );
    }

    // 测试POI性能
    @Test
    public void testLargeData() {
        Logger.getLogger("com.boubei").setLevel(Level.INFO);

        StringBuffer content = new StringBuffer();
        content.append("ID,日期,日期时间,字符串,身份证,金额").append("\n");
        for (int i = 0; i < 5 * 10000; i++) {
            content.append(i + ",2018-06-26,2018-06-26 13:59:00,我只一句中文1,00332624198305124211,10011.8").append("\n");
        }
        File csvFile = FileHelper.createFile(dir + "/inv9.csv");
        FileHelper.writeFile(csvFile, content.toString());
        System.out.println("csvFile length = " + csvFile.length() + ", " + csvFile.getPath());

        Excel.instance = new ExcelPOI();

        // 1、csv --> excel 测试导出Excel性能
        long start = System.currentTimeMillis();
        String xlsFilePath = Excel.csv2Excel(csvFile.getPath(), DataExport.CSV_UTF8);
        Assert.assertTrue(Excel.isXLSX(xlsFilePath));
        System.out.println("csv --> excel cost: " + (System.currentTimeMillis() - start));
        System.out.println("xlsxFile length = " + new File(xlsFilePath).length() + ", " + xlsFilePath);

        // 3、excel --> csv 测试读取Excel性能
        try {
            start = System.currentTimeMillis();
            String csvFilePath = Excel.excel2CSV(xlsFilePath);
            Assert.assertTrue(Excel.isCSV(csvFilePath));
            System.out.println("excel --> csv cost: " + (System.currentTimeMillis() - start));
            Assert.fail("should throw ex");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().indexOf("导入文件过大") > 0);
        }
    }

    @Test
    public void testCellStyle() {

    }
}
