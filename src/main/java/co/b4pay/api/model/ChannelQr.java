//package co.b4pay.api.model;
//
//import co.b4pay.api.model.base.BaseEntity;
//
//import javax.persistence.*;
//import java.math.BigDecimal;
//import java.util.Date;
//
///**
// * 渠道二维码信息
// * Created with IntelliJ IDEA
// * Created By AZain
// * Date: 2018-10-27
// * Time: 21:49
// */
//@Entity
//@Table(name = "dst_channel_qr")
//public class ChannelQr extends BaseEntity {
//
//
//    private static final long serialVersionUID = 352386917350321286L;
//    @Id
//    private Long id;
//
//    private String qrUrl;
//
//    private Date executionTime;
//
//    private boolean effective;
//
//    private Integer status;
//
//    private Date createTime;
//
//    private Date updateTime;
//
//    @ManyToOne
//    @JoinColumn(name = "chanel_id")
//    private Channel channel;
//
//    @ManyToOne
//    @JoinColumn(name = "router_id")
//    private Router router;
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getQrUrl() {
//        return qrUrl;
//    }
//
//    public void setQrUrl(String qrUrl) {
//        this.qrUrl = qrUrl;
//    }
//
//    public Date getExecutionTime() {
//        return executionTime;
//    }
//
//    public void setExecutionTime(Date executionTime) {
//        this.executionTime = executionTime;
//    }
//
//    public boolean isEffective() {
//        return effective;
//    }
//
//    public void setEffective(boolean effective) {
//        this.effective = effective;
//    }
//
//    public Integer getStatus() {
//        return status;
//    }
//
//    public void setStatus(Integer status) {
//        this.status = status;
//    }
//
//    public Channel getChannel() {
//        return channel;
//    }
//
//    public void setChannel(Channel channel) {
//        this.channel = channel;
//    }
//
//    public Router getRouter() {
//        return router;
//    }
//
//    public void setRouter(Router router) {
//        this.router = router;
//    }
//
//    public Date getCreateTime() {
//        return createTime;
//    }
//
//    public void setCreateTime(Date createTime) {
//        this.createTime = createTime;
//    }
//
//    public Date getUpdateTime() {
//        return updateTime;
//    }
//
//    public void setUpdateTime(Date updateTime) {
//        this.updateTime = updateTime;
//    }
//}
