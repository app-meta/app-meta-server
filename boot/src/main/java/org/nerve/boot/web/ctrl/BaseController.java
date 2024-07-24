package org.nerve.boot.web.ctrl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nerve.boot.Result;
import org.nerve.boot.web.WebUtil;
import org.nerve.boot.web.auth.AuthHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;

public abstract class BaseController {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    protected HttpServletRequest request;

    @Resource
    protected AuthHolder authHolder;

    /**
     * 向Response发送文本（默认使用UTF-8编码）
     * @param res           Response
     * @param msg           内容
     * @throws IOException  IO异常
     */
    protected void write(HttpServletResponse res, String msg)throws IOException{
        initResponse(res);
        res.getWriter().print(msg);
    }

    /**
     * 设置response，默认的编码为utf-8
     * @return       Http Response
     */
    protected HttpServletResponse initResponse(HttpServletResponse resp){
        resp.setHeader("content-type", "text/html;charset=utf-8");
        return resp;
    }

    /**
     * 通用的返回result
     * @param consumer  逻辑处理
     * @return          Result
     */
    protected Result result(DataConsumer<Result> consumer){
        Result result=new Result();
        try{
            consumer.accept(result);
        }catch (Exception e){
            result.error(e);
            if(e instanceof SQLException){
                result.setMessage("出现数据库异常，请及时联系科技部进行排查处理");
            }

            logger.error("处理出错", e);
        }
        return result;
    }

    /**
     * 计算结果并赋值到 Result.data 中，默认 success 为 true
     * @param provider
     * @return
     */
    protected Result resultWithData(DataProvider provider){
        Result result=new Result();
        try{
            result.setData(provider.get());
        }catch (Exception e){
            result.error(e);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取请求参数
     * @param name
     * @param defaultValue
     * @return
     */
    protected String getParameter(String name, String defaultValue){
        String p = request.getParameter(name);
        return Objects.isNull(p)?defaultValue:p;
    }

    /**
     * 获取请求头，不存在则返回空字符串
     * @param name
     * @return
     */
    protected String getHeader(String name){
        String h = request.getHeader(name);
        return Objects.isNull(h) ? "":h;
    }

    protected boolean isGET(){
        return request.getMethod().equalsIgnoreCase(HttpMethod.GET.name());
    }

    /**
     * 获取请求中的 int 类型参数
     * @param name
     * @param defaultValue
     * @return
     */
    protected int getParameter(String name, int defaultValue){
        return Integer.valueOf(getParameter(name, defaultValue+""));
    }

    protected Map<String, Object> getReqQueryParams(){
        final Map<String, Object> m = new HashMap<>();
        request.getParameterMap().forEach((k,v)->{
            if(v.length==1)
                m.put(k, v[0]);
            else
                m.put(k, Arrays.asList(v));
        });
        m.putAll(queryParamsForList());
        return m;
    }

    /**
     * 构建用于列表查询的参数列表，重写该方法可以添加查询条件
     * @return
     */
    protected Map<String, Object> queryParamsForList(){
        return Collections.EMPTY_MAP;
    }

    protected String getRequestIP(){
        return WebUtil.getIp(request);
    }

    /**
     * 下载文件到浏览器
     * @param file
     * @param fileName
     * @param handler
     */
    public void downloadFile(HttpServletResponse response, File file, String fileName, FinishHandler handler)
            throws Exception {
        try(
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())
        ){
            //设置 response 的头信息
            response.setContentType("text/html;charset=utf-8");
            request.setCharacterEncoding("UTF-8");
            response.setContentType("application/x-msdownload;");

            String fName = fileName == null?file.getName() : fileName;
            response.setHeader(
                    "Content-disposition",
                    "attachment; filename="+ URLEncoder.encode(fName, "UTF-8")//new String(fName.getBytes("gb2312"), StandardCharsets.ISO_8859_1)
            );
            response.setIntHeader("Content-Length", fis.available());

            byte[] buff = new byte[1024];
            int bytesRead = bis.read(buff,0, buff.length);
            while (bytesRead != -1){
                bos.write(buff,0, bytesRead);
                bytesRead = bis.read(buff,0, bytesRead);
            }
            bos.flush();
        }catch(Exception e){
            throw e;
        }finally {
            handler.onEnd();
        }
    }

    /**
     * 作业完成后的数据
     */
    public interface FinishHandler{
        void onEnd();
    }

    public interface DataProvider{
        Object get() throws Exception;
    }

    public interface DataConsumer<T> {
        void accept(T t) throws Exception;
    }
}
