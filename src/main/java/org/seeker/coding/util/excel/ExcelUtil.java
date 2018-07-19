package org.seeker.coding.util.excel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

public class ExcelUtil {
	protected Logger logger = Logger.getLogger(this.getClass().getName());
	/**
	 * 将excelPo属性转换成与Excel对应列名称
	 * @param pojoClass
	 * @return
	 * @throws Exception
	 */
	public static Map getMapByClazz(Class pojoClass) throws Exception{
		Map<String, Map<String, Method>> map = new HashMap<String, Map<String, Method>>();//获得excelPo里面添加属性值的方法
		// 得到目标目标类的所有的字段列表
		Field filed[] = pojoClass.getDeclaredFields();
		// 将所有标有Annotation的字段，也就是允许导入数据的字段,放入到一个map中
		Map<String, Method> fieldSetMap = new HashMap<String, Method>();//获得excelPo里面添加属性值的方法
		Map<String, Method> fieldSetConvertMap = new HashMap<String, Method>();
		// 循环读取所有字段
		for (int i = 0; i < filed.length; i++) {
			Field f = filed[i];
			// 得到单个字段上的Annotation
			Excel excel = f.getAnnotation(Excel.class);
			// 如果标识了Annotationd的话
			if (excel != null) {
				// 构造设置了Annotation的字段的Setter方法
				String fieldname = f.getName();
				String setMethodName = "set" + fieldname.substring(0, 1).toUpperCase() + fieldname.substring(1);
				// 构造调用的method，
				Method setMethod = pojoClass.getMethod(setMethodName, new Class[] { f.getType() });
				// 将这个method以Annotaion的名字为key来存入。
				// 对于重名将导致 覆盖 失败，对于此处的限制需要
				fieldSetMap.put(excel.exportName(), setMethod);
				// 判断是否需要转换
				if (excel.importConvertSign() == 1) {
					// 由于直接使用了数据库绑定的Entity对象，注入会有冲突
					StringBuffer setConvertMethodName = new StringBuffer("convertSet");
					setConvertMethodName.append(fieldname.substring(0, 1).toUpperCase());
					setConvertMethodName.append(fieldname.substring(1));
					Method getConvertMethod = pojoClass.getMethod(setConvertMethodName.toString(), new Class[] { String.class });
					fieldSetConvertMap.put(excel.exportName(), getConvertMethod);
				}
			}
		}
		map.put("fieldSetConvertMap",fieldSetConvertMap);
		map.put("fieldSetMap",fieldSetMap);
		return map;
	}
	public static Map<Integer,String> getTitleByRow(Row title) throws Exception{
		// 将标题的文字内容放入到一个map中。
		Map<Integer,String> titlemap = new HashMap<Integer,String>();
		// 得到第一行的所有列
		Iterator<Cell> cellTitle = title.cellIterator();
		// 从标题第一列开始
		int i = 0;
		// 循环标题所有的列
		while (cellTitle.hasNext()) {
			Cell cell = cellTitle.next();
			String value = cell.getStringCellValue();
			titlemap.put(i, value);
			i = i + 1;
		}
		return titlemap;
	}
	
	public static Map<Integer,String> getValueByRow(Row row,int col) throws Exception{
		// 将标题的文字内容放入到一个map中。
		Map<Integer,String> valMap = new HashMap<Integer,String>();
		// 得到第一行的所有列
		Iterator<Cell> cellTitle = row.cellIterator();
		// 从标题第一列开始
		int i = 1;
		// 循环标题所有的列
		while (cellTitle.hasNext()&&i<=col) {
			Cell cell = cellTitle.next();
			String value = cell.getStringCellValue();
			valMap.put(i, value);
			i = i + 1;
		}
		return valMap;
	}
	
