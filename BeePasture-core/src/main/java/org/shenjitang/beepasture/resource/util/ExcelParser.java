/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource.util;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFErrorConstants;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//import com.richeninfo.anaplatform.dbmgr.service.BindDataLogic;
//import com.richeninfo.anaplatform.etl.utils.PoiSheetUtil;
//import com.richeninfo.anaplatform.etl.utils.RZDCElementConstants;
//import com.richeninfo.anaplatform.etl.webgather.tools.PathUtils;
//import com.richeninfo.anaplatform.gather.util.BaseUtils;
//import com.richeninfo.sgrid.util.ParseUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

/**
 *
 * @author xiaolie
 */
public class ExcelParser {
    private static long fileSizeLimit = 5*1024*1024L;
    public final static String GENERAL_FORMAT = "###0.############";
    public final static String DATE_FORMAT = "yyyy-MM-dd";
    public final static String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static Map<String, List<List<String>>> parseExcel(File excelFile, String[] pages) throws Exception {
        String extension = FilenameUtils.getExtension(excelFile.getPath());
        System.out.println("========" + excelFile + "         extension=" + extension);
        if ("xlsx".equalsIgnoreCase(extension)) { // 2007
            return parse2007(excelFile, pages);
        } else { // 97
            return parse97(excelFile, pages);
        }
    }

    public static Map<String, List<List<String>>> parse97(File excelFile, String[] pages) throws Exception {
        Map<String, List<List<String>>> map = new HashMap();
        FileInputStream is = new FileInputStream(excelFile);
        HSSFWorkbook wbs = new HSSFWorkbook(is);
        Set<String> pageSet = new HashSet();
        if (pages != null && pages.length > 0) {
            pageSet.addAll(Arrays.asList(pages));
        }
        try {
            int sheetQty = wbs.getNumberOfSheets();
            for (int i = 0; i < sheetQty; i++) {
                HSSFSheet sheet = wbs.getSheetAt(i);
                if (!pageSet.isEmpty()) {
                    if (!pageSet.contains(sheet.getSheetName())) {
                        continue;
                    }
                }

                List<List<String>> sheetMatrix = new ArrayList();
                for (int j = 0; j <= sheet.getLastRowNum(); j++) {
                    HSSFRow row = sheet.getRow(j);
                    if (null != row && row.getLastCellNum() > 0) {
                        List<String> rowArray = new ArrayList();
                        for (int k = 0; k < row.getLastCellNum(); k++) {
                            HSSFCell cell = row.getCell(k);
                            if (null != cell) {
//                                rowArray[k] = cell.getStringCellValue();
//                            		rowArray[k] = getCellText97(cell);
                            	//rowArray[k] = getContent(cell);
                                rowArray.add(getContent(cell));
                            } else {
                                rowArray.add("");
                            }
                        }
                        sheetMatrix.add(rowArray);
                        //sheetMatrix[j] = rowArray;
                    } else {
                        sheetMatrix.add(new ArrayList());
                        //sheetMatrix[j] = new String[0];
                    }
                }
                map.put(sheet.getSheetName(), sheetMatrix);
            }
        } finally {
            is.close();
        }
        return map;
    }




