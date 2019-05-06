package co.b4pay.api.model;

import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.model.base.BaseEntity;
import co.b4pay.api.model.base.BaseEntity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static co.b4pay.api.common.utils.DateUtil.now;

@Entity
@Table(name = "dst_job_trade")
@EntityListeners(AuditingEntityListener.class)
public class JobTrade extends BaseEntity {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3050115515232457103L;

    @Id
    private String id;
    @Column(name = "content", columnDefinition = "text")
    private String content;
    private String notifyUrl;
    private Date execTime;
    private Integer count;
    private Integer status;
    @Enumerated(EnumType.STRING)
    private ChannelType channelType;
    //    @CreatedDate
//    private Date createTime = now();
    private Date createTime;
    //    @LastModifiedDate
//    private Date updateTime = now();
    private Date updateTime;

    public JobTrade() {
        try {
            Date a = DateUtil.YMdhms.parse(DateUtil.YMdhms.format(DateUtil.now()).toString());
            this.createTime = a;
            this.updateTime = a;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public Date getExecTime() {
        return execTime;
    }

    public void setExecTime(Date execTime) {
        this.execTime = execTime;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {

        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
//        try {
//            updateTime =DateUtil.YMdhms.parse(DateUtil.YMdhms.format(DateUtil.now()).toString());
//        }catch (Exception ex){
//            System.out.println(ex.getMessage());
//        }
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "JobTrade{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", notifyUrl='" + notifyUrl + '\'' +
                ", execTime=" + execTime +
                ", count=" + count +
                ", status=" + status +
                ", channelType=" + channelType +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
