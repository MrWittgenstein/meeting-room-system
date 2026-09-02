package com.uestcfir.pojo.entity;

import lombok.Data;

@Data
public class Result {
    private int code;
    private String message;
    private Object data;

    public static Result success(){
        Result result = new Result();
        result.setCode(1);
        result.setMessage("success");
        return result;
    }

    public static Result fail(){
        Result result = new Result();
        result.setCode(0);
        result.setMessage("fail");
        return result;}

    public static Result success(Object data){
        Result result = new Result();
        result.setCode(1);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static Result fail(String message){
        Result result = new Result();
        result.setCode(0);
        result.setMessage(message);
        return result;}


}