    public static Map<String, List<List<String>>> parse2007(File excelFile, String[] pages) throws Exception {
        Map<String, List<List<String>>> map = new HashMap();
        FileInputStream is = new FileInputStream(excelFile);
        XSSFWorkbook wbs = new XSSFWorkbook(is);
        Set<String> pageSet = new HashSet();
        if (pages != null && pages.length > 0) {
            pageSet.addAll(Arrays.asList(pages));
        }
        try {
            int sheetQty = wbs.getNumberOfSheets();
            for (int i = 0; i < sheetQty; i++) {
                XSSFSheet sheet = wbs.getSheetAt(i);
                if (!pageSet.isEmpty()) {
                    if (!pageSet.contains(sheet.getSheetName())) {
                        continue;
                    }
                }
                List<List<String>> sheetMatrix = new ArrayList();
                for (int j = 0; j <= sheet.getLastRowNum(); j++) {
                    XSSFRow row = sheet.getRow(j);
                    if (null != row && row.getLastCellNum() > 0) {
                        List<String> rowArray = new ArrayList();
                        for (int k = 0; k < row.getLastCellNum(); k++) {
                            XSSFCell cell = row.getCell(k);
                            if (null != cell) {
//                            	 if(cell.toString().endsWith(".0")) {
//                                     rowArray.add(cell.toString().substring(0,cell.toString().length()-2));
//                                 }else {
                                     rowArray.add(getContent(cell));
//                                 }
                            } else {
                                rowArray.add("");
                            }
                        }
                        sheetMatrix.add(rowArray);
                        //sheetMatrix[j] = rowArray;
                    } else {
                        sheetMatrix.add(new ArrayList());
                        //sheetMatrix[j] = new String[0];
                    }
                }
                map.put(sheet.getSheetName(), sheetMatrix);
            }
        } finally {
            is.close();
        }
        return map;
    }

