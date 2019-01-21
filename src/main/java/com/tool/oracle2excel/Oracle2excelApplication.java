package com.tool.oracle2excel;

import com.tool.oracle2excel.pojo.Field;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Table;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;

@SpringBootApplication
public class Oracle2excelApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(Oracle2excelApplication.class, args);
    }


    @PostConstruct
    public void getSchema() throws IOException {
        List<Field> fields = jdbcTemplate.query("select *\n" +
                "from user_tab_columns\n" +
                "where table_name in (select TABLE_NAME from user_tables) " +
                "ORDER BY TABLE_NAME,COLUMN_ID", new RowMapper<Field>() {
            @Override
            public Field mapRow(ResultSet rs, int rowNum) throws SQLException {
                Field t = new Field();
                t.setTableName(rs.getString("TABLE_NAME"));
                t.setFieldName(rs.getString("COLUMN_NAME"));
                t.setFieldType(rs.getString("DATA_TYPE"));
                t.setFieldLength(rs.getInt("DATA_LENGTH"));
                t.setFieldIndex(rs.getInt("COLUMN_ID"));
                t.setFieldDesc("");
                return t;
            }
        });


        Map<String, List<Field>> map = convert2Map(fields);


        convert2Excel(map);
    }

    private Map<String, List<Field>> convert2Map(List<Field> fields) {
        Map<String, List<Field>> map = new HashMap<>();

        for (Field field : fields) {
            String tableName = field.getTableName();

            if (map.containsKey(tableName)) {
                List<Field> fieldList = map.get(tableName);
                fieldList.add(field);
            } else {
                List<Field> fieldList = new ArrayList<>();
                fieldList.add(field);
                map.put(tableName, fieldList);
            }

        }

        return map;
    }

    private void createSheetHead(Sheet sheet) {
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("字段名称");
        row.createCell(1).setCellValue("字段类型");
        row.createCell(2).setCellValue("字段长度");
        row.createCell(3).setCellValue("含义");
    }

    private void createSheetBody(Sheet sheet, List<Field> fields) {

        for (Field field : fields) {
            Row row = sheet.createRow(field.getFieldIndex());
            row.createCell(0).setCellValue(field.getFieldName());
            row.createCell(1).setCellValue(field.getFieldType());
            row.createCell(2).setCellValue(field.getFieldLength());
            row.createCell(3).setCellValue(field.getFieldDesc());
        }

    }

    public void convert2Excel(Map<String, List<Field>> map) throws IOException {
        Workbook wb = new XSSFWorkbook();


        for (Map.Entry<String, List<Field>> entry : map.entrySet()) {
            String sheetName = entry.getKey();
            List<Field> fields = entry.getValue();
            Sheet sheet = wb.createSheet(sheetName);

            createSheetHead(sheet);
            createSheetBody(sheet, fields);
        }


        FileOutputStream fileOutputStream = new FileOutputStream("test.xlsx");
        wb.write(fileOutputStream);

    }

}

