package cn.keking.model;

/**
 * @author douwenjie
 * @create 2023-10-24
 */
public class ResponseResult {

    private boolean success;

    private String msg;

    private Object data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }


    public void setMsg(String msg) {
        this.msg = msg;
    }


    public Object getData() {
        return data;
    }


    public void setData(Object data) {
        this.data = data;
    }


    private ResponseResult(boolean success, String msg) {
        super();
        this.success = success;
        this.msg = msg;
    }
    private ResponseResult(boolean success, String msg,Object data) {
        super();
        this.success = success;
        this.msg = msg;
        this.data = data;
    }


    public static ResponseResult success(String msg){
        return new ResponseResult(true,msg);
    }

    public static ResponseResult failed(String msg){
        return new ResponseResult(false,msg);
    }

    public static ResponseResult success(String msg,Object data){
        return new ResponseResult(true,msg,data);
    }

    public static ResponseResult failed(String msg,Object data){
        return new ResponseResult(false,msg,data);
    }

    @Override
    public String toString() {
        return "ResponseResult{" +
                "success=" + success +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
