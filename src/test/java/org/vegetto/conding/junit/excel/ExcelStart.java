package org.vegetto.conding.junit.excel;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.seeker.coding.excel.CodeExcel;
import org.seeker.coding.mark.MakerTool;
import org.seeker.coding.pdm.xml.MyTable;
import org.seeker.coding.pdm.xml.MyTables;
import org.seeker.coding.util.InitCodeConfig;
import org.seeker.coding.util.print.PrintUtils;

public class ExcelStart {
	@Test public void start() throws Exception{
//		String path="d:/";
//		String path=Start.class.getClass().getResource("/").getPath()+"coding"+File.separatorChar;
//		String savePath =  path+"exceltemp.xls";
//		System.out.println(savePath);
//		System.out.println(savePath);
		String savePath =  InitCodeConfig.excel_path;
		PrintUtils.S(savePath);
		File file = new File(savePath);
		FileInputStream in = new FileInputStream(file);
		MyTables tabs =CodeExcel.toExcelByIs(in);
		//3 生成Java类 Jsp页面 配置文件 
		PrintUtils.S("开始生成");
		Set pacakge_set = new HashSet();//用来记录一个包结构是否已经存在，以避免struts spring文件被覆盖（已存在的话，则在原来的文件中追加）
		for(MyTable mt : tabs.getTables()) {
			MakerTool.startCreate(mt, InitCodeConfig.jsp_path, pacakge_set.contains(mt.getPackagePath()));
			pacakge_set.add(mt.getPackagePath());
			PrintUtils.S("表>>>"+mt.getClassName());
		}
		PrintUtils.S("生成完毕，请查看>>>"+InitCodeConfig.jsp_path);
		PrintUtils.S("-=-=-==-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=--=-=-=-=-=");
	}


}
