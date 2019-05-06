package co.b4pay.api.common.handler;

import co.b4pay.api.common.utils.WebUtil;
import org.apache.catalina.util.RequestUtil;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Calendar calendar = Calendar.getInstance();

    /**
     * 系统异常处理，比如：404, 500
     *
     * @param request
     * @param e
     * @return
     * @throws Exception
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseData defaultErrorHandler(HttpServletRequest request, Exception e) throws Exception {
        ResponseData r = new ResponseData();
        r.setMsg(e.getMessage());
        if (e instanceof org.springframework.web.servlet.NoHandlerFoundException) {
            r.setStatus(HttpStatus.SC_NOT_FOUND);
        } else {
            String errorMsg = e.getMessage() + "[" + request.getMethod() + ": " + WebUtil.getRequestUrl(request) + "?" + WebUtil.getQueryString(request) + "]";
            logger.error(errorMsg, e);
            r.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        r.setData(null);
        r.setCode(-1);
        r.setPath(request.getRequestURI());
        r.setTimestamp(calendar.getTime());
        return r;
    }

    private class ResponseData {

        private int code;
        private String msg;
        private Object data;
        private int status;
        private String path;
        private Date timestamp;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
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

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }
}
