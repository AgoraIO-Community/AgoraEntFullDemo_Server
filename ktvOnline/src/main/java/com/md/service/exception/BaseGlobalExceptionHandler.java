package com.md.service.exception;

import com.md.service.model.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;

@Configuration
@RestControllerAdvice
@Slf4j
public class BaseGlobalExceptionHandler {

    @ExceptionHandler({Exception.class})
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResult<?> internalExceptionHandler(Exception exception) {
        log.info("Internal server error ex={}", exception.getMessage());
        return BaseResult.error(500,"Internal Server Error");
    }

    @ExceptionHandler({BaseException.class})
    @ResponseBody
    @ResponseStatus(value = HttpStatus.OK)
    public BaseResult<?> baseBusinessExceptionHandler(BaseException exception) {
        log.info("[BizException]业务异常信息 ex={}", exception.getMessage());
        return BaseResult.error(exception.getCode(), exception.getMessage());
    }


    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public BaseResult<?> MethodArgumentNotValidExceptionHandle(MethodArgumentNotValidException exception,
                                                               HttpServletResponse httpResponse, HttpServletRequest servletRequest) {
        StringBuilder msg = new StringBuilder();
        for (ObjectError objectError : exception.getBindingResult().getAllErrors()) {
            msg.append(objectError.getDefaultMessage()).append(";");
        }
        log.info("[bindException]请求参数错误 ex={}", msg);
        return BaseResult.error(HttpStatus.BAD_REQUEST.value(), msg.toString());
    }


    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public BaseResult<?> ConstraintViolationExceptionHandler(ConstraintViolationException exception,
                                                             HttpServletResponse httpResponse, HttpServletRequest servletRequest) {
        log.info("[bindException]请求参数错误 ex={}", exception.getMessage());
        return BaseResult.error(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }

    @ExceptionHandler({BindException.class})
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public BaseResult<?> bindExceptionHandler(BindException exception) {
        log.info("[bindException]请求参数错误 ex={}", exception.getMessage());
        return BaseResult.error(HttpStatus.BAD_REQUEST.value(), exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public BaseResult<?> httpMessageNotReadableExceptionHandle(HttpMessageNotReadableException exception) {
        log.info("[httpMessageNotReadableExceptionHandle]请求参数错误 ex={}", exception.getMessage());
        return BaseResult.error(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }

}
