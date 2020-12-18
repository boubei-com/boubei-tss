/* ==================================================================   
 * Created [2017-10-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.log4j.Logger;

public class MatrixUtil {
	
	static Logger log = Logger.getLogger(MatrixUtil.class);
	
	public static String getIpAddress() {
		String rtip = "unknown.host";
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
				if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
					continue;
				} else {
					Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
					while (addresses.hasMoreElements()) {
						ip = addresses.nextElement();
						if (ip != null && ip instanceof Inet4Address) {
							rtip = ip.getHostAddress();
						}
					}
				}
			}
		} catch (Exception e) { }

		return rtip;
	}
	  
}
