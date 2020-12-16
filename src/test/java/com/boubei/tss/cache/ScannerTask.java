/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cache;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

import com.boubei.tss.cache.extension.workqueue.Task;

/** 
 * 扫描机器的各个端口，判断各端口是开着还是关闭的。 
 * 
 */
public class ScannerTask implements Task, Serializable{
 
	private static final long serialVersionUID = -578000296494659599L;
	
	final long createTime = System.currentTimeMillis();
	final String host = "127.0.0.1";
    int   port;
    String oldInfo;
    
    public static int finishedNum = 0;
    
    /**
     * 因为ScannerTask的对象很小，从缓存中获取或是直接new出来，差别并不大， <br/>
     * 所以尝试增长Scanner对象的生成时间，构造函数里 sleep（n）。 <br/>
     */
    public ScannerTask() {
        try {
            Thread.sleep(100); // 增加对象的创建时间
        } catch (InterruptedException e) {
        }
    }
    
    public ScannerTask(String name) {  }

    public void excute() {
        Socket s = null;
        try {
            if(port > 0 && port % 5000 == 0) {
                System.out.println("  已经扫描 " + port + " 个");
            }
            
            s = new Socket(host, port);
            System.out.println("  " + this + "执行完成, port is open.");
        } 
        catch (IOException ex) {
            System.out.println("  " + this + "执行完成, port is close.");
        } 
        finally {
            finishedNum ++;
            try {
                if (s != null) {
                    s.close();
                }
            } catch (IOException e) {
            }
        }
    }
    
    public int hashCode(){
        return new Long(createTime).hashCode();
    }
    
    public boolean equals(Object o){
        if(o instanceof ScannerTask){
            ScannerTask temp = (ScannerTask) o;
            return this.createTime == temp.createTime;
        }
        return false;
    }
    
    public String toString() {
        return "扫描端口 " + this.host + ": " + this.port + " 的任务"; 
    }

    public void recycle() throws Exception {
    	if( this.port == 99999) {
    		throw new RuntimeException("recycle failed");
    	}
        this.port = 0;
    }
}

