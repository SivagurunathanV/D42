import net.sf.jett.transform.ExcelTransformer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.model.geom.Context;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sivagurunathan.v on 16/06/16.
 */
public class ExcelService {
  public static Workbook workSheet = new XSSFWorkbook();
  public static Sheet sheet;

  public static Workbook createFromTemplateUsingObject() throws IOException {
	ExcelTransformer excelTransformer = new ExcelTransformer();
	sheet = workSheet.createSheet();
	Map<String, Object> bean = new HashMap<>();
	Employee employee = new Employee();
	for (int i = 0; i < 10; i++) {
	  Row row = sheet.createRow(i);
	  Cell cell = row.createCell(0);
	  cell.setCellValue("${name" + i + "}");
	  Cell cell1 = row.createCell(1);
	  cell1.setCellValue("${gender" + i + "}");
	  Cell cell2 = row.createCell(2);
	  cell2.setCellValue("${number" + i + "}");
	  employee.setGender("Male");
	  employee.setName("siva");
	  employee.setNumber(i);
	  bean.put("name" + i, employee.getName());
	  bean.put("gender" + i, employee.getGender());
	  bean.put("number" + i, employee.getNumber());

	}
	excelTransformer.transform(workSheet, bean);
	return workSheet;
  }

}
