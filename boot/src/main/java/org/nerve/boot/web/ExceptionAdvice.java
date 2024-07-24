package org.nerve.boot.web;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.nerve.boot.Result;
import org.nerve.boot.web.auth.AuthHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
@Order(Integer.MIN_VALUE)
public class ExceptionAdvice {

    @Resource
    private AuthHolder holder;
    private Logger logger = LoggerFactory.getLogger(getClass());

    @ResponseBody
    @ExceptionHandler
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handler(HttpServletRequest request, Exception e){


//        val trace           = Trace.of(request, holder.get())
//        trace.path          = request.servletPath
//        trace.ok            = false
//        trace.exception     = ExceptionUtils.getMessage(e)
//        traceService.log(trace)
        if(e instanceof MethodArgumentNotValidException){
            MethodArgumentNotValidException me = (MethodArgumentNotValidException) e;
            /*
            ObjectError 结构如下：
            {
                "arguments": [{
                    "code": "aid",
                    "codes": ["dataCreateModel.aid", "aid"],
                    "defaultMessage": "aid"
                }],
                "bindingFailure": false,
                "code": "NotBlank",
                "codes": ["NotBlank.dataCreateModel.aid", "NotBlank.aid", "NotBlank.java.lang.String", "NotBlank"],
                "defaultMessage": "aid 未填写",
                "field": "aid",
                "objectName": "dataCreateModel",
                "rejectedValue": ""
            }
             */
            ObjectError error = me.getAllErrors().get(0);
            return Result.fail("[参数检验失败] "+error.getDefaultMessage());
        }

        logger.error("[异常]: "+ExceptionUtils.getMessage(e), e);
        holder.clean();
        return Result.fail(e);
    }

    @ResponseBody
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result for404(HttpServletRequest request){
        logger.info("[404] {}", request.getServletPath());
        return Result.fail("404");
    }
}