	/**
	 * poi获取单元格数据
	 * @param cell
	 * @return
	 */
	private static String getContent(HSSFCell cell) {
		int cellType = cell.getCellType();
		String content = null;
		switch (cellType) {
			case HSSFCell.CELL_TYPE_STRING:
				content = cell.getRichStringCellValue().toString();
				break;
			case HSSFCell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					double d = cell.getNumericCellValue();
                    Date date = HSSFDateUtil.getJavaDate(d);
                    content = parseDate(date);
                } else {
					content = round(cell.getNumericCellValue(), null);
					if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString() == null) {
						Date date = cell.getDateCellValue();
                        content = parseDate(date);
					} else {
						if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString().contains("\\ [$-")) {
							Date date = cell.getDateCellValue();
                            content = parseDate(date);
						}
					}
				}
                break;
			case HSSFCell.CELL_TYPE_FORMULA:
				switch (cell.getCachedFormulaResultType()) {
					case HSSFCell.CELL_TYPE_NUMERIC:
						if (HSSFDateUtil.isCellDateFormatted(cell)) {
							double d = cell.getNumericCellValue();
							Date date = HSSFDateUtil.getJavaDate(d);
                            content = parseDate(date);
						} else {
							content = round(cell.getNumericCellValue(), null);
							if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString() == null) {
								Date date = cell.getDateCellValue();
                                content = parseDate(date);
							} else {
								if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString().contains("\\ [$-")) {
									Date date = cell.getDateCellValue();
                                    content = parseDate(date);
								}
							}
						}
						break;
					case HSSFCell.CELL_TYPE_STRING:
						content = cell.getRichStringCellValue().toString();
						break;
					case HSSFCell.CELL_TYPE_BOOLEAN:
						content = String.valueOf(cell.getBooleanCellValue());
						break;
					case HSSFCell.CELL_TYPE_ERROR:
						content = convertError(cell.getErrorCellValue());
						break;
				}
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN:
				content = String.valueOf(cell.getBooleanCellValue());
				break;
			case HSSFCell.CELL_TYPE_BLANK:
				content = "";
				break;
			case HSSFCell.CELL_TYPE_ERROR:
				byte errorCellValue = cell.getErrorCellValue();
				content = convertError(errorCellValue);
				break;
			default:
				content = "";
		}
		return content;
	}

    protected static String parseDate(Date date) {
        return new SimpleDateFormat("yyyyMMdd").format(date);
    }


	/**
	 * poi获取单元格数据
	 * @param cell
	 * @return
	 */
	private static String getContent(XSSFCell cell) {
		int cellType = cell.getCellType();
		String content = null;
		switch (cellType) {
			case XSSFCell.CELL_TYPE_STRING:
				content = cell.getRichStringCellValue().toString();
				break;
			case XSSFCell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					double d = cell.getNumericCellValue();
					Date date = HSSFDateUtil.getJavaDate(d);
                    content = parseDate(date);
				} else {
					content = round(cell.getNumericCellValue(), null);
					if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString() == null) {
						Date date = cell.getDateCellValue();
                        content = parseDate(date);
					} else {
						if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString().contains("\\ [$-")) {
							Date date = cell.getDateCellValue();
                            content = parseDate(date);
						}
					}
				}
				break;
			case XSSFCell.CELL_TYPE_FORMULA:
				switch (cell.getCachedFormulaResultType()) {
					case XSSFCell.CELL_TYPE_NUMERIC:
						if (HSSFDateUtil.isCellDateFormatted(cell)) {
							double d = cell.getNumericCellValue();
							Date date = HSSFDateUtil.getJavaDate(d);
                            content = parseDate(date);
						} else {
							content = round(cell.getNumericCellValue(), null);
							if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString() == null) {
								Date date = cell.getDateCellValue();
                                content = parseDate(date);
							} else {
								if (match(content, "^[0-9]*$") && cell.getCellStyle().getDataFormatString().contains("\\ [$-")) {
									Date date = cell.getDateCellValue();
                                    content = parseDate(date);
								}
							}
						}
						break;
					case XSSFCell.CELL_TYPE_STRING:
						content = cell.getRichStringCellValue().toString();
						break;
					case XSSFCell.CELL_TYPE_BOOLEAN:
						content = String.valueOf(cell.getBooleanCellValue());
						break;
					case XSSFCell.CELL_TYPE_ERROR:
						content = convertError(cell.getErrorCellValue());
						break;
				}
				break;
			case XSSFCell.CELL_TYPE_BOOLEAN:
				content = String.valueOf(cell.getBooleanCellValue());
				break;
			case XSSFCell.CELL_TYPE_BLANK:
				content = "";
				break;
			case XSSFCell.CELL_TYPE_ERROR:
				byte errorCellValue = cell.getErrorCellValue();
				content = convertError(errorCellValue);
				break;
			default:
				content = "";
		}
		return content;
	}


	/**
	 * 单元格数据读取error
	 * @param errorCellValue
	 * @return
	 */
	private static String convertError(byte errorCellValue) {
		String content = null;
		switch (errorCellValue) {
			case HSSFErrorConstants.ERROR_NULL:
				content = "#NULL!";
				break;
			case HSSFErrorConstants.ERROR_DIV_0:
				content = "#DIV/0!";
				break;
			case HSSFErrorConstants.ERROR_VALUE:
				content = "#VALUE!";
				break;
			case HSSFErrorConstants.ERROR_REF:
				content = "#REF!";
				break;
			case HSSFErrorConstants.ERROR_NAME:
				content = "#NAME?";
				break;
			case HSSFErrorConstants.ERROR_NUM:
				content = "#NUM!";
				break;
			case HSSFErrorConstants.ERROR_NA:
				content = "#N/A";
				break;
			default:
				content = "";
		}
		return content;
	}

    public static String getCellText97(HSSFCell cell) {
		if (cell == null) {
			return "";
		}
		//获取批注  XG注释
//		HSSFComment hssfcomment = cell.getCellComment();
//		if (hssfcomment != null) {
//			return hssfcomment.getString().toString();
//		}
		String content = "";
		int type = cell.getCellType();
		switch (type) {
		case HSSFCell.CELL_TYPE_STRING:
			content = cell.getRichStringCellValue().toString();
			break;
		case HSSFCell.CELL_TYPE_NUMERIC:
			double d = cell.getNumericCellValue();
            if (isDateTime(cell)) {
                content = getDateTimeContent(d);
            } else {
                if (HSSFDateUtil.isCellDateFormatted(cell)) {// 如果表单对象中是日期格式，对其格式化
                    Date date = HSSFDateUtil.getJavaDate(d);
                    content = new SimpleDateFormat(DATE_FORMAT).format(date);
                } else {
                    content = formatDouble(d, null);
                }
            }
			break;
		case HSSFCell.CELL_TYPE_FORMULA:

			switch (cell.getCachedFormulaResultType()) {
			case HSSFCell.CELL_TYPE_NUMERIC:
                double dd = cell.getNumericCellValue();
                if (isDateTime(cell)) {
                    content = getDateTimeContent(dd);
                } else {
                    content = String.valueOf(dd);
                }
				break;
			case HSSFCell.CELL_TYPE_STRING:
				content = cell.getRichStringCellValue().toString();
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN:
				content = String.valueOf(cell.getBooleanCellValue());
				break;
			case HSSFCell.CELL_TYPE_ERROR:
				content = convertError(cell.getErrorCellValue());
				break;
			}
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN:
				content = String.valueOf(cell.getBooleanCellValue());
				break;
			case HSSFCell.CELL_TYPE_BLANK:
				content = "";
				break;
			case HSSFCell.CELL_TYPE_ERROR:
				byte errorCellValue = cell.getErrorCellValue();
				content = convertError(errorCellValue);
				break;
			default:
				content = "";
		}
		return content;
	}

    public static String getCellText2007(XSSFCell cell) {
		if (cell == null) {
			return "";
		}
		//获取批注  XG注释
//		XSSFComment hssfcomment = cell.getCellComment();
//		if (hssfcomment != null) {
//			return hssfcomment.getString().toString();
//		}
		String content = "";
		int type = cell.getCellType();
		switch (type) {
		case XSSFCell.CELL_TYPE_STRING:
			content = cell.getRichStringCellValue().toString();
			break;
		case XSSFCell.CELL_TYPE_NUMERIC:
			double d = cell.getNumericCellValue();
            if (isDateTime(cell)) {
                content = getDateTimeContent(d);
            } else {
                if (HSSFDateUtil.isCellDateFormatted(cell)) {// 如果表单对象中是日期格式，对其格式化
                    Date date = HSSFDateUtil.getJavaDate(d);
                    content = new SimpleDateFormat(DATE_FORMAT).format(date);
                } else {
                    content = formatDouble(d, null);
                }
            }
			break;
		case XSSFCell.CELL_TYPE_FORMULA:

			switch (cell.getCachedFormulaResultType()) {
			case XSSFCell.CELL_TYPE_NUMERIC:
                double dd = cell.getNumericCellValue();
                if (isDateTime(cell)) {
                    content = getDateTimeContent(dd);
                } else {
                    content = String.valueOf(dd);
                }
				break;
			case XSSFCell.CELL_TYPE_STRING:
				content = cell.getRichStringCellValue().toString();
				break;
			case XSSFCell.CELL_TYPE_BOOLEAN:
				content = String.valueOf(cell.getBooleanCellValue());
				break;
			case XSSFCell.CELL_TYPE_ERROR:
				content = convertError(cell.getErrorCellValue());
				break;
			}
				break;
			case XSSFCell.CELL_TYPE_BOOLEAN:
				content = String.valueOf(cell.getBooleanCellValue());
				break;
			case XSSFCell.CELL_TYPE_BLANK:
				content = "";
				break;
			case XSSFCell.CELL_TYPE_ERROR:
				byte errorCellValue = cell.getErrorCellValue();
				content = convertError(errorCellValue);
				break;
			default:
				content = "";
		}
		return content;
	}

    public static String formatDouble(Double d, String format) {
		if (d == d.intValue()) {
			return String.valueOf(d.intValue());
		}
		if (StringUtils.isBlank(format)) {
			format = GENERAL_FORMAT;
		}
		return new DecimalFormat(format).format(d);
	}

    public static void print(Map<String, String[][]> map) {
        for (Map.Entry<String, String[][]> entry: map.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println("=================================================");
            for (String[] row : entry.getValue()) {
                for (String cell : row){
                    System.out.print(cell);
                    System.out.print("  ");
                }
                System.out.println();
            }
            System.out.println("-------------------------------------------------");
        }
    }

    public static boolean isDateTime(HSSFCell cell) {
        HSSFCellStyle cellStyle = cell.getCellStyle();
        short dataFormat = cellStyle.getDataFormat();
        String dataForamtStr = cellStyle.getDataFormatString();
        if (dataFormat > 0 && StringUtils.isNotBlank(dataForamtStr)) {
            boolean isInQuote = false;
            for (int i = 0 ;i < dataForamtStr.length() ; i++) {
                char c = dataForamtStr.charAt(i);
                if (c == '"') {
                    isInQuote = !isInQuote;
                } else {
                    if (!isInQuote) {
                        switch (c) {
                            case 'y':
                            case 'm':
                            case 'd':
                            case 'h':
                                return true;
                        }
                    }
                }

            }
        }
        return false;
    }

    public static boolean isDateTime(XSSFCell cell) {
        XSSFCellStyle cellStyle = cell.getCellStyle();
        short dataFormat = cellStyle.getDataFormat();
        String dataForamtStr = cellStyle.getDataFormatString();
        if (dataFormat > 0 && StringUtils.isNotBlank(dataForamtStr)) {
            boolean isInQuote = false;
            for (int i = 0 ;i < dataForamtStr.length() ; i++) {
                char c = dataForamtStr.charAt(i);
                if (c == '"') {
                    isInQuote = !isInQuote;
                } else {
                    if (!isInQuote) {
                        switch (c) {
                            case 'y':
                            case 'm':
                            case 'd':
                            case 'h':
                                return true;
                        }
                    }
                }

            }
        }
        return false;
    }

    public static String getDateTimeContent(double d) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0L);
        double time = d - ((Double) d).intValue();
        cal.add(Calendar.DATE, ((int) d) - 25568 - 1);
        if (time == 0) {
            return new SimpleDateFormat(DATE_FORMAT).format(cal.getTime());
        } else {
            long iTime = (long) (time * 24L * 3600L * 1000L);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.setTimeInMillis(cal.getTimeInMillis() + iTime);
            return new SimpleDateFormat(TIME_FORMAT).format(cal.getTime());
        }
    }


    /**
     * 将excel列的英文转换为数值
     * AA - 26
     * BB - 53
     *
     * @param rowStr AA
     * @return
     */
    public static int getRowNum(String rowStr) {
        char[] rowStrArray = rowStr.toUpperCase().toCharArray();
        int len = rowStrArray.length;
        int n = 0;
        for (int i = 0; i < len; i++) {
            n += (((int) rowStrArray[i]) - 65 + 1) * Math.pow(26, len - i - 1);
        }
        return n - 1;
    }

    public static void main(String[] args) {

		String[] dd = new String [3];
        dd[0] = "1";
        dd[2] = "2";
        for (int j = 1; j < 2; j++) {
            System.out.println("ooo:"+dd[j]);
        }

    }
    
	/**
	 * 数字格式化
	 * 
	 * @param num
	 *            要进行操作的数
	 * @param format
	 *            格式化模板
	 * @return
	 */
	public static String round(Double num, String format) {
		if (num == num.intValue())
			return String.valueOf(num.intValue());
		if (format == null)
			format = GENERAL_FORMAT;
		return new java.text.DecimalFormat(format).format(num);
	}
    
	/**
	 * 判断express是否符合rex正则
	 * 
	 * @param express
	 * @param rex
	 * @return
	 */
	public static boolean match(String express, String rex) {
		Pattern pattern = Pattern.compile(rex);
		Matcher matcher = pattern.matcher(express);
		if (matcher.matches()) {
			return true;
		}
		return false;
	}

    
}