	public static Object getEntityByRow(Object tObject,Map map,Row row) throws Exception{
		Map<String, Method> fieldSetMap = (Map<String, Method>) map.get("fieldSetMap");//获得excelPo里面添加属性值的方法
		Map<String, Method> fieldSetConvertMap = (Map<String, Method>) map.get("fieldSetConvertMap");//是否需要转化 应该没有用
		Map<Integer, String> titlemap = (Map<Integer, String>) map.get("titlemap");//是否需要转化 应该没有用
		// 标题下的第一行
		Iterator<Cell> cellbody = row.cellIterator();
//		int k = row.getLastCellNum();
//		System.out.println(row.getRowNum());
//		System.out.println(row.getFirstCellNum());
//		System.out.println(row.getLastCellNum());
		
		for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
			if(row.getCell(i)==null||"".equals(row.getCell(i).toString())){
				continue;
			}
//		}
		// 遍历一行的列
//		while (cellbody.hasNext()) {
//			Cell cell = cellbody.next();
			Cell cell = row.getCell(i);
			// 这里得到此列的对应的标题
			String titleString = titlemap.get(i);
			// 如果这一列的标题和类中的某一列的Annotation相同，那么则调用此类的的set方法，进行设值
			if (fieldSetMap.containsKey(titleString)) {
				Method setMethod = (Method) fieldSetMap.get(titleString);
				// 得到setter方法的参数
				Type[] ts = setMethod.getGenericParameterTypes();
				// 只要一个参数
				String xclass = ts[0].toString();
				// 判断参数类型
				if (Cell.CELL_TYPE_STRING == cell.getCellType() && fieldSetConvertMap.containsKey(titleString)) {
					fieldSetConvertMap.get(titleString).invoke(tObject, cell.getStringCellValue());
				} else {
					if (xclass.equals("class java.lang.String")) {
						String cellValue = "";
						cell.setCellType(Cell.CELL_TYPE_STRING);
						cellValue = cell.getStringCellValue();
						// 先设置Cell的类型，然后就可以把纯数字作为String类型读进来了：
						setMethod.invoke(tObject, cellValue);
					} else if (xclass.equals("class java.util.Date")) {
						Date cellDate = null;
						if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
							// 日期格式
							cellDate = cell.getDateCellValue();
						} else { // 全认为是 Cell.CELL_TYPE_STRING: 如果不是
							// yyyy-mm-dd hh:mm:ss 的格式就不对(wait to
							// do:有局限性)
							cellDate = stringToDate(cell.getStringCellValue());
						}
						setMethod.invoke(tObject, cellDate);
					} else if (xclass.equals("class java.lang.Boolean")) {
						boolean valBool;
						if (Cell.CELL_TYPE_BOOLEAN == cell.getCellType()) {
							valBool = cell.getBooleanCellValue();
						} else {// 全认为是 Cell.CELL_TYPE_STRING
							valBool = cell.getStringCellValue().equalsIgnoreCase("true") || (!cell.getStringCellValue().equals("0"));
						}
						setMethod.invoke(tObject, valBool);
					} else if (xclass.equals("class java.lang.Integer")) {
						Integer valInt;
						if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
							valInt = (new Double(cell.getNumericCellValue())).intValue();
						}
						// lijin======================
						else if (Cell.CELL_TYPE_FORMULA == cell.getCellType()) {
							cell.setCellType(Cell.CELL_TYPE_NUMERIC);
							valInt = (new Double(cell.getNumericCellValue())).intValue();
						} else {// 全认为是 Cell.CELL_TYPE_STRING
							valInt = new Integer(cell.getStringCellValue());
						}
						setMethod.invoke(tObject, valInt);
					} else if (xclass.equals("class java.lang.Long")) {
						Long valLong;
						if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
							valLong = (new Double(cell.getNumericCellValue())).longValue();
						} else {// 全认为是 Cell.CELL_TYPE_STRING
							valLong = new Long(cell.getStringCellValue());
						}
						setMethod.invoke(tObject, valLong);
					} else if (xclass.equals("class java.lang.Double")) {
						Double valDouble;
						if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
							valDouble = (new Double(cell.getNumericCellValue())).doubleValue();
						} else if (Cell.CELL_TYPE_FORMULA == cell.getCellType()) {
							cell.setCellType(Cell.CELL_TYPE_NUMERIC);
							valDouble = (new Double(cell.getNumericCellValue())).doubleValue();
						} else {// 全认为是 Cell.CELL_TYPE_STRING
							valDouble = new Double(cell.getStringCellValue());
						}
						setMethod.invoke(tObject, valDouble);
					}

					else if (xclass.equals("class java.math.BigDecimal")) {
						BigDecimal valDecimal;
						if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
							valDecimal = new BigDecimal(cell.getNumericCellValue());
						} else {// 全认为是 Cell.CELL_TYPE_STRING
							valDecimal = new BigDecimal(cell.getStringCellValue());
						}
						setMethod.invoke(tObject, valDecimal);
					}
				}
			}
			// 下一列
//			k = k + 1;
		}
		return tObject;
	}
	/**
	 * 字符串转换为Date类型数据（限定格式 YYYY-MM-DD hh:mm:ss）或（YYYY/MM/DD hh:mm:ss）
	 * 
	 * @param cellValue
	 *            : 字符串类型的日期数据
	 * @return
	 */
	private static Date stringToDate(String cellValue) {
		if (cellValue.length() > 19)
			cellValue = cellValue.substring(0, 19);
		Calendar calendar = Calendar.getInstance();
		String[] dateStr = cellValue.split(" ");
		String[] dateInfo = dateStr[0].split("-");
		if (dateInfo.length != 3) {
			dateInfo = dateStr[0].split("/"); // 让 yyyy/mm/dd 的格式也支持
		}
		if (dateInfo.length == 3) {
			int year = Integer.parseInt(dateInfo[0]);
			int month = Integer.parseInt(dateInfo[1]) - 1; // 0~11
			int day = Integer.parseInt(dateInfo[2]);
			calendar.set(year, month, day);
		} else {
			return null; // 格式不正确
		}
		if (dateStr.length > 1) {// 有时间（限定格式 hh:mm:ss）
			String[] timeStr = dateStr[1].split(":");
			if (timeStr.length == 3) {
				int hour = Integer.parseInt(timeStr[0]);
				int minute = Integer.parseInt(timeStr[1]);
				int second = Integer.parseInt(timeStr[2]);
				calendar.set(Calendar.HOUR_OF_DAY, hour);
				calendar.set(Calendar.MINUTE, minute);
				calendar.set(Calendar.SECOND, second);
			} else {
				return null; // 格式不正确
			}
		}
		return calendar.getTime();
	}

}
