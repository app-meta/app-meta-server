package org.nerve.boot.web;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nerve.boot.Result;

import java.io.IOException;

import static org.nerve.boot.Const.COMMA;

public final class WebUtil {

    /**
     *
     * @param request
     * @return
     */
    public static String getIp(HttpServletRequest request){
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if("0:0:0:0:0:0:0:1".equals(ip))
                ip = "127.0.0.1";
        }
        return ip.split(COMMA)[0];
    }

    public static String getHeader(HttpServletRequest request, String header){
        return request.getHeader(header);
    }

    public static String getUserAgent(HttpServletRequest request){
        return getHeader(request, "User-Agent");
    }

    /**
     *
     * @param response
     * @param result
     * @throws IOException
     */
    public static void jsonResult(HttpServletResponse response, Result result) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setHeader("content-type", "application/json;charset=UTF-8");
        response.getWriter().print(JSON.toJSONString(result));
    }
}
