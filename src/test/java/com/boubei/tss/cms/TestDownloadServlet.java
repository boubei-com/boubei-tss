/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms;
 
public class TestDownloadServlet {
 
    public static int count = 0;
    
    public static void testDownload(){
        while(true){
            try {
                java.net.URL url = new java.net.URL("http://localhost:8088/cms/download?id=881&seqNo=3");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.connect(); 
                java.io.InputStream in = conn.getInputStream(); 

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in)); 

                String currentLine = ""; 
                String totalString = "";
                while ((currentLine = reader.readLine()) != null){ 
                    totalString += currentLine + "\n"; 
                } 
                in.close();
                reader.close();
                
                System.out.println( totalString ); 
                System.out.println( count++ ); 
                
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            } 
        }
    }
 
    public static void main(String[] agrs) throws Exception {
        for(int i = 0; i < 10; i++){
            new Thread(){
                public void run(){
                    testDownload();
                }
            }.start();
        }
    }
}


