package com.hujtb.gulimall.seckill.test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextReplace {
    public static void main(String[] args) {
        //读取指定文件夹下的所有文件
        String filepath = "D:\\Desktop\\ccxwss_xjbt";//给我你的目录文件夹路径
        File file = new File(filepath);
        if (!file.isDirectory()) {
            System.out.println("请输入一个目录文件路径");
        } else if (file.isDirectory()) {
            try {
                refreshFileList(filepath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //递归查找文件
    private static void refreshFileList(String strPath) throws IOException {
        File files = new File(strPath);
        File[] filelist = files.listFiles();
        if (filelist == null)
            return;
        for (int i = 0; i < filelist.length; i++) {
            if (filelist[i].isDirectory()) {
                refreshFileList(filelist[i].getAbsolutePath());
            } else {
                String filename = files.getName();//读到的文件名
                String strFileName = filelist[i].getAbsolutePath().toLowerCase();
                String FileNamePath = strFileName.substring(6, strFileName.length());
                //截取文件格式
                String sufName = strFileName.substring(strFileName.lastIndexOf(".") + 1, strFileName.length());
                //排除不需要扫描的文件
                if (sufName.equals("rar") || sufName.equals("jpg") || sufName.equals("png") || sufName.equals("jar") || sufName.equals("doc") || sufName.equals("xls") || sufName.equals("gif") || sufName.equals("wmz")) {
                    continue;
                }
                //不知为何  两种方法判断的时候都会吧class文件和jar文件当做是含有中文字符的文件
                //所以此处排除掉这class文件和jar文件不参与判断
                if (!"fpd".equals(sufName.toLowerCase())) {
                    //开始输入文件流，检查文件
                    // String enCode = getFileEncode(filelist[i].getAbsolutePath());
                    String enCode = "UTF-8";
                    FileInputStream fis = new FileInputStream(filelist[i].getAbsolutePath());
                    InputStreamReader in = new InputStreamReader(fis, enCode);
                    BufferedReader br = new BufferedReader(in);

                    StringBuffer strBuffer = new StringBuffer();
                    String line = null;
                    while ((line = br.readLine()) != null) {

                        Map<String, String> map = new HashMap<String, String>();

                        map.put("J1_CXBB", "J1_XJBT");
                        Set<Map.Entry<String, String>> entries = map.entrySet();
                        for (Map.Entry<String, String> entry : entries) {
                            if (line.indexOf(entry.getKey()) != -1) { //判断当前行是否存在想要替换掉的字符 -1表示存在
                                line = line.replace(entry.getKey(), entry.getValue());//替换为你想替换的内容
                            }
                        }
                        strBuffer.append(line);
                        strBuffer.append(System.getProperty("line.separator"));//行与行之间的分割
                    }
                    br.close();

                    PrintWriter printWriter = new PrintWriter("D:\\Desktop\\ccxwss_xjbt_new\\" + FileNamePath);//替换后输出的文件位置
                    printWriter.write(strBuffer.toString().toCharArray());
                    printWriter.flush();
                    printWriter.close();
                    System.out.println("ok 第 " + (i + 1) + " 个文件操作成功！");
                }
            }
        }
    }

//    //检查文件类型
//    public static String getFileEncode(String path) {
//        /*
//         * detector是探测器，它把探测任务交给具体的探测实现类的实例完成。
//         * cpDetector内置了一些常用的探测实现类，这些探测实现类的实例可以通过add方法 加进来，如ParsingDetector、
//         * JChardetFacade、ASCIIDetector、UnicodeDetector。
//         * detector按照“谁最先返回非空的探测结果，就以该结果为准”的原则返回探测到的
//         * 字符集编码。使用需要用到三个第三方JAR包：antlr.jar、chardet.jar和cpdetector.jar
//         * cpDetector是基于统计学原理的，不保证完全正确。
//         */
//        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
//        /*
//         * ParsingDetector可用于检查HTML、XML等文件或字符流的编码,构造方法中的参数用于
//         * 指示是否显示探测过程的详细信息，为false不显示。
//         */
//        detector.add(new ParsingDetector(false));
//        /*
//         * JChardetFacade封装了由Mozilla组织提供的JChardet，它可以完成大多数文件的编码
//         * 测定。所以，一般有了这个探测器就可满足大多数项目的要求，如果你还不放心，可以
//         * 再多加几个探测器，比如下面的ASCIIDetector、UnicodeDetector等。
//         */
//        detector.add(JChardetFacade.getInstance());// 用到antlr.jar、chardet.jar
//        // ASCIIDetector用于ASCII编码测定
//        detector.add(ASCIIDetector.getInstance());
//        // UnicodeDetector用于Unicode家族编码的测定
//        detector.add(UnicodeDetector.getInstance());
//        java.nio.charset.Charset charset = null;
//        File f = new File(path);
//        try {
//            charset = detector.detectCodepage(f.toURI().toURL());
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        if (charset != null)
//            return charset.name();
//        else
//            return null;
//    }
}
